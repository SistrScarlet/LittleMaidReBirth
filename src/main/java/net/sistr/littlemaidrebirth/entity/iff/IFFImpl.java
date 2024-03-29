package net.sistr.littlemaidrebirth.entity.iff;

import com.google.common.collect.Lists;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;

import java.util.List;
import java.util.Optional;

public class IFFImpl implements HasIFF {
    private final List<IFF> iffs = Lists.newArrayList();

    public IFFImpl(List<IFF> iffs) {
        this.iffs.addAll(iffs);
    }

    @Override
    public IFFTag identify(LivingEntity target) {
        return this.iffs.stream()
                .filter(iff -> iff.identify(target))
                .map(IFF::getIFFTag)
                .findFirst()
                .orElse(IFFTag.UNKNOWN);
    }

    @Override
    public void setIFFs(List<IFF> iffs) {
        this.iffs.clear();
        this.iffs.addAll(iffs);
    }

    @Override
    public List<IFF> getIFFs() {
        return Lists.newArrayList(iffs);
    }

    @Override
    public void writeIFF(CompoundNBT tag) {
        ListNBT list = new ListNBT();
        tag.put("HasIFFTags", list);
        iffs.stream().map(IFF::writeTag).forEach(list::add);
    }

    @Override
    public void readIFF(CompoundNBT tag) {
        if (!tag.contains("HasIFFTags")) {
            return;
        }
        ListNBT list = tag.getList("HasIFFTags", 10);
        list.stream()
                .map(t -> (CompoundNBT) t)
                .map(t -> IFFTypeManager.getINSTANCE().loadIFF(t))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(this::addOrReplace);

    }

    public void addOrReplace(IFF iff) {
        iffs.removeIf(i -> i.getIFFType() == iff.getIFFType());
        iffs.add(iff);
    }
}
