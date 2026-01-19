package com.ovrtechnology.entity;

import com.ovrtechnology.AromaCraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

/**
 * The Nose Smith is an NPC that helps players acquire and unlock Scent Masks.
 * This character is central to the AromaCraft progression system, guiding players
 * through the mask tier system and providing access to nose equipment.
 */
public class NoseSmithEntity extends Villager {
    
    public NoseSmithEntity(EntityType<? extends Villager> entityType, Level level) {
        super(entityType, level);
        // Nose Smith doesn't need to worry about profession data as much
        this.setCustomName(net.minecraft.network.chat.Component.translatable("entity.aromacraft.nose_smith"));
        this.setCustomNameVisible(true);
    }
    
    /**
     * Create the attribute builder for the Nose Smith.
     * Based on standard Villager attributes with slightly higher health.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 25.0D) // Slightly more health than regular villager
                .add(Attributes.MOVEMENT_SPEED, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }
    
    @Override
    protected void registerGoals() {
        // Basic AI goals for the Nose Smith
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 0.5D));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.35D));
    }
    
    @Override
    @Nullable
    public Villager getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        // Nose Smiths cannot breed - they are unique NPCs
        return null;
    }
    
    @Override
    public boolean canBreed() {
        // Nose Smiths cannot breed
        return false;
    }
    
    @Override
    public boolean removeWhenFarAway(double distance) {
        // Never despawn the Nose Smith
        return false;
    }
    
    @Override
    public boolean isPersistenceRequired() {
        // Always persist
        return true;
    }
    
    // TODO: Add custom trading logic for nose items and scent masks
    // This will be implemented when the trading system is designed
}
