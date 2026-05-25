import zipfile
import io
import urllib.request

url = "https://maven.lavalink.dev/releases/dev/lavalink/youtube/youtube-plugin/1.18.1/youtube-plugin-1.18.1.jar"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
with urllib.request.urlopen(req) as response:
    jar_data = response.read()

# Simple class parser to extract method names and descriptors
def parse_class_methods(class_bytes):
    # A JVM class file parser
    # Reference: https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html
    stream = io.BytesIO(class_bytes)
    magic = stream.read(4)
    if magic != b'\xca\xfe\xba\xbe':
        raise ValueError("Invalid class file")
    minor = int.from_bytes(stream.read(2), 'big')
    major = int.from_bytes(stream.read(2), 'big')
    constant_pool_count = int.from_bytes(stream.read(2), 'big')
    
    cp = [None] * constant_pool_count
    i = 1
    while i < constant_pool_count:
        tag = stream.read(1)[0]
        if tag == 1: # UTF8
            length = int.from_bytes(stream.read(2), 'big')
            cp[i] = stream.read(length).decode('utf-8', errors='ignore')
        elif tag == 7: # Class
            name_index = int.from_bytes(stream.read(2), 'big')
            cp[i] = ('Class', name_index)
        elif tag in (8, 16): # String, MethodType
            stream.read(2)
            cp[i] = ('Ref2', )
        elif tag in (15,): # MethodHandle
            stream.read(3)
            cp[i] = ('Ref3', )
        elif tag in (3, 4, 9, 10, 11, 12, 18): # Integer, Float, Fieldref, Methodref, InterfaceMethodref, NameAndType, InvokeDynamic
            stream.read(4)
            cp[i] = ('Ref4', )
        elif tag in (5, 6): # Long, Double
            stream.read(8)
            cp[i] = ('Ref8', )
            i += 1 # Long/Double takes two slots
        else:
            raise ValueError(f"Unknown constant pool tag: {tag}")
        i += 1
        
    access_flags = stream.read(2)
    this_class = stream.read(2)
    super_class = stream.read(2)
    interfaces_count = int.from_bytes(stream.read(2), 'big')
    stream.read(2 * interfaces_count)
    
    fields_count = int.from_bytes(stream.read(2), 'big')
    for _ in range(fields_count):
        stream.read(6) # access_flags, name_index, descriptor_index
        attributes_count = int.from_bytes(stream.read(2), 'big')
        for _ in range(attributes_count):
            stream.read(2) # name_index
            length = int.from_bytes(stream.read(4), 'big')
            stream.read(length)
            
    methods_count = int.from_bytes(stream.read(2), 'big')
    methods = []
    for _ in range(methods_count):
        m_access_flags = int.from_bytes(stream.read(2), 'big')
        m_name_index = int.from_bytes(stream.read(2), 'big')
        m_descriptor_index = int.from_bytes(stream.read(2), 'big')
        m_name = cp[m_name_index]
        m_desc = cp[m_descriptor_index]
        methods.append((m_name, m_desc))
        
        attributes_count = int.from_bytes(stream.read(2), 'big')
        for _ in range(attributes_count):
            stream.read(2) # name_index
            length = int.from_bytes(stream.read(4), 'big')
            stream.read(length)
            
    return methods

with zipfile.ZipFile(io.BytesIO(jar_data)) as z:
    for cls in ["dev/lavalink/youtube/YoutubeSourceOptions.class", "dev/lavalink/youtube/YoutubeAudioSourceManager.class"]:
        print(f"\nMethods for {cls}:")
        class_bytes = z.read(cls)
        for name, desc in parse_class_methods(class_bytes):
            if not name.startswith("lambda$") and not name.startswith("access$"):
                print(f"  {name} {desc}")
