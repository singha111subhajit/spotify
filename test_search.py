
import jiosaavn

print("--- Testing JioSaavn Search ---")
songs, total = jiosaavn.search_songs("love", page=1, limit=5)

if songs:
    print(f"Successfully found {len(songs)} songs (total results: {total}).")
    for i, song in enumerate(songs):
        print(f"  {i+1}. Title: {song.get('title')}")
        print(f"     Artist: {song.get('artist')}")
        print(f"     URL: {song.get('url')}")
else:
    print("--- Test Failed: No songs were found. ---")

print("--- Test Complete ---")
