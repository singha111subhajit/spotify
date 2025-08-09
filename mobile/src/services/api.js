import axios from 'axios';
import { getToken } from './auth';

const DEFAULT_BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL || 'http://10.0.2.2:5600';

export const api = axios.create({
  baseURL: DEFAULT_BASE_URL
});

api.interceptors.request.use(async (config) => {
  const token = await getToken();
  if (token) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export async function registerUser(username, userId, password) {
  await api.post('/register', { username, user_id: userId, password });
}

export async function loginUser(userId, password) {
  const { data } = await api.post('/login', { user_id: userId, password });
  return data.token;
}

export async function fetchMe() {
  const { data } = await api.get('/me');
  return data;
}

export async function fetchSongs(page = 1, perPage = 20) {
  const { data } = await api.get('/api/songs', { params: { page, per_page: perPage } });
  return data.songs;
}

export async function fetchPlaylists() {
  const { data } = await api.get('/playlists');
  return data.playlists; // default playlist only
}

export async function fetchPlaylistSongs(playlistId) {
  const { data } = await api.get(`/playlists/${playlistId}/songs`);
  return data.songs;
}

export async function addSongToPlaylist(playlistId, songId, songTitle) {
  await api.post(`/playlists/${playlistId}/songs`, { song_id: songId, song_title: songTitle });
}

export async function removeSongFromPlaylist(playlistId, songDbId) {
  await api.delete(`/playlists/${playlistId}/songs/${songDbId}`);
}