export interface Song {
  id: string;
  title: string;
  artist: string;
  duration?: number;
  url: string;
  thumbnail?: string;
  source: 'static' | 'api';
  album?: string;
  year?: number;
}

export interface InternetArchiveResponse {
  docs: InternetArchiveItem[];
  numFound: number;
  start: number;
}

export interface InternetArchiveItem {
  identifier: string;
  title: string;
  creator?: string;
  date?: string;
  description?: string;
  subject?: string[];
  collection?: string[];
  mediatype: string;
  downloads?: number;
  item_size?: number;
}

export interface InternetArchiveFile {
  name: string;
  source: string;
  format: string;
  size?: string;
  length?: string;
}

export interface PlaylistState {
  songs: Song[];
  currentIndex: number;
  isPlaying: boolean;
  volume: number;
  currentTime: number;
  duration: number;
  isShuffled: boolean;
  isRepeating: boolean;
}

export interface PaginationState {
  currentPage: number;
  totalPages: number;
  itemsPerPage: number;
  totalItems: number;
}