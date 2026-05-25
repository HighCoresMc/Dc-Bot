import urllib.request
import subprocess
import os

url = "https://maven.lavalink.dev/releases/dev/lavalink/youtube/youtube-plugin/1.18.1/youtube-plugin-1.18.1.jar"
jar_path = "youtube-plugin-1.18.1.jar"

print(f"Downloading {url} ...")
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
with urllib.request.urlopen(req) as response:
    with open(jar_path, "wb") as f:
        f.write(response.read())

print("Downloaded. Running javap on YoutubeAudioSourceManager...")
try:
    res = subprocess.run(["javap", "-p", "-cp", jar_path, "dev.lavalink.youtube.YoutubeAudioSourceManager"], capture_output=True, text=True)
    print("STDOUT:")
    print(res.stdout)
    print("STDERR:")
    print(res.stderr)
except Exception as e:
    print("Error:", e)

# Clean up
if os.path.exists(jar_path):
    os.remove(jar_path)
