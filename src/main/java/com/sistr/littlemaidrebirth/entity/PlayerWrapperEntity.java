package com.sistr.littlemaidrebirth.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.util.DamageSource;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.FakePlayer;

//エンティティをプレイヤーにラップするクラス
//基本的にサーバーオンリー
//アイテムの使用/アイテム回収/その他
public class PlayerWrapperEntity extends FakePlayer {
    private final LivingEntity origin;

    public PlayerWrapperEntity(LivingEntity origin) {
        super((ServerWorld) origin.world, new GameProfile(origin.getUniqueID(), origin.getType().getName().getFormattedText() + "_player_wrapper"));
        this.origin = origin;
        setEntityId(origin.getEntityId());
    }

    //エラーログ避け、無くてもクラッシュはしない
    @Override
    protected void registerAttributes() {
        super.registerAttributes();
        this.getAttributes().registerAttribute(SharedMonsterAttributes.FOLLOW_RANGE);
        this.getAttributes().registerAttribute(SharedMonsterAttributes.ATTACK_KNOCKBACK);
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        spawnDrops(source);
    }
}
