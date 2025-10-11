package net.tigereye.chestcavity.soul.fakeplayer.actions.api;

public enum ActionPriority {
    EMERGENCY(100),
    HIGH(75),
    NORMAL(50),
    LOW(25),
    PASSIVE(10);

    public final int weight;
    ActionPriority(int weight) { this.weight = weight; }
}

