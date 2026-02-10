import nbtlib
import gzip
import os

base = r"C:\Users\toxic\Projects\MoodStudios\Github\aromacraft\common\src\main\resources\data\aromaaffect\structure\village"
biomes = ['plains', 'desert', 'savanna', 'snowy', 'taiga']

for biome in biomes:
    for kind, subpath in [("START", f"{biome}/town_centers/nose_smith_start.nbt"),
                          ("HOUSE", f"{biome}/houses/nose_smith_house.nbt")]:
        path = os.path.join(base, subpath)
        print(f"\n=== {kind}: {biome} ===")
        try:
            f = nbtlib.load(path)
            root = f

            # Get size
            if 'size' in root:
                size = [int(x) for x in root['size']]
                print(f"  Structure size: {size}")

            # Get palette to find jigsaw index
            palette = root.get('palette', [])
            jigsaw_indices = set()
            for i, entry in enumerate(palette):
                name = str(entry.get('Name', ''))
                if 'jigsaw' in name:
                    jigsaw_indices.add(i)
                    props = entry.get('Properties', {})
                    orient = str(props.get('orientation', '?'))
                    print(f"  Palette[{i}]: jigsaw orientation={orient}")

            # Find all jigsaw blocks
            blocks = root.get('blocks', [])
            for block in blocks:
                state = int(block['state'])
                if state in jigsaw_indices:
                    pos = [int(x) for x in block['pos']]
                    nbt_data = block.get('nbt', {})
                    pool = str(nbt_data.get('pool', '?'))
                    name = str(nbt_data.get('name', '?'))
                    target = str(nbt_data.get('target', '?'))
                    final_state = str(nbt_data.get('final_state', '?'))
                    joint = str(nbt_data.get('joint', '?'))
                    print(f"  JIGSAW pos={pos} pool={pool} name={name} target={target} final={final_state} joint={joint}")
        except Exception as e:
            print(f"  ERROR: {e}")
