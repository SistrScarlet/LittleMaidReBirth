package net.sistr.littlemaidrebirth.entity.mode;

import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.minecraft.nbt.CompoundNBT;

import java.util.Optional;

public interface ModeSupplier {

    Optional<Mode> getMode();

    void writeModeData(CompoundNBT tag);

    void readModeData(CompoundNBT tag);

}
