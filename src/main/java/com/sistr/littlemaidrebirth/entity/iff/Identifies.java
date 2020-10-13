package com.sistr.littlemaidrebirth.entity.iff;

import net.minecraft.entity.*;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.IFlyingAnimal;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.WaterMobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.pathfinding.FlyingPathNavigator;

public class Identifies {
    public static final Identify CREATURE = entity -> entity instanceof CreatureEntity;
    public static final Identify MONSTER = entity -> entity instanceof MonsterEntity;
    public static final Identify ANIMAL = entity -> entity instanceof AnimalEntity;
    public static final Identify TAMEABLE = entity -> entity instanceof TameableEntity;
    public static final Identify WATER = entity -> entity instanceof WaterMobEntity;
    public static final Identify FLYABLE = entity -> entity instanceof FlyingEntity || entity instanceof IFlyingAnimal
            || entity instanceof MobEntity && ((MobEntity) entity).getNavigator() instanceof FlyingPathNavigator;
    public static final Identify CHARGEABLE = entity -> entity instanceof IChargeableMob;
    public static final Identify RANGED_ATTACKABLE = entity -> entity instanceof IRangedAttackMob
            || entity instanceof ICrossbowUser;
    public static final Identify NPC = entity -> entity instanceof INPC;
    public static final Identify BOSS = entity -> !entity.isNonBoss();
    public static final Identify PLAYER = entity -> entity instanceof PlayerEntity;
}
