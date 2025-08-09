import React, { useEffect, useState, useCallback } from 'react';
import { View, Text, FlatList, StyleSheet, RefreshControl, TouchableOpacity } from 'react-native';
import { fetchPlaylists, fetchPlaylistSongs, removeSongFromPlaylist } from '../services/api';

export default function PlaylistScreen() {
  const [playlist, setPlaylist] = useState(null);
  const [songs, setSongs] = useState([]);
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    setRefreshing(true);
    try {
      const playlists = await fetchPlaylists();
      const pl = playlists?.[0] || null;
      setPlaylist(pl);
      if (pl) {
        const items = await fetchPlaylistSongs(pl.id);
        setSongs(items);
      }
    } finally {
      setRefreshing(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const onRemove = async (item) => {
    if (!playlist) return;
    await removeSongFromPlaylist(playlist.id, item.id);
    await load();
  };

  return (
    <View style={styles.container}>
      <Text style={styles.header}>{playlist?.name || 'Playlist'}</Text>
      <FlatList
        data={songs}
        keyExtractor={(item) => String(item.id)}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={load} />}
        renderItem={({ item }) => (
          <View style={styles.row}>
            <Text style={styles.title} numberOfLines={1}>{item.song_title}</Text>
            <TouchableOpacity onPress={() => onRemove(item)} style={styles.removeBtn}>
              <Text style={styles.removeText}>Remove</Text>
            </TouchableOpacity>
          </View>
        )}
        ItemSeparatorComponent={() => <View style={{ height: 10 }} />}
        contentContainerStyle={{ padding: 16 }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0b0b0b' },
  header: { color: '#fff', fontSize: 18, fontWeight: '700', paddingHorizontal: 16, paddingTop: 16 },
  row: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', backgroundColor: '#1b1b1b', borderRadius: 8, padding: 12 },
  title: { color: '#fff', flex: 1, marginRight: 12 },
  removeBtn: { backgroundColor: '#292929', paddingVertical: 8, paddingHorizontal: 12, borderRadius: 6 },
  removeText: { color: '#ff6b6b' }
});