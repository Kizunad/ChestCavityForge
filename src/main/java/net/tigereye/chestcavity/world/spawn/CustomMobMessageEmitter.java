package net.tigereye.chestcavity.world.spawn;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * 生物附近玩家信息广播器。
 */
public final class CustomMobMessageEmitter {

    private final Function<Mob, Component> messageFactory;
    private final double radius;
    private final int intervalTicks;
    private final boolean sendOnSpawn;
    private final boolean perPlayerCooldown;
    private final int perPlayerCooldownTicks;

    private CustomMobMessageEmitter(Builder builder) {
        this.messageFactory = builder.messageFactory;
        this.radius = builder.radius;
        this.intervalTicks = builder.intervalTicks;
        this.sendOnSpawn = builder.sendOnSpawn;
        this.perPlayerCooldown = builder.perPlayerCooldown;
        this.perPlayerCooldownTicks = builder.perPlayerCooldownTicks;
    }

    public Function<Mob, Component> messageFactory() {
        return messageFactory;
    }

    public double radius() {
        return radius;
    }

    public int intervalTicks() {
        return intervalTicks;
    }

    public boolean sendOnSpawn() {
        return sendOnSpawn;
    }

    public boolean perPlayerCooldown() {
        return perPlayerCooldown;
    }

    public int perPlayerCooldownTicks() {
        return perPlayerCooldownTicks;
    }

    Component createMessage(Mob mob) {
        return messageFactory.apply(mob);
    }

    void broadcast(ServerLevel level, Mob mob) {
        Component message = createMessage(mob);
        if (message == null) {
            return;
        }
        double radiusSq = radius * radius;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isRemoved()) {
                continue;
            }
            if (player.distanceToSqr(mob) <= radiusSq) {
                player.sendSystemMessage(message);
            }
        }
    }

    public static Builder builder(Function<Mob, Component> messageFactory) {
        return new Builder(messageFactory);
    }

    public static final class Builder {
        private final Function<Mob, Component> messageFactory;
        private double radius = 16.0D;
        private int intervalTicks = 100;
        private boolean sendOnSpawn = true;
        private boolean perPlayerCooldown = true;
        private int perPlayerCooldownTicks = 200;

        private Builder(Function<Mob, Component> messageFactory) {
            this.messageFactory = Objects.requireNonNull(messageFactory, "messageFactory");
        }

        public Builder withRadius(double radius) {
            this.radius = Math.max(1.0D, radius);
            return this;
        }

        public Builder withIntervalTicks(int ticks) {
            this.intervalTicks = Math.max(1, ticks);
            return this;
        }

        public Builder sendOnSpawn(boolean sendOnSpawn) {
            this.sendOnSpawn = sendOnSpawn;
            return this;
        }

        public Builder withPerPlayerCooldown(boolean perPlayerCooldown, int cooldownTicks) {
            this.perPlayerCooldown = perPlayerCooldown;
            this.perPlayerCooldownTicks = Math.max(1, cooldownTicks);
            return this;
        }

        public CustomMobMessageEmitter build() {
            return new CustomMobMessageEmitter(this);
        }
    }

    static final class RuntimeState {
        private final CustomMobMessageEmitter emitter;
        private final Map<UUID, Integer> perPlayerCooldowns = new HashMap<>();
        private int cooldown;

        RuntimeState(CustomMobMessageEmitter emitter) {
            this.emitter = emitter;
            this.cooldown = emitter.intervalTicks();
        }

        boolean tick(ServerLevel level, Mob mob) {
            if (mob.isRemoved() || mob.isDeadOrDying()) {
                return false;
            }
            Iterator<Map.Entry<UUID, Integer>> it = perPlayerCooldowns.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Integer> entry = it.next();
                int remaining = entry.getValue() - 1;
                if (remaining <= 0) {
                    it.remove();
                } else {
                    entry.setValue(remaining);
                }
            }
            if (--cooldown > 0) {
                return true;
            }
            cooldown = emitter.intervalTicks();
            Component message = emitter.createMessage(mob);
            if (message == null) {
                return true;
            }
            double radiusSq = emitter.radius() * emitter.radius();
            for (ServerPlayer player : level.players()) {
                if (player.isSpectator() || player.isRemoved()) {
                    continue;
                }
                if (player.distanceToSqr(mob) > radiusSq) {
                    continue;
                }
                if (emitter.perPlayerCooldown()) {
                    UUID id = player.getUUID();
                    int remaining = perPlayerCooldowns.getOrDefault(id, 0);
                    if (remaining > 0) {
                        continue;
                    }
                    perPlayerCooldowns.put(id, emitter.perPlayerCooldownTicks());
                }
                player.sendSystemMessage(message);
            }
            return true;
        }

        void broadcastOnSpawn(ServerLevel level, Mob mob) {
            if (!emitter.sendOnSpawn()) {
                return;
            }
            emitter.broadcast(level, mob);
            cooldown = emitter.intervalTicks();
        }
    }
}
