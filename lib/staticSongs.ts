import { Song } from '@/types';

// Static songs that will be stored in public/static-songs/
export const staticSongs: Song[] = [
  {
    id: 'static-1',
    title: 'Sample Song 1',
    artist: 'Sample Artist',
    url: '/static-songs/sample1.mp3',
    source: 'static',
    duration: 180, // 3 minutes
  },
  {
    id: 'static-2',
    title: 'Sample Song 2',
    artist: 'Sample Artist',
    url: '/static-songs/sample2.mp3',
    source: 'static',
    duration: 240, // 4 minutes
  },
  // Add more static songs as needed
];

export function getStaticSongs(): Song[] {
  return staticSongs;
}

export function getRandomStaticSong(): Song {
  const randomIndex = Math.floor(Math.random() * staticSongs.length);
  return staticSongs[randomIndex];
}