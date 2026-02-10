import sys
path=sys.argv[1]
with open(path,"w") as f:
    import base64,zlib
    f.write(zlib.decompress(base64.b64decode(sys.argv[2])).decode())
print("done")
