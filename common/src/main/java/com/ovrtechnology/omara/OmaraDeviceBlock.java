package com.ovrtechnology.omara;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class OmaraDeviceBlock extends BaseEntityBlock {

    public static final MapCodec<OmaraDeviceBlock> CODEC = simpleCodec(OmaraDeviceBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty WALL = BooleanProperty.create("wall");

    public static final IntegerProperty MOUNT = IntegerProperty.create("mount", 0, 3);

    public static final IntegerProperty STATUS = IntegerProperty.create("status", 0, 2);

    private static final VoxelShape FLOOR_NS = Block.box(1.5, 0, 3.5, 14.5, 5, 12.5);
    private static final VoxelShape FLOOR_EW = Block.box(3.5, 0, 1.5, 12.5, 5, 14.5);

    private static final VoxelShape WALL_N = Block.box(1.5, 3.5, 0, 14.5, 12.5, 5);
    private static final VoxelShape WALL_S = Block.box(1.5, 3.5, 11, 14.5, 12.5, 16);
    private static final VoxelShape WALL_E = Block.box(11, 3.5, 1.5, 16, 12.5, 14.5);
    private static final VoxelShape WALL_W = Block.box(0, 3.5, 1.5, 5, 12.5, 14.5);

    private static final VoxelShape WALL_SIDE_N = Block.box(3.5, 1.5, 0, 12.5, 14.5, 5);
    private static final VoxelShape WALL_SIDE_S = Block.box(3.5, 1.5, 11, 12.5, 14.5, 16);
    private static final VoxelShape WALL_SIDE_E = Block.box(11, 1.5, 3.5, 16, 14.5, 12.5);
    private static final VoxelShape WALL_SIDE_W = Block.box(0, 1.5, 3.5, 5, 14.5, 12.5);

    @SuppressWarnings("this-escape")
    public OmaraDeviceBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(WALL, false)
                        .setValue(MOUNT, 0)
                        .setValue(STATUS, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WALL, MOUNT, STATUS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();

        if (clickedFace.getAxis().isHorizontal()) {
            double relY = context.getClickLocation().y - context.getClickedPos().getY() - 0.5;

            double relH =
                    switch (clickedFace) {
                        case SOUTH -> context.getClickLocation().x
                                - context.getClickedPos().getX()
                                - 0.5;
                        case NORTH -> -(context.getClickLocation().x
                                - context.getClickedPos().getX()
                                - 0.5);
                        case WEST -> context.getClickLocation().z
                                - context.getClickedPos().getZ()
                                - 0.5;
                        case EAST -> -(context.getClickLocation().z
                                - context.getClickedPos().getZ()
                                - 0.5);
                        default -> 0;
                    };

            int mount;
            if (Math.abs(relY) >= Math.abs(relH)) {
                mount = relY >= 0 ? 0 : 2;
            } else {
                mount = relH >= 0 ? 1 : 3;
            }

            return this.defaultBlockState()
                    .setValue(WALL, true)
                    .setValue(FACING, clickedFace.getOpposite())
                    .setValue(MOUNT, mount);
        }

        return this.defaultBlockState()
                .setValue(WALL, false)
                .setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(WALL)) {
            int mount = state.getValue(MOUNT);
            if (mount == 1 || mount == 3) {
                return switch (state.getValue(FACING)) {
                    case NORTH -> WALL_SIDE_N;
                    case SOUTH -> WALL_SIDE_S;
                    case EAST -> WALL_SIDE_E;
                    case WEST -> WALL_SIDE_W;
                    default -> FLOOR_NS;
                };
            }
            return switch (state.getValue(FACING)) {
                case NORTH -> WALL_N;
                case SOUTH -> WALL_S;
                case EAST -> WALL_E;
                case WEST -> WALL_W;
                default -> FLOOR_NS;
            };
        }

        return switch (state.getValue(FACING)) {
            case EAST, WEST -> FLOOR_EW;
            default -> FLOOR_NS;
        };
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof OmaraDeviceBlockEntity omaraDevice) {
                serverPlayer.openMenu(omaraDevice);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OmaraDeviceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return type == OmaraDeviceRegistry.OMARA_DEVICE_BLOCK_ENTITY.get()
                ? (lvl, pos, st, be) ->
                        OmaraDeviceBlockEntity.serverTick(lvl, pos, st, (OmaraDeviceBlockEntity) be)
                : null;
    }
}
