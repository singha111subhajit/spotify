from flask_sqlalchemy import SQLAlchemy

db = SQLAlchemy()

class User(db.Model):
    __tablename__ = 'app_user'
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String, nullable=False)
    user_id = db.Column(db.String, unique=True, nullable=False)
    password = db.Column(db.String, nullable=False)
    playlists = db.relationship('Playlist', backref='user', lazy=True)

class Playlist(db.Model):
    __tablename__ = 'playlist'
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String, nullable=False)
    user_id = db.Column(db.Integer, db.ForeignKey('app_user.id'), nullable=False)
    songs = db.relationship('PlaylistSong', backref='playlist', lazy=True)

class PlaylistSong(db.Model):
    __tablename__ = 'playlistsong'
    id = db.Column(db.Integer, primary_key=True)
    playlist_id = db.Column(db.Integer, db.ForeignKey('playlist.id'), nullable=False)
    song_id = db.Column(db.String, nullable=False)
    song_title = db.Column(db.String, nullable=False)
