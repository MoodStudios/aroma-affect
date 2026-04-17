package com.ovrtechnology.omara;

import com.ovrtechnology.util.Texts;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.OmaraDeviceNetworking;
import com.ovrtechnology.registry.ModSounds;
import com.ovrtechnology.scent.ScentDefinition;
import com.ovrtechnology.scent.ScentRegistry;
import com.ovrtechnology.scentitem.ScentItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class OmaraDeviceBlockEntity extends BaseContainerBlockEntity {

    private static final int CONTAINER_SIZE = 1;

    /** Mode 0 = automatic, Mode 1 = redstone pulse */
    public static final int MODE_AUTO = 0;
    public static final int MODE_REDSTONE = 1;

    /** Interval presets for automatic mode (in ticks) */
    public static final int[] INTERVAL_TICKS = {1200, 6000}; // 60s, 5min

    /** Minimum cooldown between puffs in redstone mode (2 seconds) */
    public static final int REDSTONE_COOLDOWN = 40;

    /** Duration of status indicator in ticks (3 seconds) */
    public static final int STATUS_DURATION = 60;

    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private int cooldownTicks = 0;
    private int mode = MODE_AUTO;
    private int intervalIndex = 0; // 0 = 60s, 1 = 5min
    private boolean wasPowered = false;
    private int statusTicks = 0;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> cooldownTicks;
                case 1 -> getMaxCooldownForCurrentMode();
                case 2 -> mode;
                case 3 -> intervalIndex;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> cooldownTicks = value;
                case 2 -> mode = Math.max(0, Math.min(1, value));
                case 3 -> intervalIndex = Math.max(0, Math.min(INTERVAL_TICKS.length - 1, value));
            }
            setChanged();
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public OmaraDeviceBlockEntity(BlockPos pos, BlockState state) {
        super(OmaraDeviceRegistry.OMARA_DEVICE_BLOCK_ENTITY.get(), pos, state);
    }

    public int getMaxCooldownForCurrentMode() {
        return mode == MODE_AUTO ? INTERVAL_TICKS[intervalIndex] : REDSTONE_COOLDOWN;
    }

    @Override
    protected Component getDefaultName() {
        return Texts.tr("block.aromaaffect.omara_device");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getItem() instanceof ScentItem si && si.getDefinition().isCapsule();
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ItemStack oldStack = getItem(slot);
        boolean wasEmpty = oldStack.isEmpty();
        super.setItem(slot, stack);
        setChanged();

        if (level != null && !level.isClientSide()) {
            boolean isNowEmpty = stack.isEmpty();
            if (wasEmpty && !isNowEmpty) {
                level.playSound(null, getBlockPos(), SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.2F);
            } else if (!wasEmpty && isNowEmpty) {
                level.playSound(null, getBlockPos(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 0.9F);
            }
        }
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new OmaraDeviceMenu(containerId, inventory, this, this.dataAccess);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("CooldownTicks", cooldownTicks);
        output.putInt("Mode", mode);
        output.putInt("IntervalIndex", intervalIndex);
        output.putInt("StatusTicks", statusTicks);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.cooldownTicks = input.getIntOr("CooldownTicks", 0);
        this.mode = input.getIntOr("Mode", MODE_AUTO);
        this.intervalIndex = input.getIntOr("IntervalIndex", 0);
        this.statusTicks = input.getIntOr("StatusTicks", 0);
    }

    // ========================================
    // Server Tick
    // ========================================

    private void setStatus(Level level, BlockPos pos, BlockState state, int status, int ticks) {
        statusTicks = ticks;
        BlockState newState = state.setValue(OmaraDeviceBlock.STATUS, status);
        level.setBlock(pos, newState, 3);
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, OmaraDeviceBlockEntity be) {
        if (level.isClientSide()) return;

        // Status indicator timer
        if (be.statusTicks > 0) {
            be.statusTicks--;
            if (be.statusTicks <= 0) {
                be.setStatus(level, pos, state, 0, 0);
                state = level.getBlockState(pos); // refresh after state change
            }
        } else if (state.getValue(OmaraDeviceBlock.STATUS) != 0) {
            // Crash recovery: status is non-zero but timer expired
            be.setStatus(level, pos, state, 0, 0);
            state = level.getBlockState(pos);
        }

        ItemStack capsule = be.getItem(0);
        boolean hasCapsule = !capsule.isEmpty()
                && capsule.getItem() instanceof ScentItem si
                && si.getDefinition().isCapsule();

        if (!hasCapsule) {
            if (be.cooldownTicks != 0) {
                be.cooldownTicks = 0;
                be.setChanged();
            }
            // Flash red on redstone rising edge with no capsule loaded
            boolean powered = level.hasNeighborSignal(pos);
            if (be.mode == MODE_REDSTONE && powered && !be.wasPowered) {
                be.setStatus(level, pos, state, 2, STATUS_DURATION);
                state = level.getBlockState(pos);
            }
            // Keep wasPowered in sync so inserting a capsule while powered doesn't trigger an immediate puff
            be.wasPowered = powered;
            return;
        }

        ScentItem si = (ScentItem) capsule.getItem();

        if (be.mode == MODE_AUTO) {
            // Automatic mode: when cooldownTicks is 0, start a new countdown (no immediate puff)
            if (be.cooldownTicks <= 0) {
                be.cooldownTicks = be.getMaxCooldownForCurrentMode();
                be.setChanged();
                return;
            }
            be.cooldownTicks--;
            be.setChanged();
            if (be.cooldownTicks <= 0) {
                be.puff(level, pos, state, capsule, si);
            }
        } else {
            // Redstone mode: puff on rising edge when cooldown is 0
            boolean powered = level.hasNeighborSignal(pos);

            if (be.cooldownTicks > 0) {
                be.cooldownTicks--;
                be.setChanged();
            }

            if (powered && !be.wasPowered && be.cooldownTicks <= 0) {
                be.puff(level, pos, state, capsule, si);
            }

            be.wasPowered = powered;
        }
    }

    private void puff(Level level, BlockPos pos, BlockState state, ItemStack capsule, ScentItem scentItem) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        String scentName = scentItem.getDefinition().getScent();

        // Consume 1 durability
        if (capsule.isDamageableItem()) {
            int newDamage = capsule.getDamageValue() + 1;
            if (newDamage >= capsule.getMaxDamage()) {
                setItem(0, ItemStack.EMPTY);
            } else {
                capsule.setDamageValue(newDamage);
                setChanged();
            }
        }

        // Reset cooldown based on current mode
        cooldownTicks = getMaxCooldownForCurrentMode();

        // Flash green status indicator
        setStatus(level, pos, state, 1, STATUS_DURATION);
        state = level.getBlockState(pos);

        // Play puff sound (3D spatial, audible nearby)
        serverLevel.playSound(null, pos, ModSounds.OMARA_PUFF.get(), SoundSource.BLOCKS, 1.0F, 1.0F);

        // Spawn particles
        spawnPuffParticles(serverLevel, pos, state, scentName);

        // Broadcast scent trigger to all players within a 3x3 block area
        OmaraDeviceNetworking.broadcastPuff(serverLevel, pos, scentName);

        AromaAffect.LOGGER.debug("Omara Device puffed scent '{}' at {} (mode={})", scentName, pos, mode == MODE_AUTO ? "auto" : "redstone");
    }

    private void spawnPuffParticles(ServerLevel serverLevel, BlockPos pos, BlockState state, String scentName) {
        int[] rgb = {255, 255, 255};
        Optional<ScentDefinition> scentOpt = ScentRegistry.getScentByName(scentName);
        if (scentOpt.isPresent()) {
            rgb = scentOpt.get().getColorRGB();
        }

        int argb = (255 << 24) | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
        var particle = ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, argb);

        Direction mouthDir = getMouthDirection(state);
        Vec3 dir = directionToVec3(mouthDir);
        Vec3 center = Vec3.atCenterOf(pos).add(dir.scale(0.6));

        var random = serverLevel.getRandom();

        for (int i = 0; i < 18; i++) {
            double spread = 0.15 + random.nextDouble() * 0.25;

            Vec3 perp1;
            if (Math.abs(dir.y) < 0.9) {
                perp1 = dir.cross(new Vec3(0, 1, 0)).normalize();
            } else {
                perp1 = dir.cross(new Vec3(1, 0, 0)).normalize();
            }
            Vec3 perp2 = dir.cross(perp1).normalize();
            double angle = random.nextDouble() * Math.PI * 2;

            double px = center.x + perp1.x * Math.cos(angle) * spread + perp2.x * Math.sin(angle) * spread;
            double py = center.y + perp1.y * Math.cos(angle) * spread + perp2.y * Math.sin(angle) * spread;
            double pz = center.z + perp1.z * Math.cos(angle) * spread + perp2.z * Math.sin(angle) * spread;

            serverLevel.sendParticles(particle, px, py, pz, 1,
                    dir.x * 0.05, dir.y * 0.05, dir.z * 0.05, 0.02);
        }
    }

    private static Direction getMouthDirection(BlockState state) {
        Direction facing = state.getValue(OmaraDeviceBlock.FACING);
        boolean wall = state.getValue(OmaraDeviceBlock.WALL);

        if (!wall) {
            return facing.getOpposite();
        }

        int mount = state.getValue(OmaraDeviceBlock.MOUNT);
        return switch (mount) {
            case 0 -> Direction.UP;
            case 2 -> Direction.DOWN;
            case 1 -> facing.getClockWise();
            case 3 -> facing.getCounterClockWise();
            default -> facing.getOpposite();
        };
    }

    private static Vec3 directionToVec3(Direction direction) {
        return switch (direction) {
            case NORTH -> new Vec3(0, 0, -1);
            case SOUTH -> new Vec3(0, 0, 1);
            case EAST -> new Vec3(1, 0, 0);
            case WEST -> new Vec3(-1, 0, 0);
            case UP -> new Vec3(0, 1, 0);
            case DOWN -> new Vec3(0, -1, 0);
        };
    }
}
