package net.tigereye.chestcavity.compat.guzhenren.module;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.linkage.effect.LinkageEffectContext;
import net.tigereye.chestcavity.listeners.OrganHealListener;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnFireListener;
import net.tigereye.chestcavity.listeners.OrganOnGroundListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Declarative wiring details for a single Guzhenren organ integration.
 */
public final class OrganIntegrationSpec {

    private final ResourceLocation organId;
    private final List<OrganSlowTickListener> slowTickListeners;
    private final List<OrganOnHitListener> onHitListeners;
    private final List<OrganIncomingDamageListener> incomingDamageListeners;
    private final List<OrganOnFireListener> onFireListeners;
    private final List<OrganOnGroundListener> onGroundListeners;
    private final List<OrganHealListener> healListeners;
    private final List<OrganRemovalListener> removalListeners;
    private final List<Consumer<ChestCavityInstance>> ensureAttachedHooks;
    private final List<OnEquipHook> onEquipHooks;

    private OrganIntegrationSpec(Builder builder) {
        this.organId = builder.organId;
        this.slowTickListeners = List.copyOf(builder.slowTickListeners);
        this.onHitListeners = List.copyOf(builder.onHitListeners);
        this.incomingDamageListeners = List.copyOf(builder.incomingDamageListeners);
        this.onFireListeners = List.copyOf(builder.onFireListeners);
        this.onGroundListeners = List.copyOf(builder.onGroundListeners);
        this.healListeners = List.copyOf(builder.healListeners);
        this.removalListeners = List.copyOf(builder.removalListeners);
        this.ensureAttachedHooks = List.copyOf(builder.ensureAttachedHooks);
        this.onEquipHooks = List.copyOf(builder.onEquipHooks);
    }

    public ResourceLocation organId() {
        return organId;
    }

    public List<OrganSlowTickListener> slowTickListeners() {
        return slowTickListeners;
    }

    public List<OrganOnHitListener> onHitListeners() {
        return onHitListeners;
    }

    public List<OrganIncomingDamageListener> incomingDamageListeners() {
        return incomingDamageListeners;
    }

    public List<OrganOnFireListener> onFireListeners() {
        return onFireListeners;
    }

    public List<OrganOnGroundListener> onGroundListeners() {
        return onGroundListeners;
    }

    public List<OrganHealListener> healListeners() {
        return healListeners;
    }

    public List<OrganRemovalListener> removalListeners() {
        return removalListeners;
    }

    public List<Consumer<ChestCavityInstance>> ensureAttachedHooks() {
        return ensureAttachedHooks;
    }

    public List<OnEquipHook> onEquipHooks() {
        return onEquipHooks;
    }

    /** Applies all declared operations to the provided linkage context. */
    public void apply(LinkageEffectContext context) {
        Objects.requireNonNull(context, "context");
        slowTickListeners.forEach(context::addSlowTickListener);
        onHitListeners.forEach(context::addOnHitListener);
        incomingDamageListeners.forEach(context::addIncomingDamageListener);
        onFireListeners.forEach(context::addOnFireListener);
        onGroundListeners.forEach(context::addOnGroundListener);
        healListeners.forEach(context::addHealListener);
        removalListeners.forEach(context::addRemovalListener);

        ChestCavityInstance chestCavity = context.chestCavity();
        if (chestCavity != null) {
            ensureAttachedHooks.forEach(hook -> hook.accept(chestCavity));
            if (!onEquipHooks.isEmpty()) {
                List<OrganRemovalContext> staleRemovalContexts = context.staleRemovalContexts();
                ItemStack sourceOrgan = context.sourceOrgan();
                for (OnEquipHook hook : onEquipHooks) {
                    hook.onEquip(chestCavity, sourceOrgan, staleRemovalContexts);
                }
            }
        }
    }

    public static Builder builder(ResourceLocation organId) {
        return new Builder(organId);
    }

    /** Fluent builder for {@link OrganIntegrationSpec}. */
    public static final class Builder {
        private final ResourceLocation organId;
        private final List<OrganSlowTickListener> slowTickListeners = new ArrayList<>();
        private final List<OrganOnHitListener> onHitListeners = new ArrayList<>();
        private final List<OrganIncomingDamageListener> incomingDamageListeners = new ArrayList<>();
        private final List<OrganOnFireListener> onFireListeners = new ArrayList<>();
        private final List<OrganOnGroundListener> onGroundListeners = new ArrayList<>();
        private final List<OrganHealListener> healListeners = new ArrayList<>();
        private final List<OrganRemovalListener> removalListeners = new ArrayList<>();
        private final List<Consumer<ChestCavityInstance>> ensureAttachedHooks = new ArrayList<>();
        private final List<OnEquipHook> onEquipHooks = new ArrayList<>();

        private Builder(ResourceLocation organId) {
            this.organId = Objects.requireNonNull(organId, "organId");
        }

        public Builder addSlowTickListener(OrganSlowTickListener listener) {
            slowTickListeners.add(Objects.requireNonNull(listener, "listener"));
            return this;
        }

        public Builder addOnHitListener(OrganOnHitListener listener) {
            onHitListeners.add(Objects.requireNonNull(listener, "listener"));
            return this;
        }

        public Builder addIncomingDamageListener(OrganIncomingDamageListener listener) {
            incomingDamageListeners.add(Objects.requireNonNull(listener, "listener"));
            return this;
        }

        public Builder addOnFireListener(OrganOnFireListener listener) {
            onFireListeners.add(Objects.requireNonNull(listener, "listener"));
            return this;
        }

        public Builder addOnGroundListener(OrganOnGroundListener listener) {
            onGroundListeners.add(Objects.requireNonNull(listener, "listener"));
            return this;
        }

        public Builder addHealListener(OrganHealListener listener) {
            healListeners.add(Objects.requireNonNull(listener, "listener"));
            return this;
        }

        public Builder addRemovalListener(OrganRemovalListener listener) {
            removalListeners.add(Objects.requireNonNull(listener, "listener"));
            return this;
        }

        public Builder ensureAttached(Consumer<ChestCavityInstance> hook) {
            ensureAttachedHooks.add(Objects.requireNonNull(hook, "hook"));
            return this;
        }

        public Builder onEquip(OnEquipHook hook) {
            onEquipHooks.add(Objects.requireNonNull(hook, "hook"));
            return this;
        }

        public OrganIntegrationSpec build() {
            return new OrganIntegrationSpec(this);
        }
    }

    @FunctionalInterface
    public interface OnEquipHook {
        void onEquip(
                ChestCavityInstance chestCavity,
                ItemStack sourceOrgan,
                List<OrganRemovalContext> staleRemovalContexts
        );
    }
}
