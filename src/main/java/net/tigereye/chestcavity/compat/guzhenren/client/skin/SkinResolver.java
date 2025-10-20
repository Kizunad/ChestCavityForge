package net.tigereye.chestcavity.compat.guzhenren.client.skin;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-only skin resolver that registers Minecraft skin textures (and optional overlays) for arbitrary profiles.
 */
@OnlyIn(Dist.CLIENT)
public final class SkinResolver {

    private static final Map<String, ResourceLocation> BASE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> HASH_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<ResourceLocation>> INFLIGHT = new ConcurrentHashMap<>();
    private static final ResourceLocation FALLBACK = ResourceLocation.parse("minecraft:textures/entity/steve.png");

    private SkinResolver() {
    }

    public static SkinLayers resolve(SkinHandle handle) {
        ResourceLocation base = resolveBase(handle);
        ResourceLocation overlay = handle.overlayFallback();
        return new SkinLayers(base, overlay);
    }

    private static ResourceLocation resolveBase(SkinHandle handle) {
        ResourceLocation fallback = Objects.requireNonNullElse(handle.fallback(), FALLBACK);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null) {
            return fallback;
        }
        String key = handle.cacheKey();
        ResourceLocation cached = BASE_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        String url = handle.skinUrl();
        String hash = extractHash(url);
        if (hash != null) {
            ResourceLocation hashCached = HASH_CACHE.get(hash);
            if (hashCached != null) {
                BASE_CACHE.put(key, hashCached);
                return hashCached;
            }
            CompletableFuture<ResourceLocation> download = INFLIGHT.computeIfAbsent(hash, h -> downloadSkinAsync(url, h));
            if (download.isDone()) {
                ResourceLocation location = download.join();
                INFLIGHT.remove(hash);
                if (location != null) {
                    HASH_CACHE.put(hash, location);
                    BASE_CACHE.put(key, location);
                    return location;
                }
            }
        }
        return fallback;
    }

    /** Describes resolved texture layers. Currently only base layer is utilized by callers. */
    public record SkinLayers(ResourceLocation base, ResourceLocation overlay) {
        public ResourceLocation primary() {
            return base;
        }
    }

    private static CompletableFuture<ResourceLocation> downloadSkinAsync(String url, String hash) {
        return CompletableFuture.supplyAsync(() -> downloadNativeImage(url))
                .thenCompose(image -> registerTextureAsync(image, hash));
    }

    private static CompletableFuture<ResourceLocation> registerTextureAsync(NativeImage image, String hash) {
        if (image == null) {
            return CompletableFuture.completedFuture(null);
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            image.close();
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<ResourceLocation> completableFuture = new CompletableFuture<>();
        minecraft.execute(() -> {
            try {
                TextureManager textureManager = minecraft.getTextureManager();
                ResourceLocation location = ResourceLocation.fromNamespaceAndPath("guzhenren", "dynamic_skins/" + hash);
                textureManager.register(location, new DynamicTexture(image));
                completableFuture.complete(location);
            } catch (Exception ex) {
                image.close();
                completableFuture.complete(null);
            }
        });
        return completableFuture;
    }

    private static NativeImage downloadNativeImage(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();
            if (connection.getResponseCode() / 100 != 2) {
                connection.disconnect();
                return null;
            }
            try (InputStream stream = connection.getInputStream()) {
                return NativeImage.read(stream);
            } finally {
                connection.disconnect();
            }
        } catch (IOException ignored) {
            return null;
        }
    }

    private static String extractHash(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        int slash = url.lastIndexOf('/');
        if (slash >= 0 && slash < url.length() - 1) {
            return url.substring(slash + 1);
        }
        return null;
    }
}
