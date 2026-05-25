import urllib.request
import zipfile
import io

url = "https://maven.lavalink.dev/releases/dev/lavalink/youtube/youtube-plugin/1.18.1/youtube-plugin-1.18.1.jar"
print(f"Downloading {url} ...")
try:
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req) as response:
        jar_data = response.read()
    print("Downloaded successfully. Reading entries...")
    with zipfile.ZipFile(io.BytesIO(jar_data)) as z:
        for name in z.namelist():
            if "YoutubeAudioSourceManager" in name:
                print(name)
except Exception as e:
    print("Error:", e)
