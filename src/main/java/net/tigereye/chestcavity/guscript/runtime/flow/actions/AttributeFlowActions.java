package net.tigereye.chestcavity.guscript.runtime.flow.actions;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowController;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowEdgeAction;

/**
 * 属性相关的 Flow Action。
 */
final class AttributeFlowActions {

    private AttributeFlowActions() {
    }

    static FlowEdgeAction applyAttributeModifier(ResourceLocation attributeId, ResourceLocation modifierKey, AttributeModifier.Operation operation, double amount) {
        double sanitized = amount;
        if (attributeId == null || modifierKey == null || operation == null) {
            return FlowActionUtils.describe(() -> "apply_attribute(nop)");
        }
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null) {
                    return;
                }
                Holder.Reference<net.minecraft.world.entity.ai.attributes.Attribute> attribute = BuiltInRegistries.ATTRIBUTE.getHolder(attributeId).orElse(null);
                if (attribute == null) {
                    ChestCavity.LOGGER.warn("[Flow] Unknown attribute {} in apply_attribute", attributeId);
                    return;
                }
                AttributeInstance instance = performer.getAttribute(attribute);
                if (instance == null) {
                    return;
                }
                instance.removeModifier(modifierKey);
                AttributeModifier modifier = new AttributeModifier(modifierKey, sanitized, operation);
                instance.addTransientModifier(modifier);
            }

            @Override
            public String describe() {
                return "apply_attribute(" + attributeId + ")";
            }
        };
    }

    static FlowEdgeAction removeAttributeModifier(ResourceLocation attributeId, ResourceLocation modifierKey) {
        if (attributeId == null || modifierKey == null) {
            return FlowActionUtils.describe(() -> "remove_attribute(nop)");
        }
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null) {
                    return;
                }
                Holder.Reference<net.minecraft.world.entity.ai.attributes.Attribute> attribute = BuiltInRegistries.ATTRIBUTE.getHolder(attributeId).orElse(null);
                if (attribute == null) {
                    return;
                }
                AttributeInstance instance = performer.getAttribute(attribute);
                if (instance == null) {
                    return;
                }
                instance.removeModifier(modifierKey);
            }

            @Override
            public String describe() {
                return "remove_attribute(" + attributeId + ")";
            }
        };
    }
}
