package net.sistr.littlemaidrebirth.entity.goal;

import net.sistr.littlemaidrebirth.entity.HasGuiEntitySupplier;
import net.sistr.littlemaidrebirth.entity.Tameable;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;

import java.util.EnumSet;

public class WaitWhenOpenGUIGoal<T extends CreatureEntity, M extends Container & HasGuiEntitySupplier<T>> extends Goal {
    private final T mob;
    private final Tameable tameable;
    private final Class<? extends M> screenHandler;

    public WaitWhenOpenGUIGoal(T mob, Tameable tameable, Class<? extends M> screenHandler) {
        this.mob = mob;
        this.tameable = tameable;
        this.screenHandler = screenHandler;
        setMutexFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean shouldExecute() {
        return tameable.getTameOwner()
                .filter(owner -> owner instanceof PlayerEntity)
                .map(owner -> ((PlayerEntity) owner).openContainer)
                .filter(screen -> this.screenHandler.isAssignableFrom(screen.getClass()))
                .map(screen -> screenHandler.cast(screen).getGuiEntity())
                .filter(guiEntity -> mob == guiEntity)
                .isPresent();
    }



    @Override
    public boolean shouldContinueExecuting() {
        return tameable.getTameOwner()
                .filter(owner -> owner instanceof PlayerEntity)
                .map(owner -> ((PlayerEntity) owner).openContainer)
                .filter(screen -> this.screenHandler.isAssignableFrom(screen.getClass()))
                .isPresent();
    }

    @Override
    public void startExecuting() {
        this.mob.getNavigator().clearPath();
    }

    @Override
    public void tick() {
        super.tick();
        tameable.getTameOwner().ifPresent(owner ->
                this.mob.getLookController().setLookPosition(owner.getEyePosition(1F)));
    }
}
