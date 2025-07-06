import axios from 'axios';
import { InternetArchiveResponse, InternetArchiveItem, InternetArchiveFile, Song } from '@/types';

const BASE_URL = 'https://archive.org';
const SEARCH_URL = `${BASE_URL}/advancedsearch.php`;
const METADATA_URL = `${BASE_URL}/metadata`;

export class InternetArchiveAPI {
  private static async delay(ms: number) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  static async searchSongs(query: string, page: number = 1, rows: number = 20): Promise<{songs: Song[], totalFound: number}> {
    try {
      // Add rate limiting
      await this.delay(100);
      
      const params = {
        q: `collection:(etree OR opensource_audio) AND format:(MP3) AND title:(${query}) AND mediatype:audio`,
        fl: 'identifier,title,creator,date,description,downloads,item_size',
        sort: 'downloads desc',
        rows: rows,
        page: page,
        output: 'json'
      };

      const response = await axios.get<InternetArchiveResponse>(SEARCH_URL, { 
        params,
        timeout: 10000 
      });

      const songs: Song[] = [];
      
      for (const item of response.data.docs) {
        try {
          const songUrl = await this.getSongUrl(item.identifier);
          if (songUrl) {
            songs.push({
              id: item.identifier,
              title: item.title || 'Unknown Title',
              artist: item.creator || 'Unknown Artist',
              url: songUrl,
              source: 'api',
              album: item.collection?.[0],
              year: item.date ? new Date(item.date).getFullYear() : undefined,
              thumbnail: `${BASE_URL}/services/img/${item.identifier}`
            });
          }
        } catch (error) {
          console.warn(`Failed to get song URL for ${item.identifier}:`, error);
        }
      }

      return {
        songs,
        totalFound: response.data.numFound
      };
    } catch (error) {
      console.error('Error searching songs:', error);
      return { songs: [], totalFound: 0 };
    }
  }

  static async getSongUrl(identifier: string): Promise<string | null> {
    try {
      const response = await axios.get<{files: InternetArchiveFile[]}>(`${METADATA_URL}/${identifier}/files`, {
        timeout: 5000
      });

      const mp3File = response.data.files?.find(file => 
        file.format === 'VBR MP3' || 
        file.format === 'MP3' ||
        (file.name && file.name.toLowerCase().endsWith('.mp3'))
      );

      if (mp3File) {
        return `${BASE_URL}/download/${identifier}/${encodeURIComponent(mp3File.name)}`;
      }

      return null;
    } catch (error) {
      console.error(`Error getting song URL for ${identifier}:`, error);
      return null;
    }
  }

  static async getPopularSongs(limit: number = 50): Promise<Song[]> {
    try {
      const params = {
        q: 'collection:(etree OR opensource_audio) AND format:(MP3) AND mediatype:audio',
        fl: 'identifier,title,creator,date,description,downloads',
        sort: 'downloads desc',
        rows: limit,
        output: 'json'
      };

      const response = await axios.get<InternetArchiveResponse>(SEARCH_URL, { 
        params,
        timeout: 15000 
      });

      const songs: Song[] = [];
      
      // Process items in batches to avoid overwhelming the API
      const batchSize = 5;
      for (let i = 0; i < Math.min(response.data.docs.length, 20); i += batchSize) {
        const batch = response.data.docs.slice(i, i + batchSize);
        const batchPromises = batch.map(async (item) => {
          try {
            const songUrl = await this.getSongUrl(item.identifier);
            if (songUrl) {
              return {
                id: item.identifier,
                title: item.title || 'Unknown Title',
                artist: item.creator || 'Unknown Artist',
                url: songUrl,
                source: 'api' as const,
                album: item.collection?.[0],
                year: item.date ? new Date(item.date).getFullYear() : undefined,
                thumbnail: `${BASE_URL}/services/img/${item.identifier}`
              };
            }
          } catch (error) {
            console.warn(`Failed to process item ${item.identifier}:`, error);
          }
          return null;
        });

        const batchResults = await Promise.allSettled(batchPromises);
        const validSongs = batchResults
          .map(result => result.status === 'fulfilled' ? result.value : null)
          .filter((song): song is Song => song !== null);
        
        songs.push(...validSongs);
        
        // Add delay between batches
        if (i + batchSize < response.data.docs.length) {
          await this.delay(500);
        }
      }

      return songs;
    } catch (error) {
      console.error('Error fetching popular songs:', error);
      return [];
    }
  }
}