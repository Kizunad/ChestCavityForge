package net.tigereye.chestcavity.client.modernui.config.docs;

import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.client.modernui.config.docs.provider.ActiveSkillDocProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class DocRegistry {

    private static final List<DocProvider> PROVIDERS = new ArrayList<>();
    private static List<DocEntry> CACHE = List.of();
    private static boolean defaultsRegistered = false;

    private DocRegistry() {
    }

    public static synchronized void registerProvider(DocProvider provider) {
        if (provider == null) {
            return;
        }
        boolean exists = PROVIDERS.stream().anyMatch(p -> p.getClass() == provider.getClass());
        if (!exists) {
            PROVIDERS.add(provider);
        }
    }

    public static synchronized void registerDefaultProviders() {
        if (defaultsRegistered) {
            return;
        }
        registerProvider(new ActiveSkillDocProvider());
        defaultsRegistered = true;
    }

    public static synchronized void reload() {
        registerDefaultProviders();
        List<DocEntry> all = new ArrayList<>();
        for (DocProvider provider : PROVIDERS) {
            try {
                Collection<DocEntry> loaded = provider.loadAll();
                if (loaded != null && !loaded.isEmpty()) {
                    all.addAll(loaded);
                }
            } catch (Exception ex) {
                ChestCavity.LOGGER.error("[docs] provider {} failed to load", provider.name(), ex);
            }
        }
        CACHE = List.copyOf(all);
    }

    public static synchronized List<DocEntry> all() {
        if (CACHE.isEmpty()) {
            reload();
        }
        return CACHE;
    }

    public static synchronized List<DocEntry> search(String query) {
        if (query == null || query.isBlank()) {
            return all();
        }
        String needle = query.toLowerCase(Locale.ROOT);
        return all().stream().filter(entry -> matches(entry, needle)).collect(Collectors.toList());
    }

    private static boolean matches(DocEntry entry, String needle) {
        if (entry.title().toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }
        if (entry.summary().toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }
        for (String detail : entry.details()) {
            if (detail.toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        if (entry.id().toString().toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }
        for (String tag : entry.tags()) {
            if (tag.toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        return false;
    }

}
