THIS SHOULD BE A LINTER ERRORfrom flask import Flask, render_template, jsonify, send_from_directory, request
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
            
            # Look for audio files with more flexibility
            audio_file = None
            
            # Priority order for audio file selection
            audio_formats = [
                'VBR MP3', 'MP3', 'Ogg Vorbis', 'MPEG Audio', 
                'WAVE', 'FLAC', 'M4A', 'AAC'
            ]
            
            audio_extensions = ['.mp3', '.ogg', '.wav', '.flac', '.m4a', '.aac']
            
            # First, try to find files by format
            for format_type in audio_formats:
                for file in files:
                    if file.get('format', '').upper() == format_type.upper():
                        audio_file = file
                        break
                if audio_file:
                    break
            
            # If no format match, try by file extension
            if not audio_file:
                for extension in audio_extensions:
                    for file in files:
                        file_name = file.get('name', '').lower()
                        if file_name.endswith(extension):
                            audio_file = file
                            break
                    if audio_file:
                        break
            
            # If still no audio file, try looking for any file with audio keywords
            if not audio_file:
                audio_keywords = ['audio', 'song', 'music', 'track']
                for file in files:
                    file_name = file.get('name', '').lower()
                    file_format = file.get('format', '').lower()
                    
                    if any(keyword in file_name or keyword in file_format for keyword in audio_keywords):
                        # Avoid text files, images, etc.
                        if not any(ext in file_name for ext in ['.txt', '.pdf', '.jpg', '.png', '.gif', '.xml', '.json']):
                            audio_file = file
                            break
            
            if audio_file:
                file_url = f"{INTERNET_ARCHIVE_BASE_URL}/download/{identifier}/{urllib.parse.quote(audio_file['name'])}"
                print(f"Found audio file for {identifier}: {audio_file['name']} (format: {audio_file.get('format', 'unknown')})")
                return file_url
            else:
                # Last resort: try common file patterns
                common_patterns = [f"{identifier}.mp3", f"{identifier}.ogg", "01.mp3", "track01.mp3"]
                for pattern in common_patterns:
                    test_url = f"{INTERNET_ARCHIVE_BASE_URL}/download/{identifier}/{pattern}"
                    print(f"Trying fallback URL for {identifier}: {pattern}")
                    return test_url
                
    except Exception as e:
        print(f"Error getting song URL for {identifier}: {e}")
    
    print(f"No audio file found for {identifier}")
    return None

def search_internet_archive(query, page=1, rows=20):
    """Search Internet Archive for music"""
    try:
        print(f"Searching Internet Archive for: '{query}'")
        
        # Internet Archive uses 'start' not 'page' for pagination
        start_offset = (page - 1) * rows
        
        # Keep the search simple but effective
        params = {
            'q': f'{query} AND mediatype:audio',
            'fl': 'identifier,title,creator,date,description,downloads',
            'sort': 'downloads desc',
            'rows': rows,
            'start': start_offset,  # Use 'start' instead of 'page'
            'output': 'json'
        }
        
        print(f"Search params: {params}")
        
        response = requests.get(SEARCH_URL, params=params, timeout=15)
        print(f"API Response status: {response.status_code}")
        print(f"Response URL: {response.url}")
        
        if response.status_code == 200:
            data = response.json()
            print(f"Raw response keys: {list(data.keys())}")
            
            # Internet Archive API returns data in 'response' wrapper
            response_data = data.get('response', {})
            docs = response_data.get('docs', [])
            total_found = response_data.get('numFound', 0)
            
            print(f"Found {total_found} total results, processing {len(docs)} items")
            
            songs = []
            processed_count = 0
            
            for item in docs:
                identifier = item.get('identifier')
                if not identifier:
                    continue
                
                processed_count += 1
                
                # Try to get a working audio URL
                song_url = None
                try:
                    song_url = get_song_url_from_archive(identifier)
                except:
                    pass
                
                # Only add songs that have a potential audio URL
                if song_url:
                    song = {
                        "id": identifier,
                        "title": item.get('title', 'Unknown Title'),
                        "artist": item.get('creator', ['Unknown Artist'])[0] if isinstance(item.get('creator'), list) else item.get('creator', 'Unknown Artist'),
                        "url": song_url,
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
                
                # Add delay every few items
                if processed_count % 5 == 0:
                    time.sleep(0.2)
                
                # If we have enough songs, break early to improve performance
                if len(songs) >= 10:
                    break
            
            print(f"Returning {len(songs)} playable songs out of {processed_count} processed")
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
        
        # Use a broader search for better chances of finding playable audio
        params = {
            'q': 'mediatype:audio AND (collection:opensource_audio OR collection:etree)',
            'fl': 'identifier,title,creator,date,description,downloads',
            'sort': 'downloads desc',
            'rows': limit * 3,  # Get more to filter for working ones
            'output': 'json'
        }
        
        response = requests.get(SEARCH_URL, params=params, timeout=15)
        if response.status_code == 200:
            data = response.json()
            print(f"Popular songs response keys: {list(data.keys())}")
            
            # Internet Archive API returns data in 'response' wrapper
            response_data = data.get('response', {})
            docs = response_data.get('docs', [])
            
            print(f"Got {len(docs)} potential popular songs from API")
            
            songs = []
            processed_count = 0
            
            for item in docs:
                identifier = item.get('identifier')
                if not identifier:
                    continue
                
                processed_count += 1
                
                # Try to get a working audio URL - but don't spend too much time
                song_url = None
                try:
                    song_url = get_song_url_from_archive(identifier)
                except:
                    pass
                
                # Only add songs with working URLs
                if song_url:
                    song = {
                        "id": identifier,
                        "title": item.get('title', 'Unknown Title'),
                        "artist": item.get('creator', ['Unknown Artist'])[0] if isinstance(item.get('creator'), list) else item.get('creator', 'Unknown Artist'),
                        "url": song_url,
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
                
                # Stop if we have enough songs or processed enough items
                if len(songs) >= limit or processed_count >= limit * 2:
                    break
                
                # Small delay every few items
                if processed_count % 3 == 0:
                    time.sleep(0.2)
            
            print(f"Returning {len(songs)} playable popular songs out of {processed_count} processed")
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
        import traceback
        print(f"Debug search error: {e}")
        traceback.print_exc()
        return jsonify({'error': str(e)}), 500

# Raw API test endpoint
@app.route('/api/debug/raw/<query>')
def debug_raw(query):
    """Test raw API response"""
    try:
        params = {
            'q': f'{query} AND mediatype:audio',
            'fl': 'identifier,title,creator',
            'rows': 3,
            'output': 'json'
        }
        
        response = requests.get(SEARCH_URL, params=params, timeout=15)
        
        if response.status_code == 200:
            data = response.json()
            return jsonify({
                'status': 'success',
                'response_keys': list(data.keys()),
                'raw_response': data,
                'url_used': response.url
            })
        else:
            return jsonify({
                'status': 'failed',
                'status_code': response.status_code,
                'response_text': response.text[:500]
            })
            
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# Debug endpoint to inspect files in an archive item
@app.route('/api/debug/files/<identifier>')
def debug_files(identifier):
    """Debug endpoint to see what files are in an archive item"""
    try:
        response = requests.get(f'{METADATA_URL}/{identifier}/files', timeout=10)
        
        if response.status_code == 200:
            files_data = response.json()
            files = files_data.get('files', [])
            
            # Analyze the files
            file_info = []
            for file in files[:20]:  # Limit to first 20 files
                file_info.append({
                    'name': file.get('name', 'No name'),
                    'format': file.get('format', 'Unknown'),
                    'size': file.get('size', 'Unknown'),
                    'is_audio': any(ext in file.get('name', '').lower() for ext in ['.mp3', '.ogg', '.wav', '.flac', '.m4a'])
                })
            
            return jsonify({
                'identifier': identifier,
                'total_files': len(files),
                'files_shown': len(file_info),
                'files': file_info,
                'metadata_url': f'{METADATA_URL}/{identifier}/files'
            })
        else:
            return jsonify({
                'error': f'Failed to get files for {identifier}',
                'status_code': response.status_code
            })
            
    except Exception as e:
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5600)