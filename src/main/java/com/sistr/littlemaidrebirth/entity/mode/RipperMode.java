package com.sistr.littlemaidrebirth.entity.mode;

import com.google.common.collect.Lists;
import com.sistr.littlemaidrebirth.util.ModeManager;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShearsItem;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.IForgeShearable;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class RipperMode implements IMode {
    protected final CreatureEntity owner;
    protected final List<Entity> shearable = Lists.newArrayList();
    protected int timeToRecalcPath;
    protected int timeToIgnore;

    public RipperMode(CreatureEntity owner) {
        this.owner = owner;
    }

    @Override
    public void startModeTask() {

    }

    @Override
    public boolean shouldExecute() {
        this.shearable.addAll(findCanShearableMob());
        return !this.shearable.isEmpty();
    }

    public Collection<Entity> findCanShearableMob() {
        float searchRadius = 16;
        AxisAlignedBB bb = new AxisAlignedBB(
                this.owner.getPosX() + searchRadius,
                this.owner.getPosY() + searchRadius / 4,
                this.owner.getPosZ() + searchRadius,
                this.owner.getPosX() - searchRadius,
                this.owner.getPosY() - searchRadius / 4,
                this.owner.getPosZ() - searchRadius);
        return this.owner.world.getEntitiesInAABBexcluding(this.owner, bb, (entity) ->
                entity instanceof LivingEntity && entity instanceof IForgeShearable
                        && ((IForgeShearable) entity).isShearable(this.owner.getHeldItemMainhand(), this.owner.world, entity.getPosition())
                        && this.owner.getEntitySenses().canSee(entity));
    }

    @Override
    public boolean shouldContinueExecuting() {
        return !this.shearable.isEmpty();
    }

    @Override
    public void startExecuting() {
        this.owner.getNavigator().clearPath();
        List<Entity> tempList = this.shearable.stream()
                .sorted(Comparator.comparingDouble(entity -> entity.getDistanceSq(this.owner)))
                .collect(Collectors.toList());
        this.shearable.clear();
        this.shearable.addAll(tempList);
    }

    @Override
    public void tick() {
        Entity target = this.shearable.get(0);
        if (!(target instanceof LivingEntity) || !(target instanceof IForgeShearable)) {
            this.shearable.remove(0);
            this.timeToIgnore = 0;
            return;
        }
        if (200 < ++this.timeToIgnore) {
            this.shearable.remove(0);
            this.timeToIgnore = 0;
            return;
        }
        if (target.getDistanceSq(this.owner) < 2 * 2) {
            ItemStack stack = this.owner.getHeldItemMainhand();
            BlockPos pos = new BlockPos(target.getPosX(), target.getPosY(), target.getPosZ());
            if (((IForgeShearable) target).isShearable(stack, target.world, pos)) {
                //todo プレイヤー？
                List<ItemStack> drops = ((IForgeShearable) target).onSheared(null, stack, target.world, pos,
                        EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, stack));
                Random rand = new Random();
                drops.forEach(drop -> {
                    ItemEntity itemEntity = target.entityDropItem(drop, 1.0F);
                    itemEntity.setMotion(itemEntity.getMotion().add(
                            (rand.nextFloat() - rand.nextFloat()) * 0.1F,
                            rand.nextFloat() * 0.05F,
                            (rand.nextFloat() - rand.nextFloat()) * 0.1F));
                });
                stack.damageItem(1, (LivingEntity) target, e -> e.sendBreakAnimation(EquipmentSlotType.MAINHAND));
            }
            this.shearable.remove(0);
            this.timeToIgnore = 0;
            return;
        }
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = 10;
            this.owner.getNavigator().tryMoveToXYZ(target.getPosX(), target.getPosY(), target.getPosZ(), 1);
        }
    }

    @Override
    public void resetTask() {
        this.timeToIgnore = 0;
        this.timeToRecalcPath = 0;
        this.shearable.clear();
    }

    @Override
    public void endModeTask() {

    }

    @Override
    public String getName() {
        return "Ripper";
    }

    static {
        ModeManager.ModeItems items = new ModeManager.ModeItems();
        items.add(ShearsItem.class);
        ModeManager.INSTANCE.register(RipperMode.class, items);
    }
}
