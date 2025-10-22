package net.tigereye.chestcavity.soul.fakeplayer.service;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import org.jetbrains.annotations.Nullable;

/** 默认的内存实现：基于 ConcurrentHashMap 管理身份缓存。 */
public final class DefaultSoulIdentityService implements SoulIdentityService {

  private static final long ID_MASK_MSB = 0x5A5A5A5A5A5A5A5AL;
  private static final long ID_MASK_LSB = 0xA5A5A5A5A5A5A5A5L;

  private final Map<UUID, GameProfile> identities = new ConcurrentHashMap<>();

  private static UUID deriveEntityUuid(UUID profileId) {
    return new UUID(
        profileId.getMostSignificantBits() ^ ID_MASK_MSB,
        profileId.getLeastSignificantBits() ^ ID_MASK_LSB);
  }

  private static String normalizeName(UUID soulId, String candidate) {
    String baseName = candidate;
    if (baseName == null || baseName.isBlank()) {
      String hash = soulId.toString().replace("-", "");
      baseName = "Soul" + hash.substring(0, Math.min(10, hash.length()));
    }
    return baseName.length() > 16 ? baseName.substring(0, 16) : baseName;
  }

  private static void copyProfileProperties(GameProfile from, GameProfile to) {
    var toProps = to.getProperties();
    toProps.clear();
    for (Map.Entry<String, Property> entry : from.getProperties().entries()) {
      toProps.put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public GameProfile ensureIdentity(
      UUID soulId, GameProfile sourceProfile, boolean forceDerivedId) {
    if (soulId == null) {
      throw new IllegalArgumentException("soulId cannot be null");
    }
    if (sourceProfile == null) {
      throw new IllegalArgumentException("sourceProfile cannot be null");
    }
    return identities.compute(
        soulId,
        (id, existing) -> {
          String desiredName = normalizeName(id, sourceProfile.getName());
          if (existing == null) {
            UUID entityId = forceDerivedId ? deriveEntityUuid(id) : sourceProfile.getId();
            if (entityId == null) {
              entityId = deriveEntityUuid(id);
            }
            if (!forceDerivedId && entityId.equals(id)) {
              entityId = deriveEntityUuid(id);
            }
            GameProfile identity = new GameProfile(entityId, desiredName);
            copyProfileProperties(sourceProfile, identity);
            return identity;
          }
          GameProfile updated = new GameProfile(existing.getId(), desiredName);
          copyProfileProperties(sourceProfile, updated);
          return updated;
        });
  }

  @Override
  public void updateIdentityName(UUID soulId, String newName) {
    identities.compute(
        soulId,
        (id, existing) -> {
          if (existing == null) {
            return null;
          }
          String trimmed = newName == null ? "" : newName.trim();
          String name = trimmed.length() > 16 ? trimmed.substring(0, 16) : trimmed;
          GameProfile updated = new GameProfile(existing.getId(), name);
          copyProfileProperties(existing, updated);
          return updated;
        });
  }

  @Override
  public void seedIdentityName(UUID soulId, String newName) {
    if (soulId == null || newName == null) {
      return;
    }
    String trimmed = newName.trim();
    if (trimmed.isEmpty()) {
      return;
    }
    String name = trimmed.length() > 16 ? trimmed.substring(0, 16) : trimmed;
    identities.compute(
        soulId,
        (id, existing) -> {
          if (existing == null) {
            UUID entityId = deriveEntityUuid(id);
            return new GameProfile(entityId, name);
          }
          GameProfile updated = new GameProfile(existing.getId(), name);
          copyProfileProperties(existing, updated);
          return updated;
        });
  }

  @Override
  public boolean isIdentityNameInUse(String candidate) {
    if (candidate == null) {
      return false;
    }
    String target = candidate.toLowerCase(Locale.ROOT);
    for (GameProfile profile : identities.values()) {
      if (profile != null
          && profile.getName() != null
          && profile.getName().toLowerCase(Locale.ROOT).equals(target)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean hasIdentity(UUID soulId) {
    return soulId != null && identities.containsKey(soulId);
  }

  @Override
  public GameProfile getIdentityOrDefault(UUID soulId, GameProfile fallback) {
    GameProfile profile = identities.get(soulId);
    return profile != null ? profile : fallback;
  }

  @Override
  public @Nullable GameProfile getIdentity(UUID soulId) {
    return identities.get(soulId);
  }

  @Override
  public void putIdentity(UUID soulId, @Nullable GameProfile profile) {
    if (soulId == null) {
      return;
    }
    if (profile == null) {
      identities.remove(soulId);
    } else {
      GameProfile copy = cloneProfile(profile);
      identities.put(soulId, copy);
    }
  }

  @Override
  public void removeIdentity(UUID soulId) {
    if (soulId != null) {
      identities.remove(soulId);
    }
  }

  @Override
  public void forEachIdentity(BiConsumer<UUID, GameProfile> consumer) {
    if (consumer == null) {
      return;
    }
    identities.forEach(
        (id, profile) -> {
          if (profile != null) {
            consumer.accept(id, profile);
          }
        });
  }

  @Override
  public void clear() {
    identities.clear();
  }

  @Override
  public GameProfile cloneProfile(GameProfile source) {
    if (source == null) {
      throw new IllegalArgumentException("source cannot be null");
    }
    GameProfile clone = new GameProfile(source.getId(), source.getName());
    copyProfileProperties(source, clone);
    return clone;
  }

  @Override
  public void copyProperties(GameProfile from, GameProfile to) {
    if (from == null || to == null) {
      return;
    }
    copyProfileProperties(from, to);
  }
}
