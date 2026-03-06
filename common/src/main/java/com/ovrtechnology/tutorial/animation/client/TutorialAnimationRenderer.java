package com.ovrtechnology.tutorial.animation.client;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.compat.ReplayCompat;
import com.ovrtechnology.tutorial.animation.TutorialAnimationType;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Client-side particle renderer for tutorial animations.
 * <p>
 * Receives animation play events via networking and spawns dense
 * client-side particles to enhance the visual effect beyond what
 * server-side particles alone can provide.
 */
public final class TutorialAnimationRenderer {

    private static final double RENDER_DISTANCE_SQ = 80 * 80;
    private static final int ANIMATION_DURATION_TICKS = 60; // ~3 seconds of extra particles
    private static final int PARTICLE_INTERVAL = 3;         // Spawn particles every 3 ticks

    // OVR Purple dust particle
    private static final DustParticleOptions PURPLE_DUST = new DustParticleOptions(
            0xFFA890F0,
            1.0f
    );

    // Gold accent
    private static final DustParticleOptions GOLD_DUST = new DustParticleOptions(
            0xFFFFD700,
            0.8f
    );

    private static final List<ActiveClientAnimation> activeAnimations = new CopyOnWriteArrayList<>();
    private static int tickCounter = 0;
    private static boolean initialized = false;

    private TutorialAnimationRenderer() {
    }

    /**
     * Initializes the animation renderer.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        ClientTickEvent.CLIENT_POST.register(client -> {
            if (ReplayCompat.isInReplay()) return;
            onClientTick(client);
        });
        AromaAffect.LOGGER.debug("Tutorial animation renderer initialized");
    }

    /**
     * Called from networking when a PLAY packet arrives.
     *
     * @param type    the animation type
     * @param corner1 first corner of the cuboid
     * @param corner2 second corner of the cuboid
     */
    public static void onAnimationPlay(TutorialAnimationType type, BlockPos corner1, BlockPos corner2) {
        activeAnimations.add(new ActiveClientAnimation(type, corner1, corner2));
    }

    /**
     * Clears all active client animations.
     */
    public static void clearAnimations() {
        activeAnimations.clear();
    }

    private static void onClientTick(Minecraft minecraft) {
        if (activeAnimations.isEmpty()) {
            return;
        }

        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            return;
        }

        tickCounter++;
        if (tickCounter < PARTICLE_INTERVAL) {
            return;
        }
        tickCounter = 0;

        Vec3 playerPos = player.position();

        Iterator<ActiveClientAnimation> it = activeAnimations.iterator();
        while (it.hasNext()) {
            ActiveClientAnimation anim = it.next();
            anim.ticksAlive += PARTICLE_INTERVAL;

            if (anim.ticksAlive > ANIMATION_DURATION_TICKS) {
                activeAnimations.remove(anim);
                continue;
            }

            Vec3 center = anim.getCenter();
            if (playerPos.distanceToSqr(center) > RENDER_DISTANCE_SQ) {
                continue;
            }

            renderAnimationParticles(level, anim);
        }
    }

    private static void renderAnimationParticles(ClientLevel level, ActiveClientAnimation anim) {
        Vec3 center = anim.getCenter();
        double progress = (double) anim.ticksAlive / ANIMATION_DURATION_TICKS;

        double spreadX = (anim.maxX - anim.minX) / 2.0 + 0.5;
        double spreadY = (anim.maxY - anim.minY) / 2.0 + 0.5;
        double spreadZ = (anim.maxZ - anim.minZ) / 2.0 + 0.5;

        switch (anim.type) {
            case WALL_BREAK -> {
                // Expanding debris cloud
                double expand = 1.0 + progress * 2.0;
                for (int i = 0; i < 8; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double radius = Math.random() * spreadX * expand;
                    double x = center.x + Math.cos(angle) * radius;
                    double y = center.y + (Math.random() - 0.5) * spreadY * 2;
                    double z = center.z + Math.sin(angle) * radius;

                    level.addParticle(PURPLE_DUST, x, y, z, 0, 0.02, 0);
                }
                // Smoke rising
                for (int i = 0; i < 3; i++) {
                    level.addParticle(
                            ParticleTypes.CAMPFIRE_COSY_SMOKE,
                            center.x + (Math.random() - 0.5) * spreadX,
                            center.y + Math.random() * spreadY,
                            center.z + (Math.random() - 0.5) * spreadZ,
                            0, 0.05, 0
                    );
                }
            }
            case DOOR_OPEN -> {
                // Sweeping particles along the split axis
                for (int i = 0; i < 6; i++) {
                    double sweep = (progress - 0.5) * spreadX * 3;
                    double x = center.x + sweep + (Math.random() - 0.5) * 0.5;
                    double y = center.y + (Math.random() - 0.5) * spreadY * 2;
                    double z = center.z + (Math.random() - 0.5) * spreadZ * 2;

                    level.addParticle(PURPLE_DUST, x, y, z, 0, 0, 0);
                }
                // End rod sparkles
                for (int i = 0; i < 3; i++) {
                    level.addParticle(
                            ParticleTypes.END_ROD,
                            center.x + (Math.random() - 0.5) * spreadX * 2,
                            center.y + (Math.random() - 0.5) * spreadY * 2,
                            center.z + (Math.random() - 0.5) * spreadZ * 2,
                            0, 0.03, 0
                    );
                }
            }
            case DEBRIS_CLEAR -> {
                // Sparkles rising upward
                for (int i = 0; i < 6; i++) {
                    double x = center.x + (Math.random() - 0.5) * spreadX * 2;
                    double y = center.y + progress * spreadY * 2;
                    double z = center.z + (Math.random() - 0.5) * spreadZ * 2;

                    level.addParticle(GOLD_DUST, x, y, z, 0, 0.06, 0);
                }
                // Cherry leaves
                for (int i = 0; i < 4; i++) {
                    level.addParticle(
                            ParticleTypes.CHERRY_LEAVES,
                            center.x + (Math.random() - 0.5) * spreadX * 2,
                            center.y + (Math.random() - 0.5) * spreadY * 2,
                            center.z + (Math.random() - 0.5) * spreadZ * 2,
                            0, 0.04, 0
                    );
                }
                // Enchant glyphs
                if (Math.random() < 0.5) {
                    level.addParticle(
                            ParticleTypes.ENCHANT,
                            center.x + (Math.random() - 0.5) * spreadX,
                            center.y - spreadY,
                            center.z + (Math.random() - 0.5) * spreadZ,
                            0, 1.0, 0
                    );
                }
            }
        }
    }

    private static class ActiveClientAnimation {
        final TutorialAnimationType type;
        final int minX, minY, minZ;
        final int maxX, maxY, maxZ;
        int ticksAlive;

        ActiveClientAnimation(TutorialAnimationType type, BlockPos corner1, BlockPos corner2) {
            this.type = type;
            this.minX = Math.min(corner1.getX(), corner2.getX());
            this.minY = Math.min(corner1.getY(), corner2.getY());
            this.minZ = Math.min(corner1.getZ(), corner2.getZ());
            this.maxX = Math.max(corner1.getX(), corner2.getX());
            this.maxY = Math.max(corner1.getY(), corner2.getY());
            this.maxZ = Math.max(corner1.getZ(), corner2.getZ());
            this.ticksAlive = 0;
        }

        Vec3 getCenter() {
            return new Vec3(
                    (minX + maxX) / 2.0 + 0.5,
                    (minY + maxY) / 2.0 + 0.5,
                    (minZ + maxZ) / 2.0 + 0.5
            );
        }
    }
}
