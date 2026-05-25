import zipfile
import io
import urllib.request
import re

url = "https://maven.lavalink.dev/releases/dev/lavalink/youtube/youtube-plugin/1.18.1/youtube-plugin-1.18.1.jar"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
with urllib.request.urlopen(req) as response:
    jar_data = response.read()

with zipfile.ZipFile(io.BytesIO(jar_data)) as z:
    class_bytes = z.read("dev/lavalink/youtube/YoutubeSourceOptions.class")
    
    strings = re.findall(rb'[\x20-\x7E]{2,}', class_bytes)
    unique_strings = sorted(list(set(s.decode('ascii', errors='ignore') for s in strings)))
    print("All string constants in YoutubeSourceOptions:")
    for s in unique_strings:
        print("  ", s)
