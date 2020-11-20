package com.sistr.littlemaidrebirth.entity.iff;

enum Type {
    ENEMY(true, true),
    NEUTRAL(false, true),
    FRIENDLY(false, false);

    private final boolean isEnemy;
    private final boolean isTargetable;

    Type(boolean isEnemy, boolean isTargetable) {
        this.isEnemy = isEnemy;
        this.isTargetable = isTargetable;
    }

    public boolean isEnemy() {
        return isEnemy;
    }

    public boolean isTargetable() {
        return isTargetable;
    }

}
