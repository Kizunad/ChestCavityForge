package net.tigereye.chestcavity.guscript.runtime.flow.actions;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowController;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowEdgeAction;
import net.tigereye.chestcavity.guscript.runtime.flow.fx.GeckoFxAnchor;
import net.tigereye.chestcavity.guscript.fx.FxEventParameters;
import net.tigereye.chestcavity.guscript.runtime.action.DefaultGuScriptExecutionBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;

/**
 * Built-in flow actions used by the MVP implementation.
 */
public final class FlowActions {

    private FlowActions() {
    }

    private static final ResourceLocation BREAK_AIR_SOUND_ID = ResourceLocation.parse("chestcavity:custom.sword.break_air");
    private static final ResourceLocation FAIL_TO_SOULBEAST_SOUND_ID = ChestCavity.id("custom.soulbeast.fail_to_soulbeast");
    private static final ResourceLocation FAIL_TO_SOULBEAST_FX_ID = ChestCavity.id("soulbeast_fail");
    private static final FlowEdgeAction FAIL_TO_SOULBEAST_SOUND_ACTION = SoundFlowActions.playSound(
            FAIL_TO_SOULBEAST_SOUND_ID,
            SoundAnchor.PERFORMER,
            Vec3.ZERO,
            1.0F,
            1.0F,
            0
    );

    public static void overrideResourceOpenerForTests(Function<Player, Optional<GuzhenrenResourceBridge.ResourceHandle>> opener) {
        ResourceFlowActions.overrideResourceOpenerForTests(opener);
    }

    public static void resetResourceOpenerForTests() {
        ResourceFlowActions.resetResourceOpenerForTests();
    }

    public static FlowEdgeAction consumeResource(String identifier, double amount) {
        return ResourceFlowActions.consumeResource(identifier, amount);
    }

    public static FlowEdgeAction consumeResourcesCombo(double zhenyuan, double jingli, double healthCost, String failureReason) {
        return ResourceFlowActions.consumeResourcesCombo(zhenyuan, jingli, healthCost, failureReason);
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

    public static FlowEdgeAction scoreboardSet(String objective, int value, String playerName) {
        return ScoreboardFlowActions.set(objective, value, playerName);
    }

    public static FlowEdgeAction emitFx(String fxId, float baseIntensity, String variableName, double defaultScale) {
        return FxFlowActions.emitFx(fxId, baseIntensity, variableName, defaultScale);
    }

    public static FlowEdgeAction emitFxConditional(ResourceLocation fxId, String variableName, double skipValue, float intensity) {
        return FxFlowActions.emitFxConditional(fxId, variableName, skipValue, intensity);
    }

    public static FlowEdgeAction emitGecko(GeckoFxParameters parameters) {
        return FxFlowActions.emitGecko(parameters);
    }

    public static FlowEdgeAction emitFailFx() {
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                FAIL_TO_SOULBEAST_SOUND_ACTION.apply(performer, target, controller, gameTime);
                if (performer == null) {
                    return;
                }
                DefaultGuScriptExecutionBridge bridge = new DefaultGuScriptExecutionBridge(performer, target, 0);
                bridge.playFx(FAIL_TO_SOULBEAST_FX_ID, FxEventParameters.DEFAULT);
            }

            @Override
            public String describe() {
                return "emit_fail_fx(sound=" + FAIL_TO_SOULBEAST_SOUND_ID + ", fx=" + FAIL_TO_SOULBEAST_FX_ID + ")";
            }
        };
    }

    public static FlowEdgeAction playSound(ResourceLocation soundId, SoundAnchor anchor, Vec3 offset, float volume, float pitch, int delayTicks) {
        return SoundFlowActions.playSound(soundId, anchor, offset, volume, pitch, delayTicks);
    }

    public static FlowEdgeAction playSoundConditional(ResourceLocation soundId, SoundAnchor anchor, Vec3 offset, float volume, float pitch, int delayTicks, String variableName, double skipValue) {
        return SoundFlowActions.playSoundConditional(soundId, anchor, offset, volume, pitch, delayTicks, variableName, skipValue);
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

    /**
     * Spawns a new SoulPlayer for the performer with empty inventory and default stats at current position.
     * Optional: set autospawn and immediately switch control to it.
     */
    public static FlowEdgeAction spawnSoulCustom(String name, boolean autospawn, boolean switchTo) {
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (!(performer instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
                    return;
                }
                if (!net.tigereye.chestcavity.soul.engine.SoulFeatureToggle.isEnabled()) {
                    net.tigereye.chestcavity.soul.util.SoulLog.info("[soul][flow] spawnSoulCustom skipped: soul system disabled");
                    return;
                }
                var container = net.tigereye.chestcavity.registration.CCAttachments.getSoulContainer(serverPlayer);
                java.util.UUID soulId = java.util.UUID.randomUUID();
                if (!container.hasProfile(soulId)) {
                    var inv = net.tigereye.chestcavity.soul.profile.InventorySnapshot.empty();
                    var stats = net.tigereye.chestcavity.soul.profile.PlayerStatsSnapshot.empty();
                    var fx = net.tigereye.chestcavity.soul.profile.PlayerEffectsSnapshot.empty();
                    var pos = net.tigereye.chestcavity.soul.profile.PlayerPositionSnapshot.capture(serverPlayer);
                    var profile = net.tigereye.chestcavity.soul.profile.SoulProfile.fromSnapshot(soulId, inv, stats, fx, pos);
                    // 初始化（随机上限）：寿元[29,201]、最大精力=[50,301]、最大魂魄[1,257]、最大念头[17,1023]
                    net.minecraft.util.RandomSource rnd = serverPlayer.getRandom();
                    double shouyuan = 29 + rnd.nextInt(201 - 29 + 1);
                    double maxJingli = 50 + rnd.nextInt(301 - 50 + 1); // 最大精力
                    double maxHunpo = 1 + rnd.nextInt(257 - 1 + 1);
                    double maxNiantou = 17 + rnd.nextInt(1023 - 17 + 1);
                    net.tigereye.chestcavity.soul.init.SoulInitializers.applyGuzhenrenCapsOnly(
                            serverPlayer, profile, shouyuan, maxJingli, maxHunpo, maxNiantou);
                    container.putProfile(soulId, profile);
                    if (autospawn) {
                        container.setAutospawn(serverPlayer, soulId, true, "flow-spawn-autospawn");
                    }
                    net.tigereye.chestcavity.soul.util.SoulProfileOps.markContainerDirty(serverPlayer, container, "flow-spawn");
                }
                // Set custom or random name for identity cache
                String finalName = name;
                if (finalName != null && finalName.equalsIgnoreCase("random")) {
                    finalName = pickUniqueRandomName(serverPlayer);
                }
                if (finalName != null && !finalName.isBlank()) {
                    net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner.seedIdentityName(soulId, finalName);
                }
                var spawned = net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner.respawnForOwner(serverPlayer, soulId);
                if (spawned.isPresent() && switchTo) {
                    net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner.switchTo(serverPlayer, soulId);
                }
                net.tigereye.chestcavity.soul.util.SoulLog.info("[soul][flow] spawnSoulCustom owner={} soul={} name={} switchTo={}",
                        serverPlayer.getUUID(), soulId, finalName, switchTo);
            }

            @Override
            public String describe() {
                return "spawn_soul_custom(name=" + (name == null ? "" : name) + ", autospawn=" + autospawn + ", switchTo=" + switchTo + ")";
            }
        };
    }

    private static String pickUniqueRandomName(net.minecraft.server.level.ServerPlayer owner) {
        net.minecraft.util.RandomSource rand = owner.getRandom();
        // Try several times to find an unused base name; if all are taken, append a short numeric suffix.
        for (int attempt = 0; attempt < 32; attempt++) {
            String base = net.tigereye.chestcavity.soul.util.SoulNamePool.pick(new java.util.Random(rand.nextLong()));
            if (base == null || base.isBlank()) break;
            String candidate = base;
            if (candidate.length() > 16) candidate = candidate.substring(0, 16);
            if (!net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner.isIdentityNameInUse(candidate)) {
                return candidate;
            }
        }
        // Fallback: append a short random number to reduce collision risk
        for (int attempt = 0; attempt < 32; attempt++) {
            String base = net.tigereye.chestcavity.soul.util.SoulNamePool.pick(new java.util.Random(rand.nextLong()));
            if (base == null || base.isBlank()) break;
            String suffix = String.valueOf(10 + rand.nextInt(90));
            String candidate = base + suffix;
            if (candidate.length() > 16) candidate = candidate.substring(0, 16);
            if (!net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner.isIdentityNameInUse(candidate)) {
                return candidate;
            }
        }
        // Last resort: derive from owner name
        String ownerName = owner.getGameProfile().getName();
        if (ownerName == null || ownerName.isBlank()) ownerName = "Soul";
        String candidate = ownerName + "Soul";
        if (candidate.length() > 16) candidate = candidate.substring(0, 16);
        return candidate;
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
