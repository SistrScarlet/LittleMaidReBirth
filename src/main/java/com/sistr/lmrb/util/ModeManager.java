package com.sistr.lmrb.util;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sistr.lmrb.entity.mode.IMode;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tags.Tag;
import net.minecraft.util.ResourceLocation;

import java.util.Map;
import java.util.Set;

public class ModeManager {

    public static ModeManager INSTANCE = new ModeManager();

    public final Map<Class<?>, ModeItems> MODES = Maps.newHashMap();

    public void register(Class<?> mode, ModeItems items) {
        MODES.put(mode, items);
    }

    public boolean containModeItem(IMode mode, ItemStack stack) {
        ModeItems modeItems = MODES.get(mode.getClass());
        if (modeItems == null) return false;
        return modeItems.contains(stack);
    }

    //除外判定はまた今度(必要性薄そうだし…)
    public static class ModeItems {
        public final Set<String> itemNames = Sets.newHashSet();
        public final Set<String> excludeItemNames = Sets.newHashSet();
        public final Set<Item> items = Sets.newHashSet();
        public final Set<Item> excludeItems = Sets.newHashSet();
        public final Set<Class<?>> itemClasses = Sets.newHashSet();
        public final Set<Class<?>> excludeItemClasses = Sets.newHashSet();
        public final Set<Tag<Item>> itemTags = Sets.newHashSet();
        public final Set<Tag<Item>> excludeItemTags = Sets.newHashSet();

        public ModeItems add(String name) {
            itemNames.add(name);
            return this;
        }

        public ModeItems add(ResourceLocation name) {
            itemNames.add(name.toString());
            return this;
        }

        public ModeItems add(ItemStack stack) {
            items.add(stack.getItem());
            return this;
        }

        public ModeItems add(Class<?> itemClass) {
            itemClasses.add(itemClass);
            return this;
        }

        public ModeItems add(Tag<Item> itemTag) {
            itemTags.add(itemTag);
            return this;
        }

        public boolean contains(ItemStack stack) {
            Item item = stack.getItem();
            if (items.contains(item)) {
                return true;
            }
            String itemName = item.getRegistryName().toString();
            if (itemNames.contains(itemName)) {
                return true;
            }
            for (Tag<Item> itemTag : itemTags) {
                if (itemTag.contains(item)) {
                    return true;
                }
            }
            Class<?> itemClass = item.getClass();
            if (itemClasses.contains(itemClass)) {
                return true;
            }
            return isContainsSuperLoop(itemClass);
        }

        private boolean isContainsSuperLoop(Class<?> itemClass) {
            Class<?> superItem = itemClass.getSuperclass();
            if (superItem == null) return false;
            if (itemClasses.contains(superItem)) {
                return true;
            }
            return isContainsSuperLoop(superItem);
        }

    }

}
