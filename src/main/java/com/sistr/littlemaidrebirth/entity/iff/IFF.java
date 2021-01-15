package com.sistr.littlemaidrebirth.entity.iff;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;

public class IFF {
    protected IFFTag iffTag;
    protected IFFType iffType;
    protected EntityType<?> entityType;

    public IFF(IFFTag iffTag, IFFType iffType, EntityType<?> entityType) {
        this.iffTag = iffTag;
        this.iffType = iffType;
        this.entityType = entityType;
    }

    public boolean identify(LivingEntity entity) {
        return entity.getType() == entityType;
    }

    public CompoundNBT writeTag() {
        CompoundNBT tag = new CompoundNBT();
        tag.putInt("IFFTag", iffTag.getId());
        tag.putString("IFFType", IFFTypeManager.getINSTANCE().getId(iffType)
                .orElseThrow(() -> new RuntimeException("存在しないIFFTypeです。")).toString());
        tag.putString("EntityType", EntityType.getKey(entityType).toString());
        return tag;
    }

    public IFF readTag(CompoundNBT tag) {
        iffTag = IFFTag.getTagFromId(tag.getInt("IFFTag"));
        iffType = IFFTypeManager.getINSTANCE().getIFFType(new ResourceLocation(tag.getString("IFFType")))
                .orElseThrow(() -> new RuntimeException("存在しないIFFTypeです。"));
        entityType = EntityType.byKey(tag.getString("EntityType"))
                .orElseThrow(() -> new RuntimeException("存在しないEntityTypeです。"));
        return this;
    }

    public IFFTag getIFFTag() {
        return iffTag;
    }

    public IFFType getIFFType() {
        return iffType;
    }

    public IFF setTag(IFFTag iffTag) {
        this.iffTag = iffTag;
        return this;
    }
}
