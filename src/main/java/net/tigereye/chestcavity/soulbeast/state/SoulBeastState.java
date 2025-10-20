package net.tigereye.chestcavity.soulbeast.state;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Optional;

/**
 * @deprecated 迁移至 {@link net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastState}
 */
@Deprecated(forRemoval = true)
public final class SoulBeastState {

    private final net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastState delegate;

    public SoulBeastState() {
        this(new net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastState());
    }

    SoulBeastState(net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastState delegate) {
        this.delegate = delegate;
    }

    net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastState delegate() {
        return delegate;
    }

    public boolean isActive() {
        return delegate.isActive();
    }

    boolean setActive(boolean active) {
        return delegate.setActive(active);
    }

    public boolean isPermanent() {
        return delegate.isPermanent();
    }

    boolean setPermanent(boolean permanent) {
        return delegate.setPermanent(permanent);
    }

    public long getLastTick() {
        return delegate.getLastTick();
    }

    void setLastTick(long lastTick) {
        delegate.setLastTick(lastTick);
    }

    public boolean isEnabled() {
        return delegate.isEnabled();
    }

    boolean setEnabled(boolean enabled) {
        return delegate.setEnabled(enabled);
    }

    public long getStartedTick() {
        return delegate.getStartedTick();
    }

    void setStartedTick(long startedTick) {
        delegate.setStartedTick(startedTick);
    }

    public Optional<ResourceLocation> getSource() {
        return delegate.getSource();
    }

    boolean setSource(@Nullable ResourceLocation source) {
        return delegate.setSource(source);
    }

    public CompoundTag save() {
        return delegate.save();
    }

    public void load(CompoundTag tag) {
        delegate.load(tag);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
