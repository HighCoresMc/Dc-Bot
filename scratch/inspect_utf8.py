import urllib.request
import zipfile
import io
import re

url = "https://maven.lavalink.dev/releases/dev/lavalink/youtube/youtube-plugin/1.18.1/youtube-plugin-1.18.1.jar"
print(f"Downloading {url} ...")
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
with urllib.request.urlopen(req) as response:
    jar_data = response.read()

print("Downloaded successfully. Finding Class files...")
with zipfile.ZipFile(io.BytesIO(jar_data)) as z:
    for name in z.namelist():
        if name.endswith(".class") and ("youtube" in name or "cipher" in name):
            class_data = z.read(name)
            # Find all ASCII/UTF-8 strings in the class file
            strings = re.findall(rb'[\x20-\x7E]{4,}', class_data)
            unique_strings = sorted(list(set(s.decode('ascii', errors='ignore') for s in strings)))
            print(f"\n--- {name} ---")
            for s in unique_strings:
                if any(x in s.lower() for x in ["cipher", "remote", "pot", "token", "visitor", "oauth", "client"]):
                    print("  ", s)
