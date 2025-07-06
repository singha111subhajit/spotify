# Enhanced Music Player

A modern, feature-rich music player built with Next.js, React, and TypeScript that combines local MP3 files with the vast Internet Archive music collection.

## Features

### 🎵 Dual Source Music Library
- **Static MP3 Files**: Play your local music collection
- **Internet Archive Integration**: Access thousands of songs from the Internet Archive
- **Unified Interface**: Both sources displayed together seamlessly

### 🔍 Smart Search
- Search the Internet Archive's vast music collection
- Real-time search with loading indicators
- Search suggestions for popular genres
- Filter and combine results from both static and API sources

### 🎛️ Full Music Player Controls
- Play/pause, next/previous track
- Volume control with visual slider
- Progress bar with seeking capability
- Shuffle and repeat modes
- Visual loading indicators

### 📱 Modern UI/UX
- Dark theme with beautiful gradients
- Responsive design (mobile-friendly)
- Song thumbnails and metadata display
- Source indicators (local vs. Internet Archive)
- Smooth animations and transitions

### 📄 Pagination
- Efficient pagination for large song collections
- Configurable items per page
- Smart pagination controls

### 🎲 Smart Defaults
- Random song selection on startup
- Popular songs loaded by default
- Fallback to local files if API is unavailable

## Tech Stack

- **Framework**: Next.js 14 with App Router
- **Frontend**: React 18 with TypeScript
- **Styling**: Tailwind CSS
- **Icons**: Lucide React
- **HTTP Client**: Axios
- **Audio**: HTML5 Audio API

## Getting Started

### Prerequisites
- Node.js 18+ and npm

### Installation

1. **Clone and install dependencies:**
   ```bash
   npm install
   ```

2. **Add your static MP3 files:**
   - Place MP3 files in the `public/static-songs/` directory
   - Update the `staticSongs` array in `lib/staticSongs.ts` with your songs

3. **Run the development server:**
   ```bash
   npm run dev
   ```

4. **Open your browser:**
   Navigate to [http://localhost:3000](http://localhost:3000)

## Usage

### Adding Static Songs

1. Place your MP3 files in `public/static-songs/`
2. Edit `lib/staticSongs.ts` and add entries like:

```typescript
{
  id: 'static-3',
  title: 'Your Song Title',
  artist: 'Artist Name',
  url: '/static-songs/your-file.mp3',
  source: 'static',
  duration: 210, // optional, in seconds
}
```

### Search Feature

- Use the search bar to find songs from the Internet Archive
- Try searches like "classical", "jazz", "rock", "ambient"
- Results include both your local files and Internet Archive matches
- Click any song to start playing immediately

### Player Controls

- **Play/Pause**: Click the main play button
- **Skip**: Use next/previous buttons
- **Seek**: Click anywhere on the progress bar
- **Volume**: Adjust with the volume slider
- **Shuffle**: Randomize playback order
- **Repeat**: Loop the current song

## Project Structure

```
├── app/                    # Next.js app directory
│   ├── layout.tsx         # Root layout
│   ├── page.tsx           # Main page component
│   └── globals.css        # Global styles
├── components/            # React components
│   ├── MusicPlayer.tsx    # Main player controls
│   ├── SongList.tsx       # Song list with pagination
│   └── SearchBar.tsx      # Search functionality
├── hooks/                 # Custom React hooks
│   └── useMusicPlayer.ts  # Music player state management
├── lib/                   # Utility libraries
│   ├── internetArchiveApi.ts  # Internet Archive API integration
│   └── staticSongs.ts     # Static song definitions
├── types/                 # TypeScript type definitions
│   └── index.ts           # All type definitions
├── public/
│   └── static-songs/      # Static MP3 files directory
└── configuration files    # Next.js, Tailwind, TypeScript configs
```

## API Integration

The app integrates with the Internet Archive's search API to provide access to thousands of songs. The integration includes:

- Rate limiting to respect API guidelines
- Error handling and fallbacks
- Metadata extraction (title, artist, year, album)
- Thumbnail image support
- Smart filtering for audio content

## Browser Compatibility

- Chrome/Chromium: Full support
- Firefox: Full support
- Safari: Full support
- Edge: Full support

## Development

### Scripts

- `npm run dev`: Start development server
- `npm run build`: Build for production
- `npm run start`: Start production server
- `npm run lint`: Run ESLint

### Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## Deployment

The app can be deployed to any platform that supports Next.js:

- **Vercel**: Optimal for Next.js applications
- **Netlify**: Static site deployment
- **Docker**: Containerized deployment

### Build for Production

```bash
npm run build
npm run start
```

## License

This project is open source and available under the MIT License.

## Acknowledgments

- **Internet Archive**: For providing the vast music collection API
- **Next.js**: For the excellent React framework
- **Tailwind CSS**: For the utility-first CSS framework
- **Lucide**: For the beautiful icons

---

Enjoy your enhanced music experience! 🎵