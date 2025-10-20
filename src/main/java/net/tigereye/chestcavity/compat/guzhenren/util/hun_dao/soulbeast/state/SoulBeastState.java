package net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Mutable state container representing whether an entity is currently a soul beast.
 * <p>
 * This class is intentionally lightweight so it can be attached as an entity attachment
 * and synchronised across the network. All mutation happens through {@link SoulBeastStateManager}.
 */
public final class SoulBeastState {

    private static final String KEY_ACTIVE = "active";
    private static final String KEY_PERMANENT = "permanent";
    private static final String KEY_ENABLED = "enabled"; // server-side gate
    private static final String KEY_LAST_TICK = "last_tick";
    private static final String KEY_STARTED_TICK = "started_tick";
    private static final String KEY_SOURCE = "source";

    private boolean active;
    private boolean permanent;
    private boolean enabled;
    private long lastTick;
    private long startedTick;
    @Nullable
    private ResourceLocation source;

    public SoulBeastState() {
        this.active = false;
        this.permanent = false;
        this.enabled = false;
        this.lastTick = 0L;
        this.startedTick = 0L;
        this.source = null;
    }

    public boolean isActive() {
        return active;
    }

    public boolean setActive(boolean active) {
        if (this.active == active) {
            return false;
        }
        this.active = active;
        return true;
    }

    public boolean isPermanent() {
        return permanent;
    }

    public boolean setPermanent(boolean permanent) {
        if (this.permanent == permanent) {
            return false;
        }
        this.permanent = permanent;
        return true;
    }

    public long getLastTick() {
        return lastTick;
    }

    public void setLastTick(long lastTick) {
        this.lastTick = Math.max(0L, lastTick);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return false;
        }
        this.enabled = enabled;
        return true;
    }

    public long getStartedTick() {
        return startedTick;
    }

    public void setStartedTick(long startedTick) {
        this.startedTick = Math.max(0L, startedTick);
    }

    public Optional<ResourceLocation> getSource() {
        return Optional.ofNullable(source);
    }

    public boolean setSource(@Nullable ResourceLocation source) {
        if (Objects.equals(this.source, source)) {
            return false;
        }
        this.source = source;
        return true;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(KEY_ACTIVE, active);
        tag.putBoolean(KEY_PERMANENT, permanent);
        tag.putBoolean(KEY_ENABLED, enabled);
        tag.putLong(KEY_LAST_TICK, lastTick);
        tag.putLong(KEY_STARTED_TICK, startedTick);
        if (source != null) {
            tag.putString(KEY_SOURCE, source.toString());
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return;
        }
        active = tag.getBoolean(KEY_ACTIVE);
        permanent = tag.getBoolean(KEY_PERMANENT);
        enabled = tag.getBoolean(KEY_ENABLED);
        lastTick = tag.getLong(KEY_LAST_TICK);
        startedTick = tag.getLong(KEY_STARTED_TICK);
        if (tag.contains(KEY_SOURCE)) {
            try {
                source = ResourceLocation.parse(tag.getString(KEY_SOURCE));
            } catch (IllegalArgumentException ex) {
                source = null;
            }
        } else {
            source = null;
        }
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "SoulBeastState{active=%s, permanent=%s, lastTick=%d, source=%s}",
                active, permanent, lastTick, source);
    }
}
