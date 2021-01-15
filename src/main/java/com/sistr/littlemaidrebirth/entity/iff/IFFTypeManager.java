package com.sistr.littlemaidrebirth.entity.iff;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Sets;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.Set;

//IFFを実装するエンティティクラスごとにIFFTypeManagerを増やしても良い
public class IFFTypeManager {
    private static final IFFTypeManager INSTANCE = new IFFTypeManager();
    private final BiMap<ResourceLocation, IFFType> iffTypes = HashBiMap.create();
    private boolean setup;

    public static IFFTypeManager getINSTANCE() {
        return INSTANCE;
    }

    public void register(ResourceLocation id, IFFType type) {
        iffTypes.put(id, type);
    }

    private void setup(World world) {
        setup = true;
        Set<IFFType> set = Sets.newHashSet(iffTypes.values());
        for (IFFType type : set) {
            if (!type.checkEntity(world))
                iffTypes.remove(iffTypes.inverse().get(type));
        }
    }

    public Set<IFFType> getIFFTypes(World world) {
        if (!setup) setup(world);
        return iffTypes.values();
    }

    public Optional<ResourceLocation> getId(IFFType iffType) {
        return Optional.ofNullable(iffTypes.inverse().get(iffType));
    }

    public Optional<IFFType> getIFFType(ResourceLocation id) {
        return Optional.ofNullable(iffTypes.get(id));
    }

    public Optional<IFF> loadIFF(CompoundNBT tag) {
        return loadIFFType(tag)
                .map(IFFType::createIFF)
                .map(iff -> iff.readTag(tag));
    }

    public Optional<IFFType> loadIFFType(CompoundNBT tag) {
        ResourceLocation id = new ResourceLocation(tag.getString("IFFType"));
        return getIFFType(id);
    }

}
