import TrackPlayer, { State } from 'react-native-track-player';

export function convertSongToTrack(song) {
  return {
    id: String(song.id),
    url: song.localPath || song.url,
    title: song.title,
    artist: song.artist || 'Unknown Artist',
    artwork: song.thumbnail || undefined
  };
}

export async function loadQueue(songs) {
  const tracks = songs.map(convertSongToTrack);
  await TrackPlayer.reset();
  await TrackPlayer.add(tracks);
}

export async function playTrackAtIndex(index) {
  await TrackPlayer.skip(index);
  await TrackPlayer.play();
}

export async function getPlaybackState() {
  return TrackPlayer.getState();
}

export async function togglePlayPause() {
  const state = await TrackPlayer.getState();
  if (state === State.Playing) {
    return TrackPlayer.pause();
  }
  return TrackPlayer.play();
}