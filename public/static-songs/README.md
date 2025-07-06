# Static Songs Directory

Place your MP3 files in this directory to have them available in the music player.

## Instructions:

1. Add your MP3 files to this directory
2. Update the `staticSongs` array in `/lib/staticSongs.ts` to include your new songs
3. Make sure the file paths match the actual files in this directory

## Example:

If you add a file named `my-song.mp3` to this directory, add an entry like this to the staticSongs array:

```typescript
{
  id: 'static-3',
  title: 'My Song',
  artist: 'My Artist',
  url: '/static-songs/my-song.mp3',
  source: 'static',
  duration: 210, // duration in seconds (optional)
}
```

## Supported Formats:

- MP3
- The audio element also supports other formats like WAV, OGG, but MP3 is recommended for best compatibility