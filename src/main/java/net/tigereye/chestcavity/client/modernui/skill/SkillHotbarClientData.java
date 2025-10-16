package net.tigereye.chestcavity.client.modernui.skill;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.client.input.ModernUIKeyDispatcher;
import net.tigereye.chestcavity.client.modernui.network.SkillHotbarSnapshotPayload;
import net.tigereye.chestcavity.client.modernui.network.SkillHotbarUpdatePayload;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class SkillHotbarClientData {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "modernui_skill_hotbar.json";

    private static final SkillHotbarState STATE = new SkillHotbarState();
    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();
    private static final EnumMap<SkillHotbarKey, Integer> KEY_CODES = new EnumMap<>(SkillHotbarKey.class);

    private static boolean initialized = false;
    private static boolean pendingInitialSync = true;
    private static boolean applySnapshotInProgress = false;
    private static SkillHotbarKey captureTarget = null;
    private static Consumer<Integer> captureCallback = null;
    private static boolean waitingForRelease = false;

    private SkillHotbarClientData() {}

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        KEY_CODES.clear();
        for (SkillHotbarKey key : SkillHotbarKey.values()) {
            KEY_CODES.put(key, key.defaultKeyCode());
        }
        loadFromDisk();
        pendingInitialSync = true;
    }

    public static void tick(Minecraft minecraft) {
        if (pendingInitialSync && minecraft.player != null && minecraft.getConnection() != null) {
            pendingInitialSync = false;
            sendUpdateToServer();
        }
    }

    public static SkillHotbarState state() {
        return STATE;
    }

    public static void addSkill(SkillHotbarKey key, ResourceLocation skillId) {
        if (STATE.bind(key, skillId)) {
            persistAndSync();
        }
    }

    public static void removeSkill(SkillHotbarKey key, ResourceLocation skillId) {
        if (STATE.unbind(key, skillId)) {
            persistAndSync();
        }
    }

    public static void clearKey(SkillHotbarKey key) {
        STATE.clear(key);
        persistAndSync();
    }

    public static void resetToDefault() {
        STATE.reset();
        boolean changed = false;
        for (SkillHotbarKey key : SkillHotbarKey.values()) {
            int def = key.defaultKeyCode();
            if (getKeyCode(key) != def) {
                changed = true;
            }
            KEY_CODES.put(key, def);
        }
        if (changed) {
            ModernUIKeyDispatcher.resetKeyStates();
        }
        persistAndSync();
    }

    public static void applySnapshot(Map<String, List<ResourceLocation>> snapshot, boolean fromServer) {
        applySnapshotInProgress = true;
        try {
            Map<SkillHotbarKey, List<ResourceLocation>> map = new EnumMap<>(SkillHotbarKey.class);
            if (snapshot != null) {
                for (Map.Entry<String, List<ResourceLocation>> entry : snapshot.entrySet()) {
                    SkillHotbarKey key = SkillHotbarKey.fromId(entry.getKey());
                    if (key == null) {
                        continue;
                    }
                    List<ResourceLocation> list = entry.getValue();
                    if (list == null || list.isEmpty()) {
                        continue;
                    }
                    map.put(key, new ArrayList<>(list));
                }
            }
            STATE.replaceAll(map);
            notifyListeners();
            if (fromServer) {
                saveToDisk();
            }
        } finally {
            applySnapshotInProgress = false;
        }
    }

    public static void addListener(Listener listener) {
        LISTENERS.add(listener);
    }

    public static void removeListener(Listener listener) {
        LISTENERS.remove(listener);
    }

    public static int getKeyCode(SkillHotbarKey key) {
        return KEY_CODES.getOrDefault(key, key.defaultKeyCode());
    }

    public static void setKeyCode(SkillHotbarKey key, int newCode) {
        int old = getKeyCode(key);
        if (old == newCode) {
            return;
        }
        KEY_CODES.put(key, newCode);
        ModernUIKeyDispatcher.onKeyRebind(old, newCode);
        persistAndSync();
    }

    public static String describeKey(int keyCode) {
        if (keyCode <= GLFW.GLFW_KEY_UNKNOWN) {
            return "未知键";
        }
        InputConstants.Key key = InputConstants.getKey(keyCode, 0);
        Component name = key.getDisplayName();
        return name == null ? ("键值 " + keyCode) : name.getString();
    }

    public static boolean requestKeyCapture(SkillHotbarKey key, Consumer<Integer> callback) {
        if (captureTarget != null) {
            return false;
        }
        captureTarget = key;
        captureCallback = callback;
        waitingForRelease = true;
        return true;
    }

    public static boolean isCapturing() {
        return captureTarget != null;
    }

    public static boolean processCapture(long windowHandle) {
        if (captureTarget == null) {
            return false;
        }
        int firstPressed = -1;
        boolean anyDown = false;
        for (int code = GLFW.GLFW_KEY_SPACE; code <= GLFW.GLFW_KEY_LAST; code++) {
            if (GLFW.glfwGetKey(windowHandle, code) == GLFW.GLFW_PRESS) {
                anyDown = true;
                firstPressed = code;
                break;
            }
        }
        if (waitingForRelease) {
            if (!anyDown) {
                waitingForRelease = false;
            }
            return false;
        }
        if (!anyDown || firstPressed < 0) {
            return false;
        }
        SkillHotbarKey target = captureTarget;
        Consumer<Integer> callback = captureCallback;
        captureTarget = null;
        captureCallback = null;
        waitingForRelease = true;
        int code = firstPressed;
        if (callback != null) {
            callback.accept(code);
        } else if (target != null) {
            setKeyCode(target, code);
        }
        return true;
    }

    private static void persistAndSync() {
        if (applySnapshotInProgress) {
            return;
        }
        notifyListeners();
        saveToDisk();
        sendUpdateToServer();
    }

    private static void notifyListeners() {
        for (Listener listener : LISTENERS) {
            listener.onSkillHotbarUpdated(STATE);
        }
    }

    private static void saveToDisk() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        Path path = resolveConfigPath(minecraft);
        try {
            Files.createDirectories(path.getParent());
            Map<String, List<String>> serializable = new java.util.LinkedHashMap<>();
            for (SkillHotbarKey key : SkillHotbarKey.values()) {
                List<ResourceLocation> skills = STATE.getSkills(key);
                if (skills.isEmpty()) {
                    continue;
                }
                List<String> ids = new ArrayList<>();
                for (ResourceLocation id : skills) {
                    ids.add(id.toString());
                }
                serializable.put(key.id(), ids);
            }
            JsonObject root = new JsonObject();
            JsonObject skillsObj = new JsonObject();
            for (Map.Entry<String, List<String>> entry : serializable.entrySet()) {
                skillsObj.add(entry.getKey(), GSON.toJsonTree(entry.getValue()));
            }
            JsonObject keysObj = new JsonObject();
            for (SkillHotbarKey key : SkillHotbarKey.values()) {
                keysObj.addProperty(key.id(), getKeyCode(key));
            }
            root.add("skills", skillsObj);
            root.add("keys", keysObj);
            Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            ChestCavity.LOGGER.warn("[modernui][skill-hotbar] Failed to save client bindings", e);
        }
    }

    private static void loadFromDisk() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        Path path = resolveConfigPath(minecraft);
        if (!Files.exists(path)) {
            return;
        }
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            if (root == null) {
                return;
            }
            JsonObject skillsObj;
            JsonObject keysObj = null;
            if (root.has("skills") && root.get("skills").isJsonObject()) {
                skillsObj = root.getAsJsonObject("skills");
            } else {
                skillsObj = root;
            }
            if (root.has("keys") && root.get("keys").isJsonObject()) {
                keysObj = root.getAsJsonObject("keys");
            }
            Map<String, List<ResourceLocation>> map = new java.util.LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : skillsObj.entrySet()) {
                String keyId = entry.getKey();
                JsonElement element = entry.getValue();
                if (!element.isJsonArray()) {
                    continue;
                }
                List<ResourceLocation> skills = new ArrayList<>();
                for (JsonElement child : element.getAsJsonArray()) {
                    if (!child.isJsonPrimitive()) {
                        continue;
                    }
                    String raw = child.getAsString();
                    ResourceLocation id = ResourceLocation.tryParse(raw);
                    if (id != null) {
                        skills.add(id);
                    }
                }
                map.put(keyId, skills);
            }
            applySnapshot(map, false);
            if (keysObj != null) {
                for (SkillHotbarKey key : SkillHotbarKey.values()) {
                    if (keysObj.has(key.id()) && keysObj.get(key.id()).isJsonPrimitive()) {
                        KEY_CODES.put(key, keysObj.get(key.id()).getAsInt());
                    }
                }
            }
            notifyListeners();
        } catch (Exception e) {
            ChestCavity.LOGGER.warn("[modernui][skill-hotbar] Failed to load client bindings", e);
        }
    }

    private static void sendUpdateToServer() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getConnection() == null) {
            return;
        }
        Map<String, List<ResourceLocation>> payload = toSerializableMap();
        minecraft.execute(() -> minecraft.getConnection().send(new SkillHotbarUpdatePayload(payload)));
    }

    private static Map<String, List<ResourceLocation>> toSerializableMap() {
        Map<SkillHotbarKey, List<ResourceLocation>> snapshot = STATE.snapshot();
        if (snapshot.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<ResourceLocation>> result = new java.util.LinkedHashMap<>();
        for (Map.Entry<SkillHotbarKey, List<ResourceLocation>> entry : snapshot.entrySet()) {
            List<ResourceLocation> skills = entry.getValue();
            if (skills.isEmpty()) {
                continue;
            }
            result.put(entry.getKey().id(), new ArrayList<>(skills));
        }
        return result;
    }

    private static Path resolveConfigPath(Minecraft minecraft) {
        Path configDir = minecraft.gameDirectory.toPath().resolve("config").resolve("chestcavity");
        return configDir.resolve(FILE_NAME);
    }

    public interface Listener {
        void onSkillHotbarUpdated(SkillHotbarState state);
    }
}
