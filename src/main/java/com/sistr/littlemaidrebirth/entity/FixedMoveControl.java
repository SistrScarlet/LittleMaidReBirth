package com.sistr.littlemaidrebirth.entity;

import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.controller.MovementController;

public class FixedMoveControl extends MovementController {

    public FixedMoveControl(MobEntity entity) {
        super(entity);
    }

    @Override
    public void setMoveTo(double x, double y, double z, double speedIn) {
        super.setMoveTo(x, y, z, speedIn);
        this.moveStrafe = 0;
        this.mob.setMoveStrafing(moveStrafe);
    }

}
