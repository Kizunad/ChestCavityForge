package net.tigereye.chestcavity.soul.fakeplayer.brain.personality;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import net.minecraft.resources.ResourceLocation;

/**
 * 描述一组可调整的脑参数集合。
 */
public final class SoulPersonality {

    private final ResourceLocation id;
    private final Map<ResourceLocation, Double> doubleParams;
    private final Map<ResourceLocation, Integer> intParams;

    private SoulPersonality(ResourceLocation id,
                            Map<ResourceLocation, Double> doubleParams,
                            Map<ResourceLocation, Integer> intParams) {
        this.id = Objects.requireNonNull(id, "id");
        this.doubleParams = Collections.unmodifiableMap(new LinkedHashMap<>(doubleParams));
        this.intParams = Collections.unmodifiableMap(new LinkedHashMap<>(intParams));
    }

    public ResourceLocation id() {
        return id;
    }

    public double getDouble(ResourceLocation key, double fallback) {
        Double value = doubleParams.get(key);
        return value != null ? value : fallback;
    }

    public int getInt(ResourceLocation key, int fallback) {
        Integer value = intParams.get(key);
        return value != null ? value : fallback;
    }

    public Map<ResourceLocation, Double> doubles() {
        return doubleParams;
    }

    public Map<ResourceLocation, Integer> ints() {
        return intParams;
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final Map<ResourceLocation, Double> doubles = new LinkedHashMap<>();
        private final Map<ResourceLocation, Integer> ints = new LinkedHashMap<>();

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder setDouble(ResourceLocation key, double value) {
            doubles.put(Objects.requireNonNull(key, "key"), value);
            return this;
        }

        public Builder setInt(ResourceLocation key, int value) {
            ints.put(Objects.requireNonNull(key, "key"), value);
            return this;
        }

        public SoulPersonality build() {
            return new SoulPersonality(id, doubles, ints);
        }
    }

    @Override
    public String toString() {
        return "SoulPersonality{" +
                "id=" + id +
                ", doubles=" + doubleParams +
                ", ints=" + intParams +
                '}';
    }
}
