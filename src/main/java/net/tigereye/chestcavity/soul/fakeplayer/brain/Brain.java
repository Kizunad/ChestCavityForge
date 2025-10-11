package net.tigereye.chestcavity.soul.fakeplayer.brain;

/** Interface for sub-brains that drive a SoulPlayer under a given mode. */
public interface Brain {
    String id();
    BrainMode mode();
    void onEnter(BrainContext ctx);
    void onExit(BrainContext ctx);
    void tick(BrainContext ctx);
}

