package com.sistr.littlemaidrebirth.entity;

import net.minecraftforge.common.util.FakePlayer;

public interface IHasFakePlayer {

    FakePlayer getFakePlayer();

    void syncToFakePlayer();

    void syncToOrigin();

}
