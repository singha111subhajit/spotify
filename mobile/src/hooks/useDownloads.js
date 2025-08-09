import { useCallback, useEffect, useMemo, useState } from 'react';
import RNFS from 'react-native-fs';
import NetInfo from '@react-native-community/netinfo';

const DIR = `${RNFS.DocumentDirectoryPath}/downloads`;

export function useDownloads() {
  const [isOnline, setIsOnline] = useState(true);
  const [files, setFiles] = useState({});

  useEffect(() => {
    const sub = NetInfo.addEventListener(state => setIsOnline(!!state.isConnected));
    return () => sub && sub();
  }, []);

  const ensureDir = useCallback(async () => {
    const exists = await RNFS.exists(DIR);
    if (!exists) await RNFS.mkdir(DIR);
  }, []);

  const localPathFor = useCallback((songId, title) => {
    const safe = String(title || songId).replace(/[^a-z0-9\-_.]/gi, '_');
    return `${DIR}/${songId}-${safe}.mp3`;
  }, []);

  const isDownloaded = useCallback(async (songId, title) => {
    const path = localPathFor(songId, title);
    return RNFS.exists(path);
  }, [localPathFor]);

  const downloadSong = useCallback(async (song) => {
    await ensureDir();
    const dest = localPathFor(song.id, song.title);
    const exists = await RNFS.exists(dest);
    if (exists) return dest;
    await RNFS.downloadFile({ fromUrl: song.url, toFile: dest }).promise;
    return dest;
  }, [ensureDir, localPathFor]);

  const decorateSongs = useCallback(async (songs) => {
    const decorated = await Promise.all(songs.map(async (s) => {
      const path = localPathFor(s.id, s.title);
      const exists = await RNFS.exists(path);
      return exists ? { ...s, localPath: `file://${path}` } : s;
    }));
    return decorated;
  }, [localPathFor]);

  return useMemo(() => ({ isOnline, downloadSong, isDownloaded, decorateSongs }), [isOnline, downloadSong, isDownloaded, decorateSongs]);
}