package com.sistr.littlemaidrebirth.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Pose;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.nbt.CompoundNBT;
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
        //Material MasterというModでATTACK_KNOCKBACKの属性値追加を生成時に行っているための回避策
        //EntityConstructingのタイミングが後に変わった場合は不要になると思う
        if (this.getAttribute(SharedMonsterAttributes.FOLLOW_RANGE) == null) {
            this.getAttributes().registerAttribute(SharedMonsterAttributes.FOLLOW_RANGE);
        }
        if (this.getAttribute(SharedMonsterAttributes.ATTACK_KNOCKBACK) == null) {
            this.getAttributes().registerAttribute(SharedMonsterAttributes.ATTACK_KNOCKBACK);
        }
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        spawnDrops(source);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        origin.attackEntityFrom(source, amount);
        return super.attackEntityFrom(source, amount);
    }

    @Override
    public EntitySize getSize(Pose poseIn) {
        return origin.getSize(poseIn);
    }

    @Override
    public float getStandingEyeHeight(Pose poseIn, EntitySize sizeIn) {
        return sizeIn.height * 0.85F;
    }

}
