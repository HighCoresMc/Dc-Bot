import re
import sys

# Configure stdout for utf-8
sys.stdout.reconfigure(encoding='utf-8')

path = r"C:\Users\omars\.gemini\antigravity-ide\brain\212d20d7-9255-4a85-be6c-33a097fccac5\.system_generated\steps\1095\content.md"
with open(path, "r", encoding="utf-8") as f:
    text = f.read()

# Search for comments or text that might contain a version or a solution/walkaround
for match in re.finditer(r"(?:1\.18|1\.19|snapshot|compiled|build|fixed|release|working|update|cipher|remote)", text[38000:], re.IGNORECASE):
    pos = 38000 + match.start()
    start = max(38000, pos - 150)
    end = min(len(text), pos + 150)
    snippet = text[start:end].replace("\n", " ")
    print(f"Match '{match.group()}' at {pos}: ...{snippet}...")
    print("-" * 50)
