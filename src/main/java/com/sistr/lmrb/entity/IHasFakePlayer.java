package com.sistr.lmrb.entity;

import net.minecraftforge.common.util.FakePlayer;

public interface IHasFakePlayer {

    FakePlayer getFakePlayer();

    void syncData();

    void reverseSyncData();

}
