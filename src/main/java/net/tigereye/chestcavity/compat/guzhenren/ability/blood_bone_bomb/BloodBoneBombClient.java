package net.tigereye.chestcavity.compat.guzhenren.ability.blood_bone_bomb;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * @deprecated Use {@link net.tigereye.chestcavity.guscript.ability.guzhenren.blood_bone_bomb.BloodBoneBombClient} instead.
 */
@Deprecated(forRemoval = false)
public final class BloodBoneBombClient {

    private BloodBoneBombClient() {
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        net.tigereye.chestcavity.guscript.ability.guzhenren.blood_bone_bomb.BloodBoneBombClient.onRegisterRenderers(event);
    }
}
