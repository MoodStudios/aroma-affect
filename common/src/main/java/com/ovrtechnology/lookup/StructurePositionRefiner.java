package com.ovrtechnology.lookup;

import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public final class StructurePositionRefiner {

    private static final int MAX_PIECES_TO_SCAN = 3;
    private static final int MAX_BLOCKS_TO_SCAN = 5000;

    private static final Set<Block> NATURAL_BLOCKS =
            Set.of(
                    Blocks.AIR,
                    Blocks.CAVE_AIR,
                    Blocks.VOID_AIR,
                    Blocks.WATER,
                    Blocks.LAVA,
                    Blocks.DIRT,
                    Blocks.GRASS_BLOCK,
                    Blocks.COARSE_DIRT,
                    Blocks.ROOTED_DIRT,
                    Blocks.PODZOL,
                    Blocks.MYCELIUM,
                    Blocks.STONE,
                    Blocks.DEEPSLATE,
                    Blocks.GRANITE,
                    Blocks.DIORITE,
                    Blocks.ANDESITE,
                    Blocks.TUFF,
                    Blocks.CALCITE,
                    Blocks.SAND,
                    Blocks.RED_SAND,
                    Blocks.GRAVEL,
                    Blocks.SNOW,
                    Blocks.SNOW_BLOCK,
                    Blocks.ICE,
                    Blocks.PACKED_ICE,
                    Blocks.BLUE_ICE,
                    Blocks.CLAY,
                    Blocks.MUD,
                    Blocks.SOUL_SAND,
                    Blocks.SOUL_SOIL,
                    Blocks.NETHERRACK,
                    Blocks.BASALT,
                    Blocks.BLACKSTONE,
                    Blocks.END_STONE,
                    Blocks.BEDROCK,
                    Blocks.TALL_GRASS,
                    Blocks.SHORT_GRASS,
                    Blocks.FERN,
                    Blocks.LARGE_FERN,
                    Blocks.SEAGRASS,
                    Blocks.TALL_SEAGRASS,
                    Blocks.KELP,
                    Blocks.KELP_PLANT);

    private StructurePositionRefiner() {}

    public static BlockPos refine(
            ServerLevel level, BlockPos approxPos, ResourceLocation structureId) {
        try {
            Registry<Structure> structureRegistry =
                    level.registryAccess().registryOrThrow(Registries.STRUCTURE);
            Structure structure = structureRegistry.get(structureId);
            if (structure == null) {
                return approxPos;
            }

            ChunkPos chunkPos = new ChunkPos(approxPos);
            ChunkAccess chunk = level.getChunk(chunkPos.x, chunkPos.z);

            StructureStart start =
                    level.structureManager()
                            .getStartForStructure(SectionPos.bottomOf(chunk), structure, chunk);

            if (start == null || !start.isValid()) {
                return approxPos;
            }

            List<StructurePiece> pieces = start.getPieces();
            if (pieces.isEmpty()) {
                return approxPos;
            }

            int blocksScanned = 0;
            int piecesScanned = 0;

            for (StructurePiece piece : pieces) {
                if (piecesScanned >= MAX_PIECES_TO_SCAN) break;
                piecesScanned++;

                BoundingBox bb = piece.getBoundingBox();
                for (int y = bb.maxY(); y >= bb.minY() && blocksScanned < MAX_BLOCKS_TO_SCAN; y--) {
                    for (int x = bb.minX();
                            x <= bb.maxX() && blocksScanned < MAX_BLOCKS_TO_SCAN;
                            x++) {
                        for (int z = bb.minZ();
                                z <= bb.maxZ() && blocksScanned < MAX_BLOCKS_TO_SCAN;
                                z++) {
                            blocksScanned++;
                            BlockPos pos = new BlockPos(x, y, z);
                            BlockState state = level.getBlockState(pos);
                            if (!state.isAir() && isStructureBlock(state)) {
                                return pos;
                            }
                        }
                    }
                }
            }

            BoundingBox firstBB = pieces.getFirst().getBoundingBox();
            return new BlockPos(firstBB.getCenter());

        } catch (Exception e) {
            return approxPos;
        }
    }

    private static boolean isStructureBlock(BlockState state) {
        return !NATURAL_BLOCKS.contains(state.getBlock());
    }
}
