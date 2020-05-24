package com.sistr.lmrb.entity.mode;

import com.sistr.lmrb.entity.PlayerWrapper;
import com.sistr.lmrb.util.ModeManager;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.SwordItem;

public class ArcherMode implements IMode {
    private final CreatureEntity owner;
    private int cooltime;

    public ArcherMode(CreatureEntity owner) {
        this.owner = owner;
    }

    @Override
    public void startModeTask() {

    }

    @Override
    public boolean shouldExecute() {
        return this.owner.getAttackTarget() != null;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return shouldExecute();
    }

    public void startExecuting() {
        this.owner.setAggroed(true);
    }

    @Override
    public void tick() {
        LivingEntity target = this.owner.getAttackTarget();
        if (target == null) return;
        this.owner.faceEntity(target, 30.0F, 30.0F);
        this.cooltime--;
        if (cooltime <= 0) {
            this.cooltime = 40;
            owner.getHeldItemMainhand().getItem().onPlayerStoppedUsing(owner.getHeldItemMainhand(), owner.world, new PlayerWrapper(owner), 40);
        }
    }

    @Override
    public void resetTask() {
        this.owner.setAggroed(false);
    }

    @Override
    public void endModeTask() {

    }

    @Override
    public String getName() {
        return "archer";
    }

    static {
        ModeManager.ModeItems items = new ModeManager.ModeItems();
        items.add(BowItem.class);
        ModeManager.INSTANCE.register(ArcherMode.class, items);
    }
}
