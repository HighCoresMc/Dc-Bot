import os
import glob

print("JAVA_HOME:", os.environ.get("JAVA_HOME"))
print("PATH:", os.environ.get("PATH"))

java_home = os.environ.get("JAVA_HOME")
if java_home:
    javap_paths = glob.glob(os.path.join(java_home, "**/javap*"), recursive=True)
    print("Found javap in JAVA_HOME:", javap_paths)
else:
    # Try finding in common paths
    for path in ["C:\\Program Files\\Java", "C:\\Program Files (x86)\\Java"]:
        if os.path.exists(path):
            javap_paths = glob.glob(os.path.join(path, "**/javap*"), recursive=True)
            print(f"Found javap in {path}:", javap_paths)
