package net.tigereye.chestcavity.guscript.runtime.flow.actions;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowController;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowEdgeAction;
import net.tigereye.chestcavity.guscript.runtime.flow.fx.GeckoFxAnchor;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;

/**
 * Built-in flow actions used by the MVP implementation.
 */
public final class FlowActions {

    private FlowActions() {
    }

    private static final ResourceLocation BREAK_AIR_SOUND_ID = ResourceLocation.parse("chestcavity:custom.sword.break_air");

    public static void overrideResourceOpenerForTests(Function<Player, Optional<GuzhenrenResourceBridge.ResourceHandle>> opener) {
        ResourceFlowActions.overrideResourceOpenerForTests(opener);
    }

    public static void resetResourceOpenerForTests() {
        ResourceFlowActions.resetResourceOpenerForTests();
    }

    public static FlowEdgeAction consumeResource(String identifier, double amount) {
        return ResourceFlowActions.consumeResource(identifier, amount);
    }

    public static FlowEdgeAction setCooldown(String key, long durationTicks) {
        return ResourceFlowActions.setCooldown(key, durationTicks);
    }

    public static FlowEdgeAction runActions(List<Action> actions) {
        return ResourceFlowActions.runActions(actions);
    }

    public static FlowEdgeAction consumeHealth(double amount) {
        return ResourceFlowActions.consumeHealth(amount);
    }

    public static FlowEdgeAction applyEffect(ResourceLocation effectId, int duration, int amplifier, boolean showParticles, boolean showIcon) {
        return ResourceFlowActions.applyEffect(effectId, duration, amplifier, showParticles, showIcon);
    }

    public static FlowEdgeAction trueDamage(double amount) {
        return ResourceFlowActions.trueDamage(amount);
    }

    public static FlowEdgeAction tameNearby(double radius, boolean sit, boolean persist) {
        return AllyFlowActions.tameNearby(radius, sit, persist);
    }

    public static FlowEdgeAction orderGuard(double radius, boolean seekHostiles, double acquireRadius) {
        return AllyFlowActions.orderGuard(radius, seekHostiles, acquireRadius);
    }

    public static FlowEdgeAction bindOwnerNudao(double radius, boolean alsoTameIfPossible) {
        return AllyFlowActions.bindOwnerNudao(radius, alsoTameIfPossible);
    }

    public static FlowEdgeAction assistPlayerAttacks(double allyRadius, int recentTicks, double acquireRadius) {
        return AllyFlowActions.assistPlayerAttacks(allyRadius, recentTicks, acquireRadius);
    }

    public static FlowEdgeAction setInvisibleNearby(double radius, boolean invisible, boolean alliesOnly) {
        return AllyFlowActions.setInvisibleNearby(radius, invisible, alliesOnly);
    }

    public static FlowEdgeAction entityStrike(
            double allyRadius,
            Vec3 relativeOffset,
            float yawOffset,
            double dashDistance,
            String targetSelector,
            ResourceLocation soundId,
            String entityIdVariable
    ) {
        return EntityStrikeFlowActions.entityStrike(allyRadius, relativeOffset, yawOffset, dashDistance, targetSelector, soundId, entityIdVariable);
    }

    public static FlowEdgeAction emitFxOnAllies(ResourceLocation fxId, double allyRadius, float intensity) {
        return FxFlowActions.emitFxOnAllies(fxId, allyRadius, intensity);
    }

    public static FlowEdgeAction emitGeckoOnAllies(ResourceLocation fxId, double allyRadius, Vec3 offset, float scale, int tint, float alpha, boolean loop, int duration) {
        return FxFlowActions.emitGeckoOnAllies(fxId, allyRadius, offset, scale, tint, alpha, loop, duration);
    }

    public static FlowEdgeAction emitFx(String fxId, float baseIntensity, String variableName, double defaultScale) {
        return FxFlowActions.emitFx(fxId, baseIntensity, variableName, defaultScale);
    }

    public static FlowEdgeAction emitGecko(GeckoFxParameters parameters) {
        return FxFlowActions.emitGecko(parameters);
    }

    public static FlowEdgeAction playSound(ResourceLocation soundId, SoundAnchor anchor, Vec3 offset, float volume, float pitch, int delayTicks) {
        return SoundFlowActions.playSound(soundId, anchor, offset, volume, pitch, delayTicks);
    }

    public static FlowEdgeAction playSound(ResourceLocation soundId, SoundAnchor anchor, Vec3 offset, float volume, float pitch) {
        return playSound(soundId, anchor, offset, volume, pitch, 0);
    }

    public static FlowEdgeAction playBreakAirSound(int delayTicks, float volume, float pitch) {
        return SoundFlowActions.playSound(BREAK_AIR_SOUND_ID, SoundAnchor.PERFORMER, Vec3.ZERO, volume, pitch, delayTicks);
    }

    public static FlowEdgeAction playBreakAirSound(int delayTicks) {
        return playBreakAirSound(delayTicks, 1.0F, 1.0F);
    }

    public static FlowEdgeAction playBreakAirSound() {
        return playBreakAirSound(0, 1.0F, 1.0F);
    }

    public static FlowEdgeAction explode(float power) {
        return CombatFlowActions.explode(power);
    }

    public static FlowEdgeAction applyAttributeModifier(ResourceLocation attributeId, ResourceLocation modifierKey, AttributeModifier.Operation operation, double amount) {
        return AttributeFlowActions.applyAttributeModifier(attributeId, modifierKey, operation, amount);
    }

    public static FlowEdgeAction removeAttributeModifier(ResourceLocation attributeId, ResourceLocation modifierKey) {
        return AttributeFlowActions.removeAttributeModifier(attributeId, modifierKey);
    }

    public static FlowEdgeAction areaEffect(ResourceLocation effectId, int duration, int amplifier, double radius, String radiusVariable, boolean hostilesOnly, boolean includeSelf, boolean showParticles, boolean showIcon) {
        return CombatFlowActions.areaEffect(effectId, duration, amplifier, radius, radiusVariable, hostilesOnly, includeSelf, showParticles, showIcon);
    }

    public static FlowEdgeAction dampenProjectiles(double radius, String radiusVariable, double factor, int capPerTick) {
        return CombatFlowActions.dampenProjectiles(radius, radiusVariable, factor, capPerTick);
    }

    public static FlowEdgeAction highlightHostiles(double radius, String radiusVariable, int durationTicks) {
        return CombatFlowActions.highlightHostiles(radius, radiusVariable, durationTicks);
    }

    public static FlowEdgeAction setVariable(String name, double value, boolean asDouble) {
        return VariableFlowActions.setVariable(name, value, asDouble);
    }

    public static FlowEdgeAction addVariable(String name, double delta, boolean asDouble) {
        return VariableFlowActions.addVariable(name, delta, asDouble);
    }

    public static FlowEdgeAction addVariableFromVariable(String targetName, String sourceName, double scale, boolean asDouble) {
        return VariableFlowActions.addVariableFromVariable(targetName, sourceName, scale, asDouble);
    }

    public static FlowEdgeAction clampVariable(String name, double min, double max, boolean asDouble) {
        return VariableFlowActions.clampVariable(name, min, max, asDouble);
    }

    public static FlowEdgeAction setVariableFromParam(String paramKey, String variableName, double defaultValue) {
        return VariableFlowActions.setVariableFromParam(paramKey, variableName, defaultValue);
    }

    public static FlowEdgeAction copyVariable(String sourceName, String targetName, double scale, double offset, boolean asDouble) {
        return VariableFlowActions.copyVariable(sourceName, targetName, scale, offset, asDouble);
    }

    public static FlowEdgeAction replaceBlocksSphere(
            double baseRadius,
            String radiusParam,
            String radiusVariable,
            double maxHardness,
            boolean includeFluids,
            boolean dropBlocks,
            List<ResourceLocation> replacementIds,
            List<Integer> replacementWeights,
            List<ResourceLocation> forbiddenIds,
            boolean placeSnowLayers,
            int snowLayersMin,
            int snowLayersMax,
            String originKey
    ) {
        return BlockFlowActions.replaceBlocksSphere(
                baseRadius,
                radiusParam,
                radiusVariable,
                maxHardness,
                includeFluids,
                dropBlocks,
                replacementIds,
                replacementWeights,
                forbiddenIds,
                placeSnowLayers,
                snowLayersMin,
                snowLayersMax,
                originKey
        );
    }

    public static FlowEdgeAction slashArea(double radius, double damage, double breakPower) {
        return SlashFlowActions.slashArea(radius, damage, breakPower);
    }

    public static FlowEdgeAction slashDelayedRay(int delayTicks, double length, double damage, double breakPower, double rayRadius) {
        return SlashFlowActions.slashDelayedRay(delayTicks, length, damage, breakPower, rayRadius);
    }

    public static Vec3 rotateOffsetByYaw(Vec3 offset, float yawDegrees) {
        return EntityStrikeFlowActions.rotateOffsetByYaw(offset, yawDegrees);
    }

    public static Vec3 computeEntityStrikePosition(
            Vec3 performerPosition,
            float performerYaw,
            Vec3 offset,
            float yawOffset,
            double dashDistance
    ) {
        return EntityStrikeFlowActions.computeEntityStrikePosition(performerPosition, performerYaw, offset, yawOffset, dashDistance);
    }

    public static FlowEdgeAction emitGeckoRelative(GeckoFxParameters parameters) {
        return emitGecko(parameters);
    }

    static Vec3 rotateRelativeOffset(Vec3 relativeOffset, float yawDegrees, float pitchDegrees, float rollDegrees) {
        return FxFlowActions.rotateRelativeOffset(relativeOffset, yawDegrees, pitchDegrees, rollDegrees);
    }

    static UUID computeFxEventId(ResourceLocation fxId, Entity anchor, boolean loop) {
        return FxFlowActions.computeFxEventId(fxId, anchor, loop);
    }

    public enum SoundAnchor {
        PERFORMER,
        TARGET;

        public static SoundAnchor fromString(String raw) {
            if (raw == null || raw.isBlank()) {
                return PERFORMER;
            }
            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "target", "flow_target" -> TARGET;
                default -> PERFORMER;
            };
        }
    }

    public record GeckoFxParameters(
            ResourceLocation fxId,
            GeckoFxAnchor anchor,
            Vec3 offset,
            Vec3 relativeOffset,
            Vec3 worldPosition,
            Float yaw,
            Float pitch,
            Float roll,
            float scale,
            int tint,
            float alpha,
            boolean loop,
            int durationTicks,
            String entityIdVariable
    ) {}
}
