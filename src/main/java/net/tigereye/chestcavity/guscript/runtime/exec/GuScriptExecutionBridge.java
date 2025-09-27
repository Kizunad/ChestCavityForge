package net.tigereye.chestcavity.guscript.runtime.exec;

/**
 * Bridge exposed to action execution for resource manipulation and effect dispatch.
 * Concrete implementations should wire into ChestCavity/Guzhenren systems.
 */
public interface GuScriptExecutionBridge {

    void consumeZhenyuan(int amount);

    void consumeHealth(int amount);

    void emitProjectile(String projectileId, double damage);
}
