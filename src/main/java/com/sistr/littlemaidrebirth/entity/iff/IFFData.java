package com.sistr.littlemaidrebirth.entity.iff;

import com.google.common.collect.Maps;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;

import java.util.Map;

public class IFFData {
    private final Map<EntityType<?>, Type> types = Maps.newHashMap();
    private final Map<Identify, Type> identifies = Maps.newHashMap();

    //個別識別が優先される。属性識別による判定が被った場合、すべてがisTargetableを返せばtrueとなる
    public boolean isEnemy(Entity entity) {
        if (types.containsKey(entity.getType())) {
            return types.get(entity.getType()).isEnemy();
        }
        boolean result;
        for (Map.Entry<Identify, Type> entry : identifies.entrySet()) {
            if (entry.getKey().identify(entity)) {
                result = entry.getValue().isEnemy();
                if (!result) {
                    return false;
                }
            }
        }
        return false;
    }

    //個別識別が優先される。属性識別による判定が被った場合、いずれか一つがisTargetableを返せばtrueとなる
    public boolean isTargetable(Entity entity) {
        if (types.containsKey(entity.getType())) {
            return types.get(entity.getType()).isTargetable();
        }
        boolean result;
        for (Map.Entry<Identify, Type> entry : identifies.entrySet()) {
            if (entry.getKey().identify(entity)) {
                result = entry.getValue().isTargetable();
                if (result) {
                    return true;
                }
            }
        }
        return true;
    }

    public void setType(Entity entity, Type type) {
        types.put(entity.getType(), type);
    }

    public void resetType(Entity entity) {
        types.remove(entity.getType());
    }

    public void setDefaultType(Identify identify, Type type) {
        identifies.put(identify, type);
    }

    public void resetDefaultType(Identify identify) {
        identifies.remove(identify);
    }

}
