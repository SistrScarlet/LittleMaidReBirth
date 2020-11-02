package com.sistr.littlemaidrebirth.entity;

import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.controller.MovementController;
import net.minecraft.pathfinding.NodeProcessor;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;

public class FixedMoveControl extends MovementController {

    public FixedMoveControl(MobEntity mob) {
        super(mob);
    }

    @Override
    public void setMoveTo(double x, double y, double z, double speed) {
        super.setMoveTo(x, y, z, speed);
        this.moveForward = 0;
        this.moveStrafe = 0;
        this.mob.setMoveForward(0);
        this.mob.setMoveStrafing(0);
    }

    @Override
    public void tick() {
        if (this.action == Action.STRAFE) {
            float attrSpeed = (float) this.mob.getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getValue();
            float speed = (float) this.speed * attrSpeed;
            checkStrifeToPos(speed);

            this.mob.setAIMoveSpeed(speed);
            this.mob.setMoveForward(this.moveForward);
            this.mob.setMoveStrafing(this.moveStrafe);
            this.action = Action.WAIT;
            return;
        } else if (this.action == Action.WAIT) {
            this.moveForward = 0;
            this.moveStrafe = 0;
            this.mob.setMoveForward(0);
            this.mob.setMoveStrafing(0);
            return;
        }
        super.tick();
    }

    protected void checkStrifeToPos(float speed) {
        BlockPos strifeToPos = getStrifeToPos(speed, this.moveForward, this.moveStrafe);
        if (!this.canWalkable(strifeToPos)) {
            this.moveForward = 0;
            this.moveStrafe = 0;
            //大体の場合、strifeToPos == entityPos
            BlockPos entityPos = mob.getPosition();
            if (!strifeToPos.equals(entityPos) && this.canWalkable(entityPos)) {
                Vec2f strife = getPosToStrife(entityPos.getX() + 0.5F, entityPos.getZ() + 0.5F);
                this.moveForward = strife.x;
                this.moveStrafe = strife.y;
            } else {
                BlockPos.Mutable checkPos = new BlockPos.Mutable();
                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        checkPos.setPos(mob.getPosX() + i, mob.getPosY(), mob.getPosZ() + j);
                        if (canWalkable(checkPos)) {
                            Vec2f strife = getPosToStrife(checkPos.getX() + 0.5F, checkPos.getZ() + 0.5F);
                            this.moveForward = strife.x;
                            this.moveStrafe = strife.y;
                        }
                    }
                }
            }
        }
    }

    protected BlockPos getStrifeToPos(float speed, float forward, float sideways) {
        float moveAmount = MathHelper.sqrt(forward * forward + sideways * sideways);
        if (moveAmount < 1.0F) {
            moveAmount = 1.0F;
        }

        moveAmount = speed / moveAmount;
        forward *= moveAmount;
        sideways *= moveAmount;
        float sinYaw = MathHelper.sin(this.mob.rotationYaw * (float) (Math.PI / 180D));
        float cosYaw = MathHelper.cos(this.mob.rotationYaw * (float) (Math.PI / 180D));
        float moveX = forward * cosYaw - sideways * sinYaw;
        float moveZ = sideways * cosYaw + forward * sinYaw;
        return new BlockPos(
                MathHelper.floor(this.mob.getPosX() + moveX),
                MathHelper.floor(this.mob.getPosY()),
                MathHelper.floor(this.mob.getPosZ() + moveZ));
    }

    protected Vec2f getPosToStrife(float x, float z) {
        float moveX = x - (float) this.mob.getPosX();
        float moveZ = z - (float) this.mob.getPosZ();
        float moveYaw = (float)(MathHelper.atan2(moveX, moveZ) * (180D / Math.PI));
        //エンティティの向いている方向を0度として、移動したい方向を調整する
        moveYaw -= mob.rotationYaw;
        float sideways = -MathHelper.sin(moveYaw * (float) (Math.PI / 180D));
        float forward = -MathHelper.cos(moveYaw * (float) (Math.PI / 180D));
        return new Vec2f(forward, sideways);
    }

    protected boolean canWalkable(BlockPos pos) {
        return canWalkable(pos.getX(), pos.getY(), pos.getZ());
    }

    protected boolean canWalkable(int x, int y, int z) {
        PathNavigator nav = this.mob.getNavigator();
        NodeProcessor nodeProcessor = nav.getNodeProcessor();
        return nodeProcessor.getPathNodeType(this.mob.world, x, y, z) == PathNodeType.WALKABLE;
    }

}
