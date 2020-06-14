package com.sistr.littlemaidrebirth.entity.mode;

//todo read/write アイテム交換
//Goal互換
public interface IMode {

    //Goal互換 排他判定用文字列
    String MOVE = "move";
    String LOOK = "look";
    String JUMP = "jump";
    String TARGET = "target";

    //モード開始時(切り替わった時)に一度だけ処理
    void startModeTask();

    //Goal互換 発動時、処理を開始すべきか
    boolean shouldExecute();

    //Goal互換 発動時、処理を続行すべきか
    boolean shouldContinueExecuting();

    //Goal互換 発動時、最初の一回だけ処理
    void startExecuting();

    //Goal互換 発動時、毎tick処理
    void tick();

    //Goal互換 発動時、処理終了時に一回だけ処理
    void resetTask();

    //モード終了時(切り替わった時)に一回だけ処理
    void endModeTask();

    //モード判別用
    String getName();

}
