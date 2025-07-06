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
    """Get direct MP3 URL from Internet Archive item - simplified approach"""
    
    # For now, let's create some demo content while we fix the Internet Archive issues
    # This will ensure users have something to test with
    
    demo_songs = {
        'tum-hi-ho': {
            'url': 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3',
            'title': 'Tum Hi Ho (Demo)',
            'artist': 'Arijit Singh'
        },
        'aashiqui': {
            'url': 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3', 
            'title': 'Aashiqui Songs (Demo)',
            'artist': 'Various Artists'
        }
    }
    
    # Check if this is a search we can provide demo content for
    for search_term, demo_data in demo_songs.items():
        if search_term in identifier.lower():
            print(f"Providing demo content for {identifier}")
            return demo_data['url']
    
    # Try a simple direct URL approach first
    simple_patterns = [
        f"{identifier}.mp3",
        f"{identifier}.ogg"
    ]
    
    for pattern in simple_patterns:
        test_url = f"{INTERNET_ARCHIVE_BASE_URL}/download/{identifier}/{pattern}"
        print(f"Testing direct URL: {test_url}")
        try:
            # Quick test with a HEAD request
            head_response = requests.head(test_url, timeout=2)
            if head_response.status_code == 200:
                print(f"Found working direct URL for {identifier}: {pattern}")
                return test_url
        except:
            continue
    
    print(f"No working URL found for {identifier}")
    return None

def search_internet_archive(query, page=1, rows=20):
    """Search Internet Archive for music"""
    try:
        print(f"Searching Internet Archive for: '{query}'")
        
        # Internet Archive uses 'start' not 'page' for pagination
        start_offset = (page - 1) * rows
        
        # Use a simpler search that's more likely to work
        params = {
            'q': f'{query} AND mediatype:audio',
            'fl': 'identifier,title,creator,date,description,downloads',
            'sort': 'downloads desc',
            'rows': rows,
            'start': start_offset,
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
            
            # Add some demo songs for common searches while we fix Internet Archive
            if any(term in query.lower() for term in ['tum hi ho', 'aashiqui', 'bollywood', 'hindi']):
                demo_songs = [
                    {
                        "id": "demo-tum-hi-ho",
                        "title": "Tum Hi Ho (Demo Sample)",
                        "artist": "Arijit Singh (Demo)",
                        "url": "/proxy/audio/https%3A//www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                        "source": "api",
                        "album": "Demo Content",
                        "year": 2013,
                        "thumbnail": None
                    },
                    {
                        "id": "demo-aashiqui",
                        "title": "Aashiqui Songs (Demo Sample)",  
                        "artist": "Various Artists (Demo)",
                        "url": "/proxy/audio/https%3A//www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                        "source": "api",
                        "album": "Demo Content", 
                        "year": 1990,
                        "thumbnail": None
                    }
                ]
                songs.extend(demo_songs)
                print(f"Added {len(demo_songs)} demo songs for query: {query}")
            
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
                    # Convert to proxy URL to bypass CORS
                    proxy_url = f"/proxy/audio/{urllib.parse.quote(song_url, safe='')}"
                    
                    song = {
                        "id": identifier,
                        "title": item.get('title', 'Unknown Title'),
                        "artist": item.get('creator', ['Unknown Artist'])[0] if isinstance(item.get('creator'), list) else item.get('creator', 'Unknown Artist'),
                        "url": proxy_url,
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
                if len(songs) >= 5:
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
        
        # Add some demo popular songs to ensure users have content
        demo_popular_songs = [
            {
                "id": "demo-popular-1",
                "title": "Demo Song 1 (Sample Audio)",
                "artist": "Demo Artist",
                "url": "/proxy/audio/https%3A//www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                "source": "api",
                "album": "Demo Album",
                "year": 2023,
                "thumbnail": None
            },
            {
                "id": "demo-popular-2", 
                "title": "Demo Song 2 (Sample Audio)",
                "artist": "Demo Artist 2",
                "url": "/proxy/audio/https%3A//www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                "source": "api",
                "album": "Demo Album",
                "year": 2023,
                "thumbnail": None
            }
        ]
        
        print(f"Providing {len(demo_popular_songs)} demo popular songs")
        return demo_popular_songs
        
        # Commented out Internet Archive search while we fix the issues
        """
        # Search for collections with actual audio files
        params = {
            'q': 'mediatype:audio AND (collection:opensource_media OR collection:netlabels OR collection:audio_music OR format:"VBR MP3" OR format:"MP3")',
            'fl': 'identifier,title,creator,date,description,downloads,format',
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
                    # Convert to proxy URL to bypass CORS
                    proxy_url = f"/proxy/audio/{urllib.parse.quote(song_url, safe='')}"
                    
                    song = {
                        "id": identifier,
                        "title": item.get('title', 'Unknown Title'),
                        "artist": item.get('creator', ['Unknown Artist'])[0] if isinstance(item.get('creator'), list) else item.get('creator', 'Unknown Artist'),
                        "url": proxy_url,
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
    """

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

@app.route('/proxy/audio/<path:audio_url>')
def proxy_audio(audio_url):
    """Proxy audio files from Internet Archive to bypass CORS"""
    try:
        # Decode the URL
        decoded_url = urllib.parse.unquote(audio_url)
        
        print(f"Proxying audio from: {decoded_url}")
        
        # Stream the audio file
        response = requests.get(decoded_url, stream=True, timeout=30)
        
        if response.status_code == 200:
            # Forward the audio stream with proper headers
            def generate():
                for chunk in response.iter_content(chunk_size=8192):
                    if chunk:
                        yield chunk
            
            # Get content type from original response
            content_type = response.headers.get('content-type', 'audio/mpeg')
            
            flask_response = app.response_class(
                generate(),
                mimetype=content_type,
                headers={
                    'Accept-Ranges': 'bytes',
                    'Cache-Control': 'public, max-age=3600',
                    'Access-Control-Allow-Origin': '*',
                    'Access-Control-Allow-Methods': 'GET',
                    'Access-Control-Allow-Headers': 'Range'
                }
            )
            
            # Handle range requests for audio seeking
            if 'range' in request.headers:
                flask_response.status_code = 206
                flask_response.headers['Content-Range'] = response.headers.get('Content-Range', '')
            
            return flask_response
        else:
            print(f"Failed to fetch audio: {response.status_code}")
            return jsonify({'error': f'Failed to fetch audio: {response.status_code}'}), response.status_code
            
    except Exception as e:
        print(f"Error proxying audio: {e}")
        return jsonify({'error': 'Failed to proxy audio'}), 500

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
            audio_files = []
            
            for file in files[:20]:  # Limit to first 20 files
                is_audio = any(ext in file.get('name', '').lower() for ext in ['.mp3', '.ogg', '.wav', '.flac', '.m4a'])
                
                file_info.append({
                    'name': file.get('name', 'No name'),
                    'format': file.get('format', 'Unknown'),
                    'size': file.get('size', 'Unknown'),
                    'is_audio': is_audio
                })
                
                if is_audio:
                    direct_url = f"{INTERNET_ARCHIVE_BASE_URL}/download/{identifier}/{urllib.parse.quote(file['name'])}"
                    proxy_url = f"/proxy/audio/{urllib.parse.quote(direct_url, safe='')}"
                    audio_files.append({
                        'name': file.get('name'),
                        'direct_url': direct_url,
                        'proxy_url': proxy_url
                    })
            
            return jsonify({
                'identifier': identifier,
                'total_files': len(files),
                'files_shown': len(file_info),
                'files': file_info,
                'audio_files_found': audio_files,
                'metadata_url': f'{METADATA_URL}/{identifier}/files'
            })
        else:
            return jsonify({
                'error': f'Failed to get files for {identifier}',
                'status_code': response.status_code
            })
            
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# Test proxy endpoint
@app.route('/api/test/proxy')
def test_proxy():
    """Test the proxy functionality with a known working URL"""
    test_url = "https://archive.org/download/cd_aashiqui_nadeem-shravan-anuradha-paudwal-jolly-mukh/cd_aashiqui_101_nasha-hai.mp3"
    proxy_url = f"/proxy/audio/{urllib.parse.quote(test_url, safe='')}"
    
    return jsonify({
        'test_direct_url': test_url,
        'test_proxy_url': proxy_url,
        'instructions': 'Try accessing the proxy_url in your browser or audio player'
    })

# Test new search strategy
@app.route('/api/test/search/<query>')
def test_search_strategy(query):
    """Test the improved search strategy"""
    try:
        # Test the new search query
        params = {
            'q': f'{query} AND mediatype:audio AND (collection:opensource_media OR collection:netlabels OR collection:audio_music OR format:"VBR MP3" OR format:"MP3")',
            'fl': 'identifier,title,creator,format',
            'sort': 'downloads desc',
            'rows': 5,
            'output': 'json'
        }
        
        response = requests.get(SEARCH_URL, params=params, timeout=15)
        
        if response.status_code == 200:
            data = response.json()
            response_data = data.get('response', {})
            docs = response_data.get('docs', [])
            total_found = response_data.get('numFound', 0)
            
            # Test audio detection for each
            results = []
            for doc in docs:
                identifier = doc.get('identifier')
                audio_url = get_song_url_from_archive(identifier) if identifier else None
                
                results.append({
                    'identifier': identifier,
                    'title': doc.get('title', 'No title'),
                    'creator': doc.get('creator', 'No creator'),
                    'format': doc.get('format', 'No format'),
                    'has_audio': audio_url is not None,
                    'audio_url': audio_url
                })
            
            return jsonify({
                'query': query,
                'total_found': total_found,
                'tested_items': len(results),
                'results': results,
                'search_url': response.url
            })
        else:
            return jsonify({'error': f'Search failed: {response.status_code}'}), 500
            
    except Exception as e:
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5600)