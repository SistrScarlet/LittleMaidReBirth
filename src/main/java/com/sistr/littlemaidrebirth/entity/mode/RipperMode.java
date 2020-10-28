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
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.IShearable;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class RipperMode implements Mode {
    protected final CreatureEntity owner;
    protected final int radius;
    protected final List<Entity> shearable = Lists.newArrayList();
    protected int timeToRecalcPath;
    protected int timeToIgnore;

    public RipperMode(CreatureEntity owner, int radius) {
        this.owner = owner;
        this.radius = radius;
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
        AxisAlignedBB bb = new AxisAlignedBB(
                this.owner.getPosX() + radius,
                this.owner.getPosY() + radius / 4,
                this.owner.getPosZ() + radius,
                this.owner.getPosX() - radius,
                this.owner.getPosY() - radius / 4,
                this.owner.getPosZ() - radius);
        return this.owner.world.getEntitiesInAABBexcluding(this.owner, bb, (entity) ->
                entity instanceof LivingEntity && entity instanceof IShearable
                        && ((IShearable) entity).isShearable(this.owner.getHeldItemMainhand(), this.owner.world, entity.getPosition())
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
        if (!(target instanceof LivingEntity) || !(target instanceof IShearable)) {
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
            if (((IShearable) target).isShearable(stack, target.world, pos)) {
                List<ItemStack> drops = ((IShearable) target).onSheared(stack, target.world, pos,
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
    public void writeModeData(CompoundNBT tag) {

    }

    @Override
    public void readModeData(CompoundNBT tag) {

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
