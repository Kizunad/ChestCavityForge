package net.tigereye.chestcavity.compat.guzhenren.ability.blood_bone_bomb;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.tigereye.chestcavity.registration.CCEntities;

/**
 * Client-only bindings for Blood Bone Bomb visuals.
 */
public final class BloodBoneBombClient {

    private BloodBoneBombClient() {
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CCEntities.BONE_GUN_PROJECTILE.get(),
                context -> new ThrownItemRenderer<>(context, 1.0f, true));
    }
}
