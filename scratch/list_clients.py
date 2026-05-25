import zipfile
import io
import urllib.request

url = "https://maven.lavalink.dev/releases/dev/lavalink/youtube/youtube-plugin/1.18.1/youtube-plugin-1.18.1.jar"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
with urllib.request.urlopen(req) as response:
    jar_data = response.read()

with zipfile.ZipFile(io.BytesIO(jar_data)) as z:
    for name in z.namelist():
        if name.startswith("dev/lavalink/youtube/clients/") and name.endswith(".class") and "$" not in name:
            print(name)
