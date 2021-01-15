package com.sistr.littlemaidrebirth.entity.mode;

import com.google.common.collect.Lists;
import com.sistr.littlemaidrebirth.entity.FakePlayerSupplier;
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
import net.minecraftforge.common.IForgeShearable;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class RipperMode implements Mode {
    protected final CreatureEntity mob;
    protected final FakePlayerSupplier fakePlayer;
    protected final int radius;
    protected final List<Entity> shearable = Lists.newArrayList();
    protected int timeToRecalcPath;
    protected int timeToIgnore;

    public RipperMode(CreatureEntity mob, FakePlayerSupplier fakePlayer, int radius) {
        this.mob = mob;
        this.fakePlayer = fakePlayer;
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
                this.mob.getPosX() + radius,
                this.mob.getPosY() + radius / 4F,
                this.mob.getPosZ() + radius,
                this.mob.getPosX() - radius,
                this.mob.getPosY() - radius / 4F,
                this.mob.getPosZ() - radius);
        return this.mob.world.getEntitiesInAABBexcluding(this.mob, bb, (entity) ->
                entity instanceof LivingEntity && entity instanceof IForgeShearable
                        && ((IForgeShearable) entity).isShearable(this.mob.getHeldItemMainhand(), this.mob.world, entity.getPosition())
                        && this.mob.getEntitySenses().canSee(entity));
    }

    @Override
    public boolean shouldContinueExecuting() {
        return !this.shearable.isEmpty();
    }

    @Override
    public void startExecuting() {
        this.mob.getNavigator().clearPath();
        List<Entity> tempList = this.shearable.stream()
                .sorted(Comparator.comparingDouble(entity -> entity.getDistanceSq(this.mob)))
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
        if (target.getDistanceSq(this.mob) < 2 * 2) {
            shearing(target);
            this.shearable.remove(0);
            this.timeToIgnore = 0;
            return;
        }
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = 10;
            this.mob.getNavigator().tryMoveToXYZ(target.getPosX(), target.getPosY(), target.getPosZ(), 1);
        }
    }
    
    public void shearing(Entity target) {
        ItemStack stack = this.mob.getHeldItemMainhand();
        BlockPos pos = new BlockPos(target.getPosX(), target.getPosY(), target.getPosZ());
        if (((IForgeShearable) target).isShearable(stack, target.world, pos)) {
            List<ItemStack> drops = ((IForgeShearable) target).onSheared(
                    fakePlayer.getFakePlayer(), stack, target.world, pos,
                    EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, stack));
            Random rand = new java.util.Random();
            drops.forEach(drop -> {
                ItemEntity ent = target.entityDropItem(drop, 1.0F);
                assert ent != null;
                ent.setMotion(ent.getMotion().add(
                        (rand.nextFloat() - rand.nextFloat()) * 0.1F,
                        rand.nextFloat() * 0.05F,
                        (rand.nextFloat() - rand.nextFloat()) * 0.1F));
            });
            stack.damageItem(1, (LivingEntity) target,
                    entity -> entity.sendBreakAnimation(EquipmentSlotType.MAINHAND));
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
