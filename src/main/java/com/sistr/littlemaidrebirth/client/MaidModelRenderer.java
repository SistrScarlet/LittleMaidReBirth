package com.sistr.littlemaidrebirth.client;

import com.sistr.littlemaidrebirth.entity.ITameable;
import com.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.blacklab.lmr.entity.maidmodel.IModelCaps;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.sistr.lmml.client.renderer.MultiModelRenderer;

public class MaidModelRenderer extends MultiModelRenderer<LittleMaidEntity> {

    public MaidModelRenderer(EntityRendererManager rendererManager) {
        super(rendererManager);
    }

    @Override
    public void setModelValues(LittleMaidEntity entity, double x, double y, double z, float yaw, float partialTicks, IModelCaps caps) {
        super.setModelValues(entity, x, y, z, yaw, partialTicks, caps);
        modelMain.setCapsValue(IModelCaps.caps_aimedBow, entity.getAimingBow());
        modelMain.setCapsValue(IModelCaps.caps_isWait, entity.getMovingState().equals(ITameable.WAIT));
    }
}
