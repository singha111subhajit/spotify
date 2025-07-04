from flask import Flask, render_template, jsonify, send_from_directory
import os
import urllib.parse
from mutagen.mp3 import MP3
from mutagen.id3 import ID3, TIT2, TPE1


app = Flask(__name__)

# Configuration
SONGS_FOLDER = 'static/songs'
ALLOWED_EXTENSIONS = {'mp3', 'wav', 'ogg'}

def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS



def get_song_list():
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
                minutes = int(duration // 60)
                seconds = int(duration % 60)
                duration_str = f"{minutes}:{seconds:02}"
                
                # Try to extract ID3 tags (title, artist)
                tags = ID3(file_path)
                title = tags.get("TIT2")
                artist = tags.get("TPE1")
                title = title.text[0] if title else None
                artist = artist.text[0] if artist else None

            except Exception as e:
                print(f"Warning: Could not read tags from {filename}: {e}")
                duration_str = "Unknown"

            # Fall back to filename if tags are missing
            base_name = os.path.splitext(filename)[0]
            pretty_title = title or base_name.replace('_', ' ').replace('-', ' ').title()
            pretty_artist = artist or "Unknown Artist"

            encoded_filename = urllib.parse.quote(filename)

            songs.append({
                "id": i,
                "title": pretty_title,
                "artist": pretty_artist,
                "filename": encoded_filename,
                "duration": duration_str
            })

    return songs



@app.route('/')
def index():
    """Serve the main page"""
    return render_template('index.html')

@app.route('/api/songs')
def api_songs():
    """API endpoint to get list of songs"""
    songs = get_song_list()
    return jsonify(songs)

@app.route('/songs/<filename>')
def serve_song(filename):
    """Serve audio files"""
    return send_from_directory(os.path.join(app.static_folder, 'songs'), filename)

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5600)