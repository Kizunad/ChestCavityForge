package net.tigereye.chestcavity.soul.fakeplayer.brain;

/** Top-level coordinator that orchestrates one or more sub-brains. */
public interface Brain {
  String id();

  BrainMode mode();

  void onEnter(BrainContext ctx);

  void onExit(BrainContext ctx);

  void tick(BrainContext ctx);
}
