from flask import Flask, render_template, jsonify, send_from_directory
import os
import json

app = Flask(__name__)

# Configuration
SONGS_FOLDER = 'static/songs'
ALLOWED_EXTENSIONS = {'mp3', 'wav', 'ogg'}

def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def get_song_list():
    """Get list of songs from the songs folder"""
    songs = []
    songs_path = os.path.join(app.static_folder, 'songs')
    
    # Create songs directory if it doesn't exist
    if not os.path.exists(songs_path):
        os.makedirs(songs_path)
        return songs
    
    # Sample song data (in real app, you'd extract metadata from MP3 files)
    sample_songs = [
        {
            "id": 1,
            "title": "Summer Vibes",
            "artist": "Chill Beats",
            "filename": "summer_vibes.mp3",
            "duration": "3:24"
        },
        {
            "id": 2,
            "title": "Midnight Drive",
            "artist": "Synthwave Collective",
            "filename": "midnight_drive.mp3",
            "duration": "4:15"
        },
        {
            "id": 3,
            "title": "Coffee Shop Jazz",
            "artist": "Jazz Ensemble",
            "filename": "coffee_shop_jazz.mp3",
            "duration": "5:02"
        },
        {
            "id": 4,
            "title": "Digital Dreams",
            "artist": "Electronic Paradise",
            "filename": "digital_dreams.mp3",
            "duration": "3:45"
        },
        {
            "id": 5,
            "title": "Acoustic Sunset",
            "artist": "Folk & Friends",
            "filename": "acoustic_sunset.mp3",
            "duration": "4:38"
        }
    ]
    
    return sample_songs

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
    app.run(debug=True, host='0.0.0.0', port=5000)