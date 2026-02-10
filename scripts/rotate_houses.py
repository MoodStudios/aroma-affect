"""
Rotate all nose_smith_house.nbt structures 180 degrees around the Y axis.

This fixes the visual orientation so the house entrance/path faces the
village center instead of away from it.

After rotating all blocks, the connection jigsaw is swapped back to
[0,1,5] with west_up orientation so jigsaw mechanics are unchanged.
"""

import nbtlib
import os
import shutil
import copy

base = r"C:\Users\toxic\Projects\MoodStudios\Github\aromacraft\common\src\main\resources\data\aromaaffect\structure\village"

DIRECTION_180 = {
    'north': 'south', 'south': 'north',
    'east': 'west', 'west': 'east',
    'up': 'up', 'down': 'down',
}

ORIENTATION_180 = {
    'north_up': 'south_up', 'south_up': 'north_up',
    'east_up': 'west_up', 'west_up': 'east_up',
    'up_north': 'up_south', 'up_south': 'up_north',
    'up_east': 'up_west', 'up_west': 'up_east',
    'down_north': 'down_south', 'down_south': 'down_north',
    'down_east': 'down_west', 'down_west': 'down_east',
    'north_down': 'south_down', 'south_down': 'north_down',
    'east_down': 'west_down', 'west_down': 'east_down',
}


def rotate_palette_180(palette):
    """Rotate all directional properties in palette entries for 180 Y rotation."""
    for entry in palette:
        props = entry.get('Properties')
        if not props:
            continue

        # facing (stairs, doors, chests, furnaces, etc.)
        if 'facing' in props:
            old = str(props['facing'])
            if old in DIRECTION_180:
                props['facing'] = nbtlib.String(DIRECTION_180[old])

        # orientation (jigsaw blocks)
        if 'orientation' in props:
            old = str(props['orientation'])
            if old in ORIENTATION_180:
                props['orientation'] = nbtlib.String(ORIENTATION_180[old])

        # rotation (standing signs, banners: 0-15)
        if 'rotation' in props:
            old = int(str(props['rotation']))
            props['rotation'] = nbtlib.String(str((old + 8) % 16))

        # Connection properties (fences, walls, glass panes, iron bars)
        # Swap north<->south values and east<->west values
        if 'north' in props and 'south' in props:
            n, s = str(props['north']), str(props['south'])
            props['north'] = nbtlib.String(s)
            props['south'] = nbtlib.String(n)
        if 'east' in props and 'west' in props:
            e, w = str(props['east']), str(props['west'])
            props['east'] = nbtlib.String(w)
            props['west'] = nbtlib.String(e)

        # axis, half, shape, hinge, type, open, powered, etc. are
        # unchanged under 180-degree Y rotation.


def rotate_house(biome):
    path = os.path.join(base, biome, "houses", "nose_smith_house.nbt")
    backup = path + ".bak"
    print(f"\n=== Rotating {biome} house ===")
    print(f"  File: {path}")

    if not os.path.exists(path):
        print("  ERROR: File not found!")
        return

    shutil.copy2(path, backup)
    print(f"  Backup: {backup}")

    f = nbtlib.load(path)
    root = f

    sx, sy, sz = [int(x) for x in root['size']]
    print(f"  Size: [{sx}, {sy}, {sz}]")

    # --- 1. Rotate palette (directional block states) ---
    palette = root['palette']

    # Before rotating, find the palette index for the connection jigsaw (west_up)
    # so we can restore it afterwards.
    conn_palette_idx = None
    for i, entry in enumerate(palette):
        if str(entry.get('Name', '')) == 'minecraft:jigsaw':
            props = entry.get('Properties', {})
            if str(props.get('orientation', '')) == 'west_up':
                conn_palette_idx = i
                break

    rotate_palette_180(palette)
    # After rotation, the west_up entry is now east_up.

    # --- 2. Rotate block positions ---
    blocks = root['blocks']
    for block in blocks:
        pos = block['pos']
        old_x = int(pos[0])
        old_z = int(pos[2])
        pos[0] = nbtlib.Int(sx - 1 - old_x)
        pos[2] = nbtlib.Int(sz - 1 - old_z)

    # --- 3. Restore connection jigsaw to [0,1,5] west_up ---
    # Find connection jigsaw (has nbt.pool containing 'empty' or nbt.name containing 'nose_smith_house')
    conn_block_idx = None
    target_block_idx = None  # block currently at [0,1,5]

    for idx, block in enumerate(blocks):
        nbt_data = block.get('nbt', {})
        name = str(nbt_data.get('name', ''))
        if 'nose_smith_house' in name:
            pos = [int(x) for x in block['pos']]
            print(f"  Connection jigsaw after rotation: pos={pos}")
            conn_block_idx = idx

    for idx, block in enumerate(blocks):
        pos = [int(x) for x in block['pos']]
        if pos == [0, 1, 5] and idx != conn_block_idx:
            target_block_idx = idx
            break

    if conn_block_idx is not None:
        conn_block = blocks[conn_block_idx]
        conn_pos = [int(x) for x in conn_block['pos']]
        print(f"  Swapping connection jigsaw from {conn_pos} back to [0, 1, 5]")

        if target_block_idx is not None:
            # Swap positions
            target_block = blocks[target_block_idx]
            target_block['pos'] = nbtlib.List[nbtlib.Int]([
                nbtlib.Int(conn_pos[0]),
                nbtlib.Int(conn_pos[1]),
                nbtlib.Int(conn_pos[2]),
            ])
        conn_block['pos'] = nbtlib.List[nbtlib.Int]([
            nbtlib.Int(0), nbtlib.Int(1), nbtlib.Int(5),
        ])

        # Restore palette entry to west_up for the connection jigsaw.
        # The rotated palette entry is east_up. We need a west_up entry.
        # Check if any other block uses the same palette index.
        conn_state = int(conn_block['state'])
        other_uses = any(
            int(b['state']) == conn_state and i != conn_block_idx
            for i, b in enumerate(blocks)
        )

        if other_uses:
            # Create a new palette entry (clone + fix orientation)
            new_entry = copy.deepcopy(palette[conn_state])
            new_entry['Properties']['orientation'] = nbtlib.String('west_up')
            palette.append(new_entry)
            new_idx = len(palette) - 1
            conn_block['state'] = nbtlib.Int(new_idx)
            print(f"  Created new palette entry [{new_idx}] for west_up jigsaw")
        else:
            # Safe to modify in place
            palette[conn_state]['Properties']['orientation'] = nbtlib.String('west_up')
            print(f"  Restored palette[{conn_state}] to west_up")
    else:
        print("  WARNING: Connection jigsaw not found!")

    # --- 4. Save ---
    f.save(path)
    print(f"  Saved!")

    # --- 5. Verify ---
    f2 = nbtlib.load(path)
    pal2 = f2['palette']
    jigsaw_indices = {}
    for i, entry in enumerate(pal2):
        if 'jigsaw' in str(entry.get('Name', '')):
            props = entry.get('Properties', {})
            jigsaw_indices[i] = str(props.get('orientation', '?'))

    for block in f2['blocks']:
        state = int(block['state'])
        if state in jigsaw_indices:
            pos = [int(x) for x in block['pos']]
            nbt_data = block.get('nbt', {})
            pool = str(nbt_data.get('pool', '?'))
            name = str(nbt_data.get('name', '?'))
            orient = jigsaw_indices[state]
            print(f"  Verify: pos={pos} orient={orient} pool={pool} name={name}")


for biome in ['plains', 'desert', 'savanna', 'snowy', 'taiga']:
    rotate_house(biome)

print("\n=== Done! Rebuild and test in a new world. ===")
