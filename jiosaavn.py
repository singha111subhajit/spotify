"""
Unofficial JioSaavn API for Python
Adapted from cyberboysumanjay/JioSaavnAPI
"""
from jiosaavnpy import JioSaavn

def _format_song(song_json):
    """Formats a single song's JSON data from jiosaavnpy into a cleaner dictionary."""
    if not song_json:
        return None

    try:
        return {
            'id': song_json.get('track_id'),
            'title': song_json.get('title', '').strip(),
            'artist': song_json.get('primary_artists', '').strip(),
            'album': song_json.get('album_name', '').strip(),
            'year': int(song_json['release_year']) if song_json.get('release_year') else None,
            'duration': int(song_json['duration']) if song_json.get('duration') else None,
            'url': song_json.get('stream_urls', {}).get('very_high_quality'),
            'thumbnail': song_json.get('thumbnails', {}).get('quality', {}).get('500x500'),
            'source': 'jiosaavn',
        }
    except (ValueError, TypeError) as e:
        print(f"Could not format song: {song_json.get('track_id')}. Error: {e}")
        return None

def search_songs(query: str, page: int = 1, limit: int = 20):
    """
    Search for songs on JioSaavn using the jiosaavnpy library.

    :param query: Search query.
    :param page: Page number (ignored, as the library doesn't support pagination in the same way).
    :param limit: Number of results per page.
    :return: A list of song dictionaries and the total number of results.
    """
    try:
        jio = JioSaavn()
        results = jio.search_songs(query, limit=limit)
        
        if not results:
            return [], 0
            
        formatted_songs = [_format_song(song) for song in results]
        # Filter out songs that couldn't be formatted
        valid_songs = [song for song in formatted_songs if song and song.get('url')]

        # The library does not provide a total count, so we return the number of results found.
        total_results = len(valid_songs)

        return valid_songs, total_results

    except Exception as e:
        print(f"Error searching JioSaavn: {e}")
        return [], 0