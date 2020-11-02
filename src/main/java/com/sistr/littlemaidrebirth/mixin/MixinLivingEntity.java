package com.sistr.littlemaidrebirth.mixin;

import com.sistr.littlemaidrebirth.util.LivingAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SEntityEquipmentPacket;
import net.minecraft.util.DamageSource;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity implements LivingAccessor {

    public MixinLivingEntity(EntityType<?> entityTypeIn, World worldIn) {
        super(entityTypeIn, worldIn);
    }

    @Shadow protected abstract void updateActiveHand();

    @Shadow protected abstract boolean canBlockDamageSource(DamageSource damageSourceIn);

    @Shadow @Final private NonNullList<ItemStack> handInventory;

    @Shadow @Final private NonNullList<ItemStack> armorArray;

    @Shadow public abstract ItemStack getItemStackFromSlot(EquipmentSlotType slotIn);

    @Shadow public abstract AbstractAttributeMap getAttributes();

    //1.15ではメソッドとして分離していないため、コピペで対応
    @Override
    public void applyEquipmentAttributes_LM() {
        for(EquipmentSlotType equipmentslottype : EquipmentSlotType.values()) {
            ItemStack itemstack;
            switch(equipmentslottype.getSlotType()) {
                case HAND:
                    itemstack = this.handInventory.get(equipmentslottype.getIndex());
                    break;
                case ARMOR:
                    itemstack = this.armorArray.get(equipmentslottype.getIndex());
                    break;
                default:
                    continue;
            }

            ItemStack itemstack1 = this.getItemStackFromSlot(equipmentslottype);
            if (!ItemStack.areItemStacksEqual(itemstack1, itemstack)) {
                if (!itemstack1.equals(itemstack, true))
                    ((ServerWorld)this.world).getChunkProvider().sendToAllTracking(this,
                            new SEntityEquipmentPacket(this.getEntityId(), equipmentslottype, itemstack1));
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                        new net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent(
                                (LivingEntity) this.getEntity(), equipmentslottype, itemstack, itemstack1));
                if (!itemstack.isEmpty()) {
                    this.getAttributes().removeAttributeModifiers(itemstack.getAttributeModifiers(equipmentslottype));
                }

                if (!itemstack1.isEmpty()) {
                    this.getAttributes().applyAttributeModifiers(itemstack1.getAttributeModifiers(equipmentslottype));
                }

                switch(equipmentslottype.getSlotType()) {
                    case HAND:
                        this.handInventory.set(equipmentslottype.getIndex(), itemstack1.copy());
                        break;
                    case ARMOR:
                        this.armorArray.set(equipmentslottype.getIndex(), itemstack1.copy());
                }
            }
        }
    }

    @Override
    public void tickActiveItemStack_LM() {
        updateActiveHand();
    }

    @Override
    public boolean blockedByShield_LM(DamageSource source) {
        return canBlockDamageSource(source);
    }
}
