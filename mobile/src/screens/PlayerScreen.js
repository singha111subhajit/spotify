import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import TrackPlayer, { useProgress, State } from 'react-native-track-player';
import { togglePlayPause } from '../services/player';

export default function PlayerScreen() {
  const progress = useProgress();
  const [track, setTrack] = useState(null);
  const [state, setState] = useState(State.None);

  useEffect(() => {
    const sub = TrackPlayer.addEventListener('playback-state', async () => {
      const s = await TrackPlayer.getState();
      setState(s);
      const id = await TrackPlayer.getCurrentTrack();
      if (id != null) {
        const t = await TrackPlayer.getTrack(id);
        setTrack(t);
      }
    });
    return () => sub.remove();
  }, []);

  const onSeek = async (delta) => {
    const pos = progress.position + delta;
    await TrackPlayer.seekTo(Math.max(0, Math.min(pos, progress.duration)));
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>{track?.title || 'No track'}</Text>
      <Text style={styles.artist}>{track?.artist || ''}</Text>
      <View style={styles.row}>
        <TouchableOpacity style={styles.control} onPress={() => TrackPlayer.skipToPrevious()}>
          <Text style={styles.controlText}>{'<<'}</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.control} onPress={togglePlayPause}>
          <Text style={styles.controlText}>{state === State.Playing ? 'Pause' : 'Play'}</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.control} onPress={() => TrackPlayer.skipToNext()}>
          <Text style={styles.controlText}>{'>>'}</Text>
        </TouchableOpacity>
      </View>
      <View style={styles.row}>
        <TouchableOpacity style={styles.small} onPress={() => onSeek(-10)}>
          <Text style={styles.smallText}>-10s</Text>
        </TouchableOpacity>
        <Text style={styles.time}>{Math.floor(progress.position)} / {Math.floor(progress.duration)}s</Text>
        <TouchableOpacity style={styles.small} onPress={() => onSeek(10)}>
          <Text style={styles.smallText}>+10s</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0b0b0b', padding: 20, justifyContent: 'center' },
  title: { color: '#fff', fontSize: 20, fontWeight: '700', textAlign: 'center' },
  artist: { color: '#999', textAlign: 'center', marginBottom: 24 },
  row: { flexDirection: 'row', justifyContent: 'center', alignItems: 'center', marginVertical: 10 },
  control: { backgroundColor: '#1DB954', paddingVertical: 12, paddingHorizontal: 18, borderRadius: 8, marginHorizontal: 10 },
  controlText: { color: '#000', fontWeight: '700' },
  small: { padding: 8 },
  smallText: { color: '#1DB954' },
  time: { color: '#fff', marginHorizontal: 10 }
});