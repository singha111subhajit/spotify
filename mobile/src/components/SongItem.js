import React from 'react';
import { View, Text, Image, TouchableOpacity, StyleSheet } from 'react-native';

export default function SongItem({ song, onPress, onAdd, onLongPress }) {
  return (
    <TouchableOpacity onPress={onPress} onLongPress={onLongPress} delayLongPress={350} style={styles.container}>
      {song.thumbnail ? (
        <Image source={{ uri: song.thumbnail }} style={styles.thumb} />
      ) : (
        <View style={[styles.thumb, styles.placeholder]} />
      )}
      <View style={styles.meta}>
        <Text numberOfLines={1} style={styles.title}>{song.title}</Text>
        <Text numberOfLines={1} style={styles.artist}>{song.artist || 'Unknown Artist'}</Text>
      </View>
      {onAdd && (
        <TouchableOpacity onPress={onAdd} style={styles.addBtn}>
          <Text style={styles.addText}>+</Text>
        </TouchableOpacity>
      )}
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  container: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#1b1b1b', padding: 12, borderRadius: 8 },
  thumb: { width: 54, height: 54, borderRadius: 6, backgroundColor: '#333' },
  placeholder: { alignItems: 'center', justifyContent: 'center' },
  meta: { flex: 1, marginLeft: 12 },
  title: { color: '#fff', fontWeight: '700' },
  artist: { color: '#999', marginTop: 2 },
  addBtn: { width: 36, height: 36, borderRadius: 18, backgroundColor: '#1DB954', alignItems: 'center', justifyContent: 'center' },
  addText: { color: '#000', fontWeight: '800', fontSize: 18 }
});