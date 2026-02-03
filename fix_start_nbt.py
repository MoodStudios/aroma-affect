"""
Fix the nose_smith_start.nbt for plains and taiga.

Problem: The house connector jigsaw is at z=4, which causes the house
bounding box to start at z=-1, colliding with street pieces.

Fix: Move the house jigsaw from z=4 to z=5 (swap with whatever block
is at the target position). This makes the house origin z=0, matching
the working biomes (savanna, snowy).
"""

import nbtlib
import os
import shutil

base = r"C:\Users\toxic\Projects\MoodStudios\Github\aromacraft\common\src\main\resources\data\aromaaffect\structure\village"

def fix_start_piece(biome):
    path = os.path.join(base, biome, "town_centers", "nose_smith_start.nbt")
    backup = path + ".bak"

    print(f"\n=== Fixing {biome} start piece ===")
    print(f"  File: {path}")

    # Backup
    shutil.copy2(path, backup)
    print(f"  Backup saved to: {backup}")

    f = nbtlib.load(path)
    root = f

    size = [int(x) for x in root['size']]
    print(f"  Structure size: {size}")

    # Find palette indices for jigsaw blocks
    palette = root['palette']
    jigsaw_indices = {}
    for i, entry in enumerate(palette):
        name = str(entry.get('Name', ''))
        if 'jigsaw' in name:
            props = entry.get('Properties', {})
            orient = str(props.get('orientation', '?'))
            jigsaw_indices[i] = orient

    # Find the house connector jigsaw (the one with aromaaffect pool and east_up orientation)
    blocks = root['blocks']
    house_jigsaw_idx = None
    house_jigsaw_pos = None

    for idx, block in enumerate(blocks):
        state = int(block['state'])
        if state in jigsaw_indices:
            nbt_data = block.get('nbt', {})
            pool = str(nbt_data.get('pool', ''))
            if 'nose_smith_house' in pool:
                pos = [int(x) for x in block['pos']]
                print(f"  Found house jigsaw at pos={pos}, orientation={jigsaw_indices[state]}")
                house_jigsaw_idx = idx
                house_jigsaw_pos = pos
                break

    if house_jigsaw_idx is None:
        print("  ERROR: House jigsaw not found!")
        return

    current_z = house_jigsaw_pos[2]
    target_z = 5  # Match savanna/snowy which work

    if current_z == target_z:
        print(f"  Jigsaw already at z={target_z}, no fix needed")
        return

    if current_z >= target_z:
        print(f"  Jigsaw at z={current_z}, already >= {target_z}, no fix needed")
        return

    print(f"  Moving jigsaw from z={current_z} to z={target_z}")

    target_pos = [house_jigsaw_pos[0], house_jigsaw_pos[1], target_z]

    # Find the block currently at the target position
    target_block_idx = None
    for idx, block in enumerate(blocks):
        pos = [int(x) for x in block['pos']]
        if pos == target_pos:
            target_block_idx = idx
            state = int(block['state'])
            block_name = str(palette[state].get('Name', '?'))
            print(f"  Block at target pos {target_pos}: {block_name} (palette idx {state})")
            break

    if target_block_idx is None:
        print(f"  No block entry at target position {target_pos} - position might be air (implicit)")
        # In NBT structures, positions without entries are air.
        # We need to add an air entry at the old position and move the jigsaw.

        # Find or create air palette entry
        air_palette_idx = None
        for i, entry in enumerate(palette):
            if str(entry.get('Name', '')) == 'minecraft:air':
                air_palette_idx = i
                break

        if air_palette_idx is None:
            # Add air to palette
            air_entry = nbtlib.Compound({
                'Name': nbtlib.String('minecraft:air')
            })
            palette.append(air_entry)
            air_palette_idx = len(palette) - 1
            print(f"  Added air to palette at index {air_palette_idx}")

        # Move the jigsaw to new position
        blocks[house_jigsaw_idx]['pos'] = nbtlib.List[nbtlib.Int]([
            nbtlib.Int(target_pos[0]),
            nbtlib.Int(target_pos[1]),
            nbtlib.Int(target_pos[2])
        ])
        print(f"  Moved jigsaw to {target_pos}")
    else:
        # Swap positions between the two blocks
        old_pos = [house_jigsaw_pos[0], house_jigsaw_pos[1], house_jigsaw_pos[2]]

        # Move jigsaw to target position
        blocks[house_jigsaw_idx]['pos'] = nbtlib.List[nbtlib.Int]([
            nbtlib.Int(target_pos[0]),
            nbtlib.Int(target_pos[1]),
            nbtlib.Int(target_pos[2])
        ])

        # Move target block to old jigsaw position
        blocks[target_block_idx]['pos'] = nbtlib.List[nbtlib.Int]([
            nbtlib.Int(old_pos[0]),
            nbtlib.Int(old_pos[1]),
            nbtlib.Int(old_pos[2])
        ])

        print(f"  Swapped: jigsaw -> {target_pos}, other block -> {old_pos}")

    # Save
    f.save(path)
    print(f"  Saved!")

    # Verify
    f2 = nbtlib.load(path)
    for block in f2['blocks']:
        state = int(block['state'])
        if state in jigsaw_indices:
            nbt_data = block.get('nbt', {})
            pool = str(nbt_data.get('pool', ''))
            if 'nose_smith_house' in pool:
                pos = [int(x) for x in block['pos']]
                print(f"  Verified: house jigsaw now at pos={pos}")
                break


fix_start_piece("plains")
fix_start_piece("taiga")

print("\n=== Done! Rebuild the mod and test with a new world. ===")
