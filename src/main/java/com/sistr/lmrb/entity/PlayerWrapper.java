package com.sistr.lmrb.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

//world.addEntity()したら多分バグる
public class PlayerWrapper extends FakePlayer {

    //UUIDが一緒なので、getEntityByUuid()でオリジナルの方が帰ってくる
    public PlayerWrapper(LivingEntity origin) {
        super((ServerWorld) origin.world, new GameProfile(origin.getUniqueID(), origin.getType().getRegistryName().getPath() + "test"));
    }

    @Override
    protected void registerAttributes() {
        super.registerAttributes();
        this.getAttributes().registerAttribute(SharedMonsterAttributes.FOLLOW_RANGE);
        this.getAttributes().registerAttribute(SharedMonsterAttributes.ATTACK_KNOCKBACK);
    }

    //今のところ未使用
    public static void onLivingAttack(LivingAttackEvent event) {
        DamageSource source = event.getSource();
        Entity attacker = source.getTrueSource();
        if (attacker instanceof PlayerWrapper) {
            event.setCanceled(true);
            DamageSource newSource = new DamageSource(source.getDamageType());
            setSourceSettings(newSource, attacker);
            event.getEntity().attackEntityFrom(newSource, event.getAmount());
        }
    }

    public static void setSourceSettings(DamageSource source, Entity attacker) {

    }

}
