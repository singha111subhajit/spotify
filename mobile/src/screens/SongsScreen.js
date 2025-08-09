import React, { useEffect, useState, useCallback } from 'react';
import { View, Text, FlatList, RefreshControl, StyleSheet, Alert } from 'react-native';
import { fetchSongs, fetchPlaylists, addSongToPlaylist } from '../services/api';
import { loadQueue, playTrackAtIndex } from '../services/player';
import SongItem from '../components/SongItem';
import { useDownloads } from '../hooks/useDownloads';

export default function SongsScreen() {
  const [songs, setSongs] = useState([]);
  const [refreshing, setRefreshing] = useState(false);
  const [playlist, setPlaylist] = useState(null);
  const { decorateSongs, downloadSong } = useDownloads();

  const load = useCallback(async () => {
    setRefreshing(true);
    try {
      const [songsData, playlists] = await Promise.all([fetchSongs(), fetchPlaylists()]);
      const decorated = await decorateSongs(songsData);
      setSongs(decorated);
      setPlaylist(playlists?.[0] || null);
      await loadQueue(decorated);
    } finally {
      setRefreshing(false);
    }
  }, [decorateSongs]);

  useEffect(() => {
    load();
  }, [load]);

  const onPressSong = async (item, index) => {
    await playTrackAtIndex(index);
  };

  const onAddToPlaylist = async (item) => {
    if (!playlist) return;
    await addSongToPlaylist(playlist.id, item.id, item.title);
  };

  const onLongPressSong = async (item) => {
    try {
      const path = await downloadSong(item);
      Alert.alert('Downloaded', 'Saved for offline playback');
      await load();
    } catch (e) {
      Alert.alert('Download failed', 'Could not download this track');
    }
  };

  return (
    <View style={styles.container}>
      <FlatList
        data={songs}
        keyExtractor={(item) => String(item.id)}
        renderItem={({ item, index }) => (
          <SongItem song={item} onPress={() => onPressSong(item, index)} onAdd={() => onAddToPlaylist(item)} onLongPress={() => onLongPressSong(item)} />
        )}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={load} />}
        ItemSeparatorComponent={() => <View style={{ height: 10 }} />}
        contentContainerStyle={{ padding: 16 }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0b0b0b' }
});