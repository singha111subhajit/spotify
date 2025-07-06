from flask import Flask, render_template, jsonify, send_from_directory, request
from flask_cors import CORS
import os
import urllib.parse
import requests
import time
import random
from mutagen.mp3 import MP3
from mutagen.id3 import ID3, TIT2, TPE1, ID3NoHeaderError

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
                
                # Try to extract ID3 tags (title, artist) - handle files without ID3 tags
                try:
                    tags = ID3(file_path)
                    title = tags.get("TIT2")
                    artist = tags.get("TPE1")
                    title = title.text[0] if title else None
                    artist = artist.text[0] if artist else None
                except ID3NoHeaderError:
                    # File doesn't have ID3 tags, that's fine
                    pass

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
        response = requests.get(f'{METADATA_URL}/{identifier}/files', timeout=10)
        if response.status_code == 200:
            files_data = response.json()
            files = files_data.get('files', [])
            
            # Find MP3 file - be more flexible with formats
            mp3_file = None
            for file in files:
                file_name = file.get('name', '').lower()
                file_format = file.get('format', '').lower()
                
                if (file_format in ['vbr mp3', 'mp3', 'mpeg audio'] or 
                    file_name.endswith('.mp3') or 
                    file_name.endswith('.m4a')):
                    mp3_file = file
                    break
            
            if mp3_file:
                file_url = f"{INTERNET_ARCHIVE_BASE_URL}/download/{identifier}/{urllib.parse.quote(mp3_file['name'])}"
                print(f"Found audio file for {identifier}: {mp3_file['name']}")
                return file_url
            else:
                print(f"No audio file found for {identifier}")
                # Return a placeholder URL that might work
                return f"{INTERNET_ARCHIVE_BASE_URL}/download/{identifier}"
                
    except Exception as e:
        print(f"Error getting song URL for {identifier}: {e}")
    
    return None

def search_internet_archive(query, page=1, rows=20):
    """Search Internet Archive for music"""
    try:
        print(f"Searching Internet Archive for: '{query}'")
        
        # Simplified and more flexible search query
        params = {
            'q': f'{query} AND mediatype:audio',
            'fl': 'identifier,title,creator,date,description,downloads',
            'sort': 'downloads desc',
            'rows': rows,
            'page': page,
            'output': 'json'
        }
        
        print(f"Search params: {params}")
        
        response = requests.get(SEARCH_URL, params=params, timeout=15)
        print(f"API Response status: {response.status_code}")
        
        if response.status_code == 200:
            data = response.json()
            docs = data.get('docs', [])
            total_found = data.get('numFound', 0)
            
            print(f"Found {total_found} total results, processing {len(docs)} items")
            
            songs = []
            for item in docs:
                identifier = item.get('identifier')
                if not identifier:
                    continue
                
                # Create song entry even if we can't get direct URL
                song = {
                    "id": identifier,
                    "title": item.get('title', 'Unknown Title'),
                    "artist": item.get('creator', ['Unknown Artist'])[0] if isinstance(item.get('creator'), list) else item.get('creator', 'Unknown Artist'),
                    "url": f"{INTERNET_ARCHIVE_BASE_URL}/download/{identifier}",  # Default URL
                    "source": "api",
                    "album": None,
                    "year": None,
                    "thumbnail": f"{INTERNET_ARCHIVE_BASE_URL}/services/img/{identifier}"
                }
                
                # Try to get a better URL, but don't fail if we can't
                try:
                    better_url = get_song_url_from_archive(identifier)
                    if better_url:
                        song["url"] = better_url
                except:
                    pass  # Use default URL
                
                # Extract year if available
                if item.get('date'):
                    try:
                        song["year"] = int(item['date'][:4])
                    except:
                        pass
                
                songs.append(song)
                
                # Add small delay to be respectful to the API
                time.sleep(0.1)
            
            print(f"Returning {len(songs)} songs")
            return songs, total_found
            
    except Exception as e:
        print(f"Error searching Internet Archive: {e}")
        import traceback
        traceback.print_exc()
    
    return [], 0

def get_popular_songs(limit=10):
    """Get popular songs from Internet Archive"""
    try:
        print(f"Fetching {limit} popular songs...")
        
        params = {
            'q': 'mediatype:audio AND collection:opensource_audio',
            'fl': 'identifier,title,creator,date,description,downloads',
            'sort': 'downloads desc',
            'rows': limit,
            'output': 'json'
        }
        
        response = requests.get(SEARCH_URL, params=params, timeout=15)
        if response.status_code == 200:
            data = response.json()
            docs = data.get('docs', [])
            
            print(f"Got {len(docs)} popular songs from API")
            
            songs = []
            for i, item in enumerate(docs):
                identifier = item.get('identifier')
                if not identifier:
                    continue
                
                song = {
                    "id": identifier,
                    "title": item.get('title', 'Unknown Title'),
                    "artist": item.get('creator', ['Unknown Artist'])[0] if isinstance(item.get('creator'), list) else item.get('creator', 'Unknown Artist'),
                    "url": f"{INTERNET_ARCHIVE_BASE_URL}/download/{identifier}",
                    "source": "api",
                    "album": None,
                    "year": None,
                    "thumbnail": f"{INTERNET_ARCHIVE_BASE_URL}/services/img/{identifier}"
                }
                
                # Extract year if available
                if item.get('date'):
                    try:
                        song["year"] = int(item['date'][:4])
                    except:
                        pass
                
                songs.append(song)
                
                # Small delay every few items
                if i > 0 and i % 3 == 0:
                    time.sleep(0.2)
            
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
        print(f"Found {len(static_songs)} static songs")
        
        # Get fewer popular songs to reduce load time
        popular_songs = get_popular_songs(5)
        print(f"Found {len(popular_songs)} popular songs")
        
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
        import traceback
        traceback.print_exc()
        return jsonify({'error': 'Failed to fetch songs'}), 500

@app.route('/api/search')
def api_search():
    """Search for songs using Internet Archive API"""
    try:
        query = request.args.get('q', '')
        page = request.args.get('page', 1, type=int)
        per_page = request.args.get('per_page', 20, type=int)
        
        print(f"Search request: query='{query}', page={page}, per_page={per_page}")
        
        if not query:
            return jsonify({'error': 'Query parameter required'}), 400
        
        # Search static songs first
        static_songs = get_static_songs()
        matching_static = [
            song for song in static_songs 
            if query.lower() in song['title'].lower() or query.lower() in song['artist'].lower()
        ]
        
        print(f"Found {len(matching_static)} matching static songs")
        
        # Search API songs
        api_songs, total_found = search_internet_archive(query, page, per_page)
        
        print(f"Found {len(api_songs)} API songs")
        
        # Combine results (static songs first)
        all_results = matching_static + api_songs
        
        response_data = {
            'songs': all_results,
            'total': len(matching_static) + total_found,
            'page': page,
            'per_page': per_page,
            'query': query,
            'static_matches': len(matching_static),
            'api_matches': len(api_songs)
        }
        
        print(f"Returning {len(all_results)} total songs")
        return jsonify(response_data)
        
    except Exception as e:
        print(f"Error in api_search: {e}")
        import traceback
        traceback.print_exc()
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
            popular_songs = get_popular_songs(5)
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

# Debug endpoint to test API directly
@app.route('/api/debug/search/<query>')
def debug_search(query):
    """Debug endpoint to test search directly"""
    try:
        songs, total = search_internet_archive(query, 1, 5)
        return jsonify({
            'query': query,
            'songs_found': len(songs),
            'total_available': total,
            'songs': songs
        })
    except Exception as e:
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5600)