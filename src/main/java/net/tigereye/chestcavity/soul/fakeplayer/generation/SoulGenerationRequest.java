package net.tigereye.chestcavity.soul.fakeplayer.generation;

import com.mojang.authlib.GameProfile;
import java.util.Objects;
import java.util.UUID;
import net.tigereye.chestcavity.soul.fakeplayer.brain.BrainMode;
import net.tigereye.chestcavity.soul.fakeplayer.brain.intent.BrainIntent;
import org.jetbrains.annotations.Nullable;

/** 新建/复原分魂壳体时的参数快照，集中承载身份、原因以及初始 AI 模式。 */
public record SoulGenerationRequest(
    UUID soulId,
    @Nullable GameProfile identity,
    boolean forceDerivedIdentity,
    @Nullable String reason,
    @Nullable BrainMode initialMode,
    @Nullable BrainIntent initialIntent) {

  public SoulGenerationRequest {
    Objects.requireNonNull(soulId, "soulId");
  }

  public static SoulGenerationRequest create(UUID soulId) {
    return new SoulGenerationRequest(soulId, null, false, null, null, null);
  }

  public SoulGenerationRequest withIdentity(
      @Nullable GameProfile identity, boolean forceDerivedIdentity) {
    return new SoulGenerationRequest(
        soulId, identity, forceDerivedIdentity, reason, initialMode, initialIntent);
  }

  public SoulGenerationRequest withReason(@Nullable String reason) {
    return new SoulGenerationRequest(
        soulId, identity, forceDerivedIdentity, reason, initialMode, initialIntent);
  }

  public SoulGenerationRequest withBrain(@Nullable BrainMode mode, @Nullable BrainIntent intent) {
    return new SoulGenerationRequest(soulId, identity, forceDerivedIdentity, reason, mode, intent);
  }
}
