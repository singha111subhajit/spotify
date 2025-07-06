from flask import Flask, render_template, jsonify, send_from_directory, request
from flask_cors import CORS
import os
import urllib.parse
import requests
import time
import random
from mutagen.mp3 import MP3
from mutagen.id3 import ID3, TIT2, TPE1

app = Flask(__name__)
CORS(app)  # Enable CORS for React frontend

# Configuration
SONGS_FOLDER = 'static/songs'
ALLOWED_EXTENSIONS = {'mp3', 'wav', 'ogg'}

# Internet Archive API Configuration
INTERNET_ARCHIVE_BASE_URL = 'https://archive.org'
SEARCH_URL = f'{INTERNET_ARCHIVE_BASE_URL}/advancedsearch.php'
METADATA_URL = f'{INTERNET_ARCHIVE_BASE_URL}/metadata'

def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def get_static_songs():
    """Get list of static/local MP3 files"""
    songs = []
    songs_path = os.path.join(app.static_folder, 'songs')

    if not os.path.exists(songs_path):
        os.makedirs(songs_path)
        return songs

    for i, filename in enumerate(os.listdir(songs_path), start=1):
        if allowed_file(filename):
            file_path = os.path.join(songs_path, filename)
            title = None
            artist = None
            duration = None

            try:
                # Load MP3 and extract duration
                audio = MP3(file_path)
                duration = audio.info.length
                
                # Try to extract ID3 tags (title, artist)
                tags = ID3(file_path)
                title = tags.get("TIT2")
                artist = tags.get("TPE1")
                title = title.text[0] if title else None
                artist = artist.text[0] if artist else None

            except Exception as e:
                print(f"Warning: Could not read tags from {filename}: {e}")

            # Fall back to filename if tags are missing
            base_name = os.path.splitext(filename)[0]
            pretty_title = title or base_name.replace('_', ' ').replace('-', ' ').title()
            pretty_artist = artist or "Unknown Artist"

            encoded_filename = urllib.parse.quote(filename)

            songs.append({
                "id": f"static-{i}",
                "title": pretty_title,
                "artist": pretty_artist,
                "duration": duration,
                "url": f"/songs/{encoded_filename}",
                "source": "static",
                "thumbnail": None
            })

    return songs

def get_song_url_from_archive(identifier):
    """Get direct MP3 URL from Internet Archive item"""
    try:
        response = requests.get(f'{METADATA_URL}/{identifier}/files', timeout=5)
        if response.status_code == 200:
            files_data = response.json()
            files = files_data.get('files', [])
            
            # Find MP3 file
            mp3_file = None
            for file in files:
                if file.get('format') in ['VBR MP3', 'MP3'] or file.get('name', '').lower().endswith('.mp3'):
                    mp3_file = file
                    break
            
            if mp3_file:
                return f"{INTERNET_ARCHIVE_BASE_URL}/download/{identifier}/{urllib.parse.quote(mp3_file['name'])}"
    except Exception as e:
        print(f"Error getting song URL for {identifier}: {e}")
    
    return None

def search_internet_archive(query, page=1, rows=20):
    """Search Internet Archive for music"""
    try:
        # Add small delay to respect rate limits
        time.sleep(0.1)
        
        params = {
            'q': f'collection:(etree OR opensource_audio) AND format:(MP3) AND title:({query}) AND mediatype:audio',
            'fl': 'identifier,title,creator,date,description,downloads,item_size',
            'sort': 'downloads desc',
            'rows': rows,
            'page': page,
            'output': 'json'
        }
        
        response = requests.get(SEARCH_URL, params=params, timeout=10)
        if response.status_code == 200:
            data = response.json()
            docs = data.get('docs', [])
            total_found = data.get('numFound', 0)
            
            songs = []
            for item in docs:
                # Get direct song URL
                song_url = get_song_url_from_archive(item['identifier'])
                if song_url:
                    songs.append({
                        "id": item['identifier'],
                        "title": item.get('title', 'Unknown Title'),
                        "artist": item.get('creator', 'Unknown Artist'),
                        "url": song_url,
                        "source": "api",
                        "album": item.get('collection', [None])[0] if item.get('collection') else None,
                        "year": int(item['date'][:4]) if item.get('date') else None,
                        "thumbnail": f"{INTERNET_ARCHIVE_BASE_URL}/services/img/{item['identifier']}"
                    })
            
            return songs, total_found
            
    except Exception as e:
        print(f"Error searching Internet Archive: {e}")
    
    return [], 0

def get_popular_songs(limit=20):
    """Get popular songs from Internet Archive"""
    try:
        params = {
            'q': 'collection:(etree OR opensource_audio) AND format:(MP3) AND mediatype:audio',
            'fl': 'identifier,title,creator,date,description,downloads',
            'sort': 'downloads desc',
            'rows': limit,
            'output': 'json'
        }
        
        response = requests.get(SEARCH_URL, params=params, timeout=15)
        if response.status_code == 200:
            data = response.json()
            docs = data.get('docs', [])
            
            songs = []
            # Process in smaller batches to avoid overwhelming the API
            for i, item in enumerate(docs[:limit]):
                if i > 0 and i % 5 == 0:
                    time.sleep(0.5)  # Rate limiting
                
                song_url = get_song_url_from_archive(item['identifier'])
                if song_url:
                    songs.append({
                        "id": item['identifier'],
                        "title": item.get('title', 'Unknown Title'),
                        "artist": item.get('creator', 'Unknown Artist'),
                        "url": song_url,
                        "source": "api",
                        "album": item.get('collection', [None])[0] if item.get('collection') else None,
                        "year": int(item['date'][:4]) if item.get('date') else None,
                        "thumbnail": f"{INTERNET_ARCHIVE_BASE_URL}/services/img/{item['identifier']}"
                    })
            
            return songs
            
    except Exception as e:
        print(f"Error fetching popular songs: {e}")
    
    return []

# API Routes
@app.route('/api/songs')
def api_songs():
    """Get combined list of static and popular API songs"""
    try:
        static_songs = get_static_songs()
        popular_songs = get_popular_songs(20)
        
        all_songs = static_songs + popular_songs
        
        # Pagination
        page = request.args.get('page', 1, type=int)
        per_page = request.args.get('per_page', 10, type=int)
        
        start_idx = (page - 1) * per_page
        end_idx = start_idx + per_page
        paginated_songs = all_songs[start_idx:end_idx]
        
        return jsonify({
            'songs': paginated_songs,
            'total': len(all_songs),
            'page': page,
            'per_page': per_page,
            'total_pages': (len(all_songs) + per_page - 1) // per_page
        })
    except Exception as e:
        print(f"Error in api_songs: {e}")
        return jsonify({'error': 'Failed to fetch songs'}), 500

@app.route('/api/search')
def api_search():
    """Search for songs using Internet Archive API"""
    try:
        query = request.args.get('q', '')
        page = request.args.get('page', 1, type=int)
        per_page = request.args.get('per_page', 20, type=int)
        
        if not query:
            return jsonify({'error': 'Query parameter required'}), 400
        
        # Search API songs
        api_songs, total_found = search_internet_archive(query, page, per_page)
        
        # Also search static songs
        static_songs = get_static_songs()
        matching_static = [
            song for song in static_songs 
            if query.lower() in song['title'].lower() or query.lower() in song['artist'].lower()
        ]
        
        # Combine results (static songs first)
        all_results = matching_static + api_songs
        
        return jsonify({
            'songs': all_results,
            'total': len(matching_static) + total_found,
            'page': page,
            'per_page': per_page,
            'query': query
        })
    except Exception as e:
        print(f"Error in api_search: {e}")
        return jsonify({'error': 'Search failed'}), 500

@app.route('/api/random')
def api_random():
    """Get a random song for default selection"""
    try:
        static_songs = get_static_songs()
        
        if static_songs:
            # Prefer static songs for random selection
            return jsonify(random.choice(static_songs))
        else:
            # Fallback to popular songs
            popular_songs = get_popular_songs(10)
            if popular_songs:
                return jsonify(random.choice(popular_songs))
        
        return jsonify({'error': 'No songs available'}), 404
    except Exception as e:
        print(f"Error in api_random: {e}")
        return jsonify({'error': 'Failed to get random song'}), 500

@app.route('/songs/<filename>')
def serve_song(filename):
    """Serve static audio files"""
    return send_from_directory(os.path.join(app.static_folder, 'songs'), filename)

@app.route('/')
def index():
    """Serve the main page - will serve React app in production"""
    return render_template('index.html')

# Health check endpoint
@app.route('/api/health')
def health_check():
    return jsonify({'status': 'healthy', 'service': 'music_player_api'})

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5600)