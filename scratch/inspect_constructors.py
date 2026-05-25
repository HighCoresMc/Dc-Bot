import zipfile
import io
import urllib.request

url = "https://maven.lavalink.dev/releases/dev/lavalink/youtube/youtube-plugin/1.18.1/youtube-plugin-1.18.1.jar"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
with urllib.request.urlopen(req) as response:
    jar_data = response.read()

with zipfile.ZipFile(io.BytesIO(jar_data)) as z:
    class_bytes = z.read("dev/lavalink/youtube/YoutubeAudioSourceManager.class")
    
    # Let's search for method descriptors of constructors
    # A constructor descriptor starts with ( and has <init> somewhere.
    # We can just print all strings that look like signatures
    import re
    strings = re.findall(rb'[\x20-\x7E]{2,}', class_bytes)
    unique_strings = sorted(list(set(s.decode('ascii', errors='ignore') for s in strings)))
    print("All string constants in YoutubeAudioSourceManager:")
    for s in unique_strings:
        if "(" in s or "init" in s or "dev/lavalink" in s or "Options" in s:
            print("  ", s)
