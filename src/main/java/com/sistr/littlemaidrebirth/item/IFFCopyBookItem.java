package com.sistr.littlemaidrebirth.item;

import com.sistr.littlemaidrebirth.entity.iff.HasIFF;
import com.sistr.littlemaidrebirth.entity.iff.IFFTypeManager;
import com.sistr.littlemaidrebirth.setup.ModSetup;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class IFFCopyBookItem extends Item {

    public IFFCopyBookItem() {
        super(new Item.Properties()
                .group(ModSetup.ITEM_GROUP)
                .maxStackSize(1));
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        CompoundNBT tag = stack.getTag();
        if (tag != null && tag.contains("IFFs")) {
            tooltip.add(new TranslationTextComponent("item.littlemaidrebirth.iff_copy_book.tooltip"));
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity user, Hand hand) {
        if (world.isRemote) {
            return super.onItemRightClick(world, user, hand);
        }
        ItemStack stack = user.getHeldItem(hand);
        Vector3d start = user.getEyePosition(1F);
        Vector3d end = start.add(user.getLookVec().scale(4D));
        BlockRayTraceResult bResult = world.rayTraceBlocks(new RayTraceContext(
                start, end, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, user));
        if (bResult.getType() != RayTraceResult.Type.MISS) {
            end = bResult.getHitVec();
        }
        AxisAlignedBB box = new AxisAlignedBB(start, end).grow(1);
        EntityRayTraceResult eResult = ProjectileHelper.rayTraceEntities(world, user, start, end, box,
                entity -> entity instanceof HasIFF);
        if (eResult == null || eResult.getType() == RayTraceResult.Type.MISS)
            return super.onItemRightClick(world, user, hand);

        Entity target = eResult.getEntity();
        if (user.isSneaking()) {
            ListNBT list = new ListNBT();
            ((HasIFF) target).getIFFs().forEach(iff -> list.add(iff.writeTag()));
            CompoundNBT tag = stack.getOrCreateTag();
            tag.put("IFFs", list);
            user.sendStatusMessage(new TranslationTextComponent("item.littlemaidrebirth.iff_copy_book.message_written"), true);
        } else {
            CompoundNBT tag = stack.getOrCreateTag();
            if (!tag.contains("IFFs")) {
                return super.onItemRightClick(world, user, hand);
            }
            ListNBT list = tag.getList("IFFs", 10);
            ((HasIFF) target).setIFFs(list.stream()
                    .map(t -> (CompoundNBT) t)
                    .map(t -> IFFTypeManager.getINSTANCE().loadIFF(t))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList()));
            user.sendStatusMessage(new TranslationTextComponent("item.littlemaidrebirth.iff_copy_book.message_apply"), true);
        }
        user.world.playSound(null, user.getPosX(), user.getPosY(), user.getPosZ(),
                SoundEvents.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 1F, 1F);
        return ActionResult.resultSuccess(stack);
    }

    //こちらだとタグがイマイチうまく保存できない
    /*@Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (!(entity instanceof HasIFF)) {
            return super.useOnEntity(stack, user, entity, hand);
        }
        if (user.world.isClient) {
            return ActionResult.success(true);
        }
        if (user.isSneaking()) {
            ListNBT list = new ListNBT();
            ((HasIFF) entity).getIFFs().forEach(iff -> list.add(iff.writeTag()));
            CompoundNBT tag = stack.getOrCreateTag();
            tag.put("IFFs", list);
        } else {
            CompoundNBT tag = stack.getOrCreateTag();
            if (!tag.contains("IFFs")) {
                return super.useOnEntity(stack, user, entity, hand);
            }
            ListNBT list = tag.getList("IFFs", 10);
            ((HasIFF) entity).setIFFs(list.stream()
                    .map(t -> (CompoundNBT) t)
                    .map(t -> IFFTypeManager.getINSTANCE().loadIFF(t))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList()));
        }
        return ActionResult.success(false);
    }*/


}
