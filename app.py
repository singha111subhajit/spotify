from flask import Flask, render_template, jsonify, send_from_directory, request
from flask_cors import CORS
import os
import urllib.parse
import requests
import random
from mutagen.mp3 import MP3
from mutagen.id3 import ID3, TIT2, TPE1, TALB, TDRC, TYER, ID3NoHeaderError
import sqlite3
import jwt
from werkzeug.security import generate_password_hash, check_password_hash
from datetime import datetime, timedelta
from functools import wraps

app = Flask(__name__)
CORS(app)  # Enable CORS for React frontend

# Configuration
SONGS_FOLDER = 'static/songs'
ALLOWED_EXTENSIONS = {'mp3', 'wav', 'ogg'}


# JioSaavn API endpoint (unofficial public API)
JIOSAAVN_API_BASE = 'https://saavn.dev/api'

# JWT secret key (should be in env in production)
JWT_SECRET = 'supersecretkey'
JWT_ALGO = 'HS256'
JWT_EXP_DELTA_SECONDS = 7 * 24 * 3600  # 7 days

DB_PATH = 'music_app.db'

def get_db():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn

def init_db():
    conn = get_db()
    c = conn.cursor()
    # User table
    c.execute('''CREATE TABLE IF NOT EXISTS user (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT NOT NULL,
        user_id TEXT NOT NULL UNIQUE,
        password TEXT NOT NULL
    )''')
    # Playlist table
    c.execute('''CREATE TABLE IF NOT EXISTS playlist (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        user_id INTEGER NOT NULL,
        FOREIGN KEY(user_id) REFERENCES user(id)
    )''')
    # PlaylistSong table
    c.execute('''CREATE TABLE IF NOT EXISTS playlistsong (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        playlist_id INTEGER NOT NULL,
        song_id TEXT NOT NULL,
        song_title TEXT NOT NULL,
        FOREIGN KEY(playlist_id) REFERENCES playlist(id)
    )''')
    conn.commit()
    conn.close()

init_db()

# --- JWT Auth Helpers ---
def create_jwt(user_id):
    payload = {
        'user_id': user_id,
        'exp': datetime.utcnow() + timedelta(seconds=JWT_EXP_DELTA_SECONDS)
    }
    return jwt.encode(payload, JWT_SECRET, algorithm=JWT_ALGO)

def decode_jwt(token):
    try:
        payload = jwt.decode(token, JWT_SECRET, algorithms=[JWT_ALGO])
        return payload['user_id']
    except Exception:
        return None

def login_required(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        auth = request.headers.get('Authorization', None)
        if not auth or not auth.startswith('Bearer '):
            return jsonify({'error': 'Missing or invalid token'}), 401
        token = auth.split(' ')[1]
        user_id = decode_jwt(token)
        if not user_id:
            return jsonify({'error': 'Invalid or expired token'}), 401
        request.user_id = user_id
        return f(*args, **kwargs)
    return decorated

# --- User Endpoints ---
@app.route('/register', methods=['POST'])
def register():
    data = request.json
    username = data.get('username')
    user_id = data.get('user_id')
    password = data.get('password')
    if not username or not user_id or not password:
        return jsonify({'error': 'Missing fields'}), 400
    conn = get_db()
    c = conn.cursor()
    c.execute('SELECT id FROM user WHERE user_id = ?', (user_id,))
    if c.fetchone():
        return jsonify({'error': 'User ID already exists'}), 400
    hashed = generate_password_hash(password)
    c.execute('INSERT INTO user (username, user_id, password) VALUES (?, ?, ?)', (username, user_id, hashed))
    user_db_id = c.lastrowid
    # Create default playlist for user
    default_playlist_name = f"{username} - playlist"
    c.execute('INSERT INTO playlist (name, user_id) VALUES (?, ?)', (default_playlist_name, user_db_id))
    conn.commit()
    conn.close()
    return jsonify({'message': 'Registered successfully'})

@app.route('/login', methods=['POST'])
def login():
    data = request.json
    user_id = data.get('user_id')
    password = data.get('password')
    if not user_id or not password:
        return jsonify({'error': 'Missing fields'}), 400
    conn = get_db()
    c = conn.cursor()
    c.execute('SELECT id, password FROM user WHERE user_id = ?', (user_id,))
    row = c.fetchone()
    if not row or not check_password_hash(row['password'], password):
        return jsonify({'error': 'Invalid credentials'}), 401
    token = create_jwt(user_id)
    return jsonify({'token': token})

@app.route('/me', methods=['GET'])
@login_required
def me():
    conn = get_db()
    c = conn.cursor()
    c.execute('SELECT id, username, user_id FROM user WHERE user_id = ?', (request.user_id,))
    row = c.fetchone()
    if not row:
        return jsonify({'error': 'User not found'}), 404
    return jsonify({'id': row['id'], 'username': row['username'], 'user_id': row['user_id']})

# --- Playlist Endpoints ---
@app.route('/playlists', methods=['POST'])
@login_required
def create_playlist():
    return jsonify({'error': 'Multiple playlists are not supported. Each user has a default playlist.'}), 400

@app.route('/playlists', methods=['GET'])
@login_required
def get_playlists():
    conn = get_db()
    c = conn.cursor()
    c.execute('SELECT id FROM user WHERE user_id = ?', (request.user_id,))
    user_row = c.fetchone()
    if not user_row:
        return jsonify({'error': 'User not found'}), 404
    user_db_id = user_row['id']
    # Only return the default playlist
    c.execute('SELECT id, name FROM playlist WHERE user_id = ? LIMIT 1', (user_db_id,))
    row = c.fetchone()
    playlists = [{'id': row['id'], 'name': row['name']}] if row else []
    conn.close()
    return jsonify({'playlists': playlists})

@app.route('/playlists/<int:playlist_id>/songs', methods=['POST'])
@login_required
def add_song_to_playlist(playlist_id):
    data = request.json
    song_id = data.get('song_id')
    song_title = data.get('song_title')
    if not song_id or not song_title:
        return jsonify({'error': 'Missing song_id or song_title'}), 400
    conn = get_db()
    c = conn.cursor()
    # Find user's default playlist
    c.execute('SELECT p.id FROM playlist p JOIN user u ON p.user_id = u.id WHERE u.user_id = ? LIMIT 1', (request.user_id,))
    row = c.fetchone()
    if not row:
        return jsonify({'error': 'Default playlist not found for user'}), 404
    playlist_id = row['id']
    c.execute('INSERT INTO playlistsong (playlist_id, song_id, song_title) VALUES (?, ?, ?)', (playlist_id, song_id, song_title))
    conn.commit()
    conn.close()
    return jsonify({'message': 'Song added'})

@app.route('/playlists/<int:playlist_id>/songs', methods=['GET'])
@login_required
def get_playlist_songs(playlist_id):
    conn = get_db()
    c = conn.cursor()
    # Find user's default playlist
    c.execute('SELECT p.id FROM playlist p JOIN user u ON p.user_id = u.id WHERE u.user_id = ? LIMIT 1', (request.user_id,))
    row = c.fetchone()
    if not row:
        return jsonify({'error': 'Default playlist not found for user'}), 404
    playlist_id = row['id']
    c.execute('SELECT id, song_id, song_title FROM playlistsong WHERE playlist_id = ?', (playlist_id,))
    songs = [{'id': row['id'], 'song_id': row['song_id'], 'song_title': row['song_title']} for row in c.fetchall()]
    conn.close()
    return jsonify({'songs': songs})

@app.route('/playlists/<int:playlist_id>/songs/<int:song_db_id>', methods=['DELETE'])
@login_required
def remove_song_from_playlist(playlist_id, song_db_id):
    conn = get_db()
    c = conn.cursor()
    # Find user's default playlist
    c.execute('SELECT p.id FROM playlist p JOIN user u ON p.user_id = u.id WHERE u.user_id = ? LIMIT 1', (request.user_id,))
    row = c.fetchone()
    if not row:
        return jsonify({'error': 'Default playlist not found for user'}), 404
    playlist_id = row['id']
    c.execute('DELETE FROM playlistsong WHERE id = ? AND playlist_id = ?', (song_db_id, playlist_id))
    conn.commit()
    conn.close()
    return jsonify({'message': 'Song removed'})

def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def get_static_songs():
    """Get list of static/local MP3 files with enhanced metadata"""
    songs = []
    songs_path = os.path.join(app.static_folder, 'songs')

    if not os.path.exists(songs_path):
        os.makedirs(songs_path)
        return songs

    print(f"Scanning for songs in: {songs_path}")
    
    for i, filename in enumerate(os.listdir(songs_path), start=1):
        if allowed_file(filename):
            file_path = os.path.join(songs_path, filename)
            title = None
            artist = None
            album = None
            duration = None
            year = None

            try:
                # Load MP3 and extract duration and metadata
                audio = MP3(file_path)
                duration = round(audio.info.length) if audio.info.length else None
                
                # Try to extract ID3 tags (title, artist, album, year)
                try:
                    tags = ID3(file_path)
                    title = tags.get("TIT2")
                    artist = tags.get("TPE1") 
                    album = tags.get("TALB")
                    year_tag = tags.get("TDRC") or tags.get("TYER")

                    def extract_text(tag):
                        if tag is None:
                            return None
                        val = tag.text[0] if hasattr(tag, 'text') and tag.text else tag
                        if isinstance(val, bytes):
                            try:
                                return val.decode('utf-8', errors='ignore')
                            except Exception:
                                return str(val)
                        return str(val)

                    title = extract_text(title)
                    artist = extract_text(artist)
                    album = extract_text(album)
                    year = extract_text(year_tag)
                    if year:
                        year = ''.join(filter(str.isdigit, year))[:4]
                        year = int(year) if year.isdigit() else None
                    else:
                        year = None
                except ID3NoHeaderError:
                    # File doesn't have ID3 tags, that's fine
                    pass

            except Exception as e:
                print(f"Warning: Could not read metadata from {filename}: {e}")

            # Fall back to filename parsing if tags are missing
            base_name = os.path.splitext(filename)[0]
            
            # Try to parse artist and title from filename patterns
            if not title or not artist:
                # Common patterns: "Artist - Title", "Artist_Title", etc.
                if ' - ' in base_name:
                    parts = base_name.split(' - ', 1)
                    if not artist:
                        artist = parts[0].strip()
                    if not title:
                        title = parts[1].strip()
                elif '_' in base_name and not title:
                    title = base_name.replace('_', ' ').replace('-', ' ').title()
                else:
                    title = base_name.replace('_', ' ').replace('-', ' ').title()

            # Final fallbacks
            pretty_title = title or base_name.replace('_', ' ').replace('-', ' ').title()
            pretty_artist = artist or "Unknown Artist"
            pretty_album = album or "Unknown Album"

            encoded_filename = urllib.parse.quote(filename)

            songs.append({
                "id": f"static-{i}",
                "title": pretty_title,
                "artist": pretty_artist,
                "album": pretty_album,
                "year": year,
                "duration": duration,
                "url": f"/songs/{encoded_filename}",
                "source": "static",
                "filename": filename,
                "thumbnail": None
            })

    print(f"Found {len(songs)} static songs")
    return songs

def upgrade_url(url):
    """Force any http:// URL to https:// for security (prevents mixed content)"""
    if isinstance(url, str) and url.startswith('http://'):
        return 'https://' + url[len('http://'):]
    return url

# --- JioSaavn API search ---
def search_jiosaavn(query, page=1, per_page=20):
    """Search for songs using the JioSaavn public API (unofficial)"""

    try:
        print(f"Searching JioSaavn for: '{query}' (page={page}, per_page={per_page})")
        params = {
            'query': query,
            'page': page
        }
        url = f"{JIOSAAVN_API_BASE}/search/songs"
        response = requests.get(url, params=params, timeout=10)
        print(f"JioSaavn API status: {response.status_code}")
        if response.status_code == 200:
            data = response.json()
            print('JioSaavn API raw response keys:', list(data.keys()))  # DEBUG: print response structure
            songs = []
            # JioSaavn API returns results in data['results']
            results = data.get('data', {}).get('results', [])

            for item in results[:per_page]:
                # Enhanced metadata extraction

                # Extract title with fallbacks
                title = item.get('name') or item.get('title') or 'Unknown Title'

                # Enhanced artist extraction with multiple fallbacks
                artist = None
                # ...existing code for artist extraction...
                if item.get('artists') and isinstance(item['artists'], dict) and 'primary' in item['artists']:
                    primary_artists = item['artists']['primary']
                    if isinstance(primary_artists, list) and primary_artists:
                        artist_names = [a['name'] for a in primary_artists if isinstance(a, dict) and a.get('name')]
                        if artist_names:
                            artist = ', '.join(artist_names)
                if not artist and item.get('primaryArtists'):
                    if isinstance(item['primaryArtists'], str):
                        artist = item['primaryArtists']
                if not artist and item.get('artists'):
                    if isinstance(item['artists'], str):
                        artist = item['artists']
                if not artist:
                    artist = item.get('artist')
                if not artist and item.get('artistMap') and item['artistMap'].get('primary_artists'):
                    pa = item['artistMap']['primary_artists']
                    if isinstance(pa, list) and pa and isinstance(pa[0], dict):
                        artist = pa[0].get('name')
                if isinstance(artist, str):
                    artist = artist.strip()
                    if not artist or artist.lower() in ['unknown', 'unknown artist', '']:
                        artist = 'Unknown Artist'

                # Enhanced album extraction
                album = None
                if item.get('album'):
                    if isinstance(item['album'], dict):
                        album = item['album'].get('name') or item['album'].get('title')
                    elif isinstance(item['album'], str):
                        album = item['album']
                if not album:
                    album = item.get('albumMap', {}).get('name') or item.get('albumName')

                # Enhanced thumbnail extraction with multiple sizes
                thumbnail = None
                if item.get('image'):
                    if isinstance(item['image'], list) and item['image']:
                        thumbnail = item['image'][-1]
                        if isinstance(thumbnail, dict):
                            thumbnail = thumbnail.get('link') or thumbnail.get('url')
                    elif isinstance(item['image'], str):
                        thumbnail = item['image']
                if not thumbnail:
                    for img_field in ['imageUrl', 'image_url', 'artwork', 'cover']:
                        if item.get(img_field):
                            thumbnail = item[img_field]
                            break
                if thumbnail and isinstance(thumbnail, str):
                    if '150x150' in thumbnail:
                        thumbnail = thumbnail.replace('150x150', '500x500')
                    elif '50x50' in thumbnail:
                        thumbnail = thumbnail.replace('50x50', '500x500')
                    thumbnail = upgrade_url(thumbnail)

                # Enhanced year extraction
                year = None
                year_fields = ['year', 'releaseYear', 'release_year', 'albumYear']
                for field in year_fields:
                    if item.get(field):
                        try:
                            year_val = str(item[field])
                            if year_val.isdigit() and len(year_val) == 4:
                                year = int(year_val)
                                break
                        except:
                            continue

                # Enhanced duration extraction (convert to seconds if needed)
                duration = None
                if item.get('duration'):
                    try:
                        duration_val = item['duration']
                        if isinstance(duration_val, str):
                            if ':' in duration_val:
                                parts = duration_val.split(':')
                                if len(parts) == 2:
                                    minutes, seconds = int(parts[0]), int(parts[1])
                                    duration = minutes * 60 + seconds
                            else:
                                duration = int(duration_val)
                        else:
                            duration = int(duration_val)
                    except:
                        pass

                # Use the best available audio URL or fallback to JioSaavn web link
                audio_url = None
                if 'downloadUrl' in item and item['downloadUrl']:
                    download_urls = item['downloadUrl']
                    if isinstance(download_urls, list):
                        for quality in ['320kbps', '160kbps', '96kbps', '48kbps']:
                            for d in download_urls:
                                if isinstance(d, dict) and d.get('quality') == quality and d.get('url'):
                                    audio_url = d['url']
                                    break
                            if audio_url:
                                break
                        if not audio_url:
                            for d in download_urls:
                                if isinstance(d, dict) and d.get('url'):
                                    audio_url = d['url']
                                    break
                if not audio_url:
                    url_fields = ['permaUrl', 'url', 'playUrl', 'streamUrl']
                    for field in url_fields:
                        if item.get(field):
                            audio_url = item[field]
                            break
                if audio_url:
                    audio_url = upgrade_url(audio_url)

                # Only add if we have a valid url and basic metadata
                if audio_url and title:
                    song_data = {
                        'id': item.get('id') or f"jiosaavn-{len(songs)}",
                        'title': title,
                        'artist': artist,
                        'album': album,
                        'year': year,
                        'duration': duration,
                        'url': audio_url,
                        'source': 'jiosaavn',
                        'thumbnail': thumbnail
                    }
                    songs.append(song_data)
            print(f"Returning {len(songs)} JioSaavn songs")
            return songs, len(results)
        else:
            print(f"JioSaavn API error: {response.status_code} - {response.text[:200]}")
    except Exception as e:
        print(f"Error searching JioSaavn: {e}")
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
                "url": "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                "source": "api",
                "album": "Demo Album",
                "year": 2023,
                "thumbnail": None
            },
            {
                "id": "demo-popular-2", 
                "title": "Demo Song 2 (Sample Audio)",
                "artist": "Demo Artist 2",
                "url": "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                "source": "api",
                "album": "Demo Album",
                "year": 2023,
                "thumbnail": None
            }
        ]
        
        print(f"Providing {len(demo_popular_songs)} demo popular songs")
        return demo_popular_songs
        
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
        # Shuffle the song list for randomness on every request
        random.shuffle(all_songs)

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
    """Search for songs using static files and JioSaavn API"""
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
        # Search JioSaavn API
        jiosaavn_songs, total_found = search_jiosaavn(query, page, per_page)
        print(f"Found {len(jiosaavn_songs)} JioSaavn songs")
        # Combine results (static songs first)
        # Ensure all external URLs are HTTPS in the response
        def secure_song(song):
            if song.get('source') == 'jiosaavn':
                song = song.copy()
                song['url'] = upgrade_url(song.get('url'))
                if song.get('thumbnail'):
                    song['thumbnail'] = upgrade_url(song.get('thumbnail'))
            return song

        all_results = [secure_song(song) for song in matching_static + jiosaavn_songs]
        response_data = {
            'songs': all_results,
            'total': len(matching_static) + total_found,
            'page': page,
            'per_page': per_page,
            'query': query,
            'static_matches': len(matching_static),
            'api_matches': len(jiosaavn_songs)
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
            return jsonify(static_songs[random.randrange(len(static_songs))])
        else:
            # Fallback to popular songs
            popular_songs = get_popular_songs(5)
            if popular_songs:
                song = popular_songs[random.randrange(len(popular_songs))]
                song['url'] = upgrade_url(song.get('url'))
                if song.get('thumbnail'):
                    song['thumbnail'] = upgrade_url(song.get('thumbnail'))
                return jsonify(song)
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
    """Proxy audio files from external sources to bypass CORS"""
    try:
        # Decode the URL
        decoded_url = urllib.parse.unquote(audio_url)
        
        print(f"Proxying audio from: {decoded_url}")
        
        # Add headers to mimic a real browser request
        headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
            'Accept': 'audio/webm,audio/ogg,audio/wav,audio/*;q=0.9,application/ogg;q=0.7,video/*;q=0.6,*/*;q=0.5',
            'Accept-Language': 'en-US,en;q=0.5',
            'Accept-Encoding': 'identity',
            'Range': request.headers.get('Range', '')
        }
        
        # Remove empty range header
        if not headers['Range']:
            del headers['Range']
        
        # Stream the audio file
        response = requests.get(decoded_url, stream=True, timeout=30, headers=headers)
        
        print(f"Remote response status: {response.status_code}")
        print(f"Remote response headers: {dict(response.headers)}")
        
        if response.status_code in [200, 206]:
            # Forward the audio stream with proper headers
            def generate():
                for chunk in response.iter_content(chunk_size=8192):
                    if chunk:
                        yield chunk
            
            # Get content type from original response
            content_type = response.headers.get('content-type', 'audio/mpeg')
            
            proxy_headers = {
                'Accept-Ranges': 'bytes',
                'Cache-Control': 'public, max-age=3600',
                'Access-Control-Allow-Origin': '*',
                'Access-Control-Allow-Methods': 'GET',
                'Access-Control-Allow-Headers': 'Range',
            }
            if response.headers.get('Content-Length'):
                proxy_headers['Content-Length'] = response.headers['Content-Length']
            if response.headers.get('Content-Range'):
                proxy_headers['Content-Range'] = response.headers['Content-Range']

            flask_response = app.response_class(
                generate(),
                status=response.status_code,
                mimetype=content_type,
                headers=proxy_headers
            )
            return flask_response
        else:
            print(f"Failed to fetch audio: {response.status_code}")
            print(f"Response text: {response.text[:200]}")
            return jsonify({'error': f'Failed to fetch audio: {response.status_code}'}), response.status_code
            
    except Exception as e:
        print(f"Error proxying audio: {e}")
        import traceback
        traceback.print_exc()
        return jsonify({'error': 'Failed to proxy audio'}), 500

# Serve React static files
# @app.route('/', defaults={'path': ''})
# @app.route('/<path:path>')
# def serve_react(path):
#     build_dir = os.path.join(os.path.dirname(__file__), 'frontend', 'build')
#     if path != "" and os.path.exists(os.path.join(build_dir, path)):
#         return send_from_directory(build_dir, path)
#     else:
#         return send_from_directory(build_dir, 'index.html')
@app.route('/', defaults={'path': ''})
@app.route('/<path:path>')
def serve_react(path):
    if path != "" and os.path.exists(os.path.join('static', path)):
        return send_from_directory('static', path)
    return render_template('index.html')

@app.route('/static/<path:filename>')
def serve_react_static(filename):
    build_static_dir = os.path.join(os.path.dirname(__file__), 'frontend', 'build', 'static')
    return send_from_directory(build_static_dir, filename)

# Health check endpoint
@app.route('/api/health')
def health_check():
    return jsonify({'status': 'healthy', 'service': 'music_player_api'})

# Debug endpoint to test API directly (now uses JioSaavn)
@app.route('/api/debug/search/<query>')
def debug_search(query):
    """Debug endpoint to test search directly (JioSaavn)"""
    try:
        songs, total = search_jiosaavn(query, 1, 5)
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
        
        return jsonify({'error': 'This endpoint is deprecated. Internet Archive API is no longer supported.'}), 410
        
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
        return jsonify({'error': 'This endpoint is deprecated. Internet Archive API is no longer supported.'}), 410
        
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
                    continue
            
            return jsonify({
                'identifier': identifier,
                'total_files': len(files),
                'files_shown': len(file_info),
                'files': file_info,
                'audio_files_found': audio_files,
                'metadata_url': None
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
    test_url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
    proxy_url = f"/proxy/audio/{urllib.parse.quote(test_url, safe='')}"
    
    return jsonify({
        'test_direct_url': test_url,
        'test_proxy_url': proxy_url,
        'instructions': 'Try accessing the proxy_url in your browser or audio player'
    })

# Simple test endpoint
@app.route('/api/test/simple')
def test_simple():
    """Simple test to verify Flask is working"""
    return jsonify({
        'status': 'Flask is working!',
        'message': 'If you see this, the backend is running properly'
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
        
        return jsonify({'error': 'This endpoint is deprecated. Internet Archive API is no longer supported.'}), 410
        
        if response.status_code == 200:
            data = response.json()
            response_data = data.get('response', {})
            docs = response_data.get('docs', [])
            total_found = response_data.get('numFound', 0)
            
            # Test audio detection for each
            results = []
            for doc in docs:
                identifier = doc.get('identifier')
                audio_url = None
                
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

# Comprehensive test endpoint
@app.route('/api/test/all')
def test_all():
    """Comprehensive test of all functionality"""
    results = {}
    
    try:
        # Test 1: Static songs
        static_songs = get_static_songs()
        results['static_songs'] = {
            'status': 'success',
            'count': len(static_songs),
            'sample_songs': static_songs[:3] if static_songs else []
        }
    except Exception as e:
        results['static_songs'] = {'status': 'error', 'error': str(e)}
    
    try:
        # Test 2: Demo/Popular songs  
        popular_songs = get_popular_songs(3)
        results['popular_songs'] = {
            'status': 'success',
            'count': len(popular_songs),
            'sample_songs': popular_songs
        }
    except Exception as e:
        results['popular_songs'] = {'status': 'error', 'error': str(e)}
    
    try:
        # Test 3: Search functionality
        results['search'] = {
            'status': 'skipped',
            'reason': 'Internet Archive API is no longer supported.'
        }
    except Exception as e:
        results['search'] = {'status': 'error', 'error': str(e)}
    
    # Test 4: Basic app info
    results['app_info'] = {
        'status': 'success',
        'static_folder': app.static_folder,
        'songs_folder_exists': os.path.exists(os.path.join(app.static_folder, 'songs')),
        'available_routes': [
            '/api/songs',
            '/api/search',
            '/api/random',
            '/api/health',
            '/proxy/audio/<url>',
            '/songs/<filename>'
        ]
    }
    
    return jsonify({
        'overall_status': 'Backend is working!',
        'timestamp': None,
        'test_results': results
    })

# Enhanced API endpoints for production features

@app.route('/api/songs/shuffle')
def api_songs_shuffle():
    """Get a shuffled list of all songs"""
    try:
        static_songs = get_static_songs()
        popular_songs = get_popular_songs(10)
        all_songs = static_songs + popular_songs
        
        import random
        shuffled = all_songs.copy()
        random.shuffle(shuffled)
        
        return jsonify({
            'songs': shuffled,
            'total': len(shuffled),
            'shuffled': True
        })
    except Exception as e:
        print(f"Error in shuffle: {e}")
        return jsonify({'error': 'Failed to shuffle songs'}), 500

@app.route('/api/songs/<song_id>/info')
def api_song_info(song_id):
    """Get detailed information about a specific song"""
    try:
        static_songs = get_static_songs()
        popular_songs = get_popular_songs(20)
        all_songs = static_songs + popular_songs

        song = next((s for s in all_songs if s['id'] == song_id), None)

        if not song:
            return jsonify({'error': 'Song not found'}), 404

        # Add extra metadata if it's a static song
        if song['source'] == 'static':
            file_path = os.path.join(app.static_folder, 'songs', song.get('filename', ''))
            if os.path.exists(file_path):
                try:
                    audio = MP3(file_path)
                    song['bitrate'] = audio.info.bitrate if hasattr(audio.info, 'bitrate') else None
                    song['sample_rate'] = audio.info.sample_rate if hasattr(audio.info, 'sample_rate') else None
                    song['file_size'] = os.path.getsize(file_path)
                except:
                    pass
        # Ensure HTTPS for external URLs
        if song.get('source') == 'jiosaavn':
            song['url'] = upgrade_url(song.get('url'))
            if song.get('thumbnail'):
                song['thumbnail'] = upgrade_url(song.get('thumbnail'))

        return jsonify(song)
    except Exception as e:
        print(f"Error getting song info: {e}")
        return jsonify({'error': 'Failed to get song info'}), 500

@app.route('/api/songs/by-artist/<artist_name>')
def api_songs_by_artist(artist_name):
    """Get all songs by a specific artist"""
    try:
        static_songs = get_static_songs()
        popular_songs = get_popular_songs(20)
        all_songs = static_songs + popular_songs
        
        artist_songs = [
            song for song in all_songs 
            if artist_name.lower() in song['artist'].lower()
        ]
        
        return jsonify({
            'artist': artist_name,
            'songs': artist_songs,
            'total': len(artist_songs)
        })
    except Exception as e:
        print(f"Error getting songs by artist: {e}")
        return jsonify({'error': 'Failed to get songs by artist'}), 500

@app.route('/api/artists')
def api_artists():
    """Get list of all artists"""
    try:
        static_songs = get_static_songs()
        popular_songs = get_popular_songs(20)
        all_songs = static_songs + popular_songs
        
        artists = {}
        for song in all_songs:
            artist = song['artist']
            if artist not in artists:
                artists[artist] = {
                    'name': artist,
                    'song_count': 0,
                    'albums': set()
                }
            artists[artist]['song_count'] += 1
            if song.get('album'):
                artists[artist]['albums'].add(song['album'])
        
        # Convert sets to lists for JSON serialization
        for artist in artists.values():
            artist['albums'] = list(artist['albums'])
            artist['album_count'] = len(artist['albums'])
        
        return jsonify({
            'artists': list(artists.values()),
            'total': len(artists)
        })
    except Exception as e:
        print(f"Error getting artists: {e}")
        return jsonify({'error': 'Failed to get artists'}), 500

@app.route('/api/albums')
def api_albums():
    """Get list of all albums"""
    try:
        static_songs = get_static_songs()
        popular_songs = get_popular_songs(20)
        all_songs = static_songs + popular_songs
        
        albums = {}
        for song in all_songs:
            album = song.get('album') or 'Unknown Album'
            if album not in albums:
                albums[album] = {
                    'name': album,
                    'artist': song['artist'],
                    'year': song.get('year'),
                    'songs': [],
                    'duration': 0
                }
            albums[album]['songs'].append(song)
            if song.get('duration'):
                albums[album]['duration'] += song['duration']
        
        # Convert to list and add metadata
        album_list = []
        for album_name, album_data in albums.items():
            album_data['song_count'] = len(album_data['songs'])
            album_data['id'] = f"album-{len(album_list)}"
            album_list.append(album_data)
        
        return jsonify({
            'albums': album_list,
            'total': len(album_list)
        })
    except Exception as e:
        print(f"Error getting albums: {e}")
        return jsonify({'error': 'Failed to get albums'}), 500

@app.route('/api/stats')
def api_stats():
    """Get music library statistics"""
    try:
        static_songs = get_static_songs()
        popular_songs = get_popular_songs(20)
        
        total_duration = sum(song.get('duration', 0) for song in static_songs if song.get('duration'))
        
        artists = set(song['artist'] for song in static_songs)
        albums = set(song.get('album') for song in static_songs if song.get('album'))
        
        years = [song.get('year') for song in static_songs if song.get('year')]
        year_range = f"{min(years)}-{max(years)}" if years else "Unknown"
        
        return jsonify({
            'total_songs': len(static_songs),
            'demo_songs': len(popular_songs),
            'total_artists': len(artists),
            'total_albums': len(albums),
            'total_duration': total_duration,
            'total_duration_formatted': f"{total_duration // 3600}h {(total_duration % 3600) // 60}m",
            'year_range': year_range,
            'formats_supported': list(ALLOWED_EXTENSIONS),
            'library_size_mb': sum(
                os.path.getsize(os.path.join(app.static_folder, 'songs', song['filename'])) 
                for song in static_songs 
                if song.get('filename') and os.path.exists(os.path.join(app.static_folder, 'songs', song['filename']))
            ) / (1024 * 1024)
        })
    except Exception as e:
        print(f"Error getting stats: {e}")
        return jsonify({'error': 'Failed to get stats'}), 500


# --- Production Note ---
# For production, run this app with a WSGI server such as gunicorn:
#   gunicorn -w 4 -b 0.0.0.0:5600 app:app
# Do NOT use Flask's built-in server in production.