package net.tigereye.chestcavity.soul.fakeplayer.brain;

/** High-level behavior modes for SoulPlayer. */
public enum BrainMode {
    AUTO,      // Pick sub-brain based on context/orders
    COMBAT,    // Force combat sub-brain
    SURVIVAL,  // Heal/avoid risks (placeholder)
    IDLE       // Do nothing
}

