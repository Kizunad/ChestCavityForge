package net.tigereye.chestcavity.client.modernui.skill;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.client.input.ModernUIKeyDispatcher;
import net.tigereye.chestcavity.client.modernui.network.SkillHotbarUpdatePayload;
import org.lwjgl.glfw.GLFW;

public final class SkillHotbarClientData {

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final String FILE_NAME = "modernui_skill_hotbar.json";

  private static final SkillHotbarState STATE = new SkillHotbarState();
  private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();
  private static final EnumMap<SkillHotbarKey, SkillHotbarKeyBinding> KEY_BINDINGS =
      new EnumMap<>(SkillHotbarKey.class);

  private static boolean initialized = false;
  private static boolean pendingInitialSync = true;
  private static boolean applySnapshotInProgress = false;
  private static final List<CaptureListener> CAPTURE_LISTENERS = new CopyOnWriteArrayList<>();

  /** User-facing inactivity guard: abort capture if no keys are pressed within one second. */
  private static final long CAPTURE_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(1);

  /** Limit UI refresh cadence to avoid flooding listeners while the user holds a key. */
  private static final long STATUS_BROADCAST_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(100);

  private static KeyCaptureSession captureSession = null;

  private enum CaptureEndReason {
    CONFIRMED,
    CANCELLED,
    TIMEOUT
  }

  /**
   * Runtime state for the active capture request. Each session tracks modifier/key presses, the
   * most recent preview combination, and timestamps used for inactivity detection.
   */
  private static final class KeyCaptureSession {
    final SkillHotbarKey target;
    final Consumer<SkillHotbarKeyBinding> callback;
    boolean waitingForInitialRelease = true;
    boolean awaitingConfirmation = false;
    boolean hasCandidate = false;
    boolean cancelledByUser = false;
    SkillHotbarKeyBinding preview = null;
    long lastInputNanos = System.nanoTime();
    long lastBroadcastNanos = 0L;

    KeyCaptureSession(SkillHotbarKey target, Consumer<SkillHotbarKeyBinding> callback) {
      this.target = target;
      this.callback = callback;
    }
  }

  /**
   * Immutable snapshot describing the capture overlay state. The record is exposed to listeners so
   * the Modern UI configuration screen can mirror countdowns and button availability.
   */
  public record KeyCaptureStatus(
      boolean active,
      SkillHotbarKey target,
      SkillHotbarKeyBinding preview,
      boolean hasCandidate,
      boolean awaitingConfirmation,
      boolean waitingForInitialRelease,
      boolean timedOut,
      boolean cancelled,
      long inactivityMillisRemaining) {}

  public interface CaptureListener {
    void onCaptureStatusChanged(KeyCaptureStatus status);
  }

  private SkillHotbarClientData() {}

  public static void initialize() {
    if (initialized) {
      return;
    }
    initialized = true;
    KEY_BINDINGS.clear();
    for (SkillHotbarKey key : SkillHotbarKey.values()) {
      KEY_BINDINGS.put(key, SkillHotbarKeyBinding.of(key.defaultKeyCode(), false, false, false));
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
      SkillHotbarKeyBinding defaultBinding =
          SkillHotbarKeyBinding.of(key.defaultKeyCode(), false, false, false);
      SkillHotbarKeyBinding existing = getBinding(key);
      if (!defaultBinding.equals(existing)) {
        changed = true;
      }
      KEY_BINDINGS.put(key, defaultBinding);
    }
    if (changed) {
      ModernUIKeyDispatcher.resetKeyStates();
    }
    persistAndSync();
  }

  public static void applySnapshot(
      Map<String, List<ResourceLocation>> snapshot, boolean fromServer) {
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

  public static SkillHotbarKeyBinding getBinding(SkillHotbarKey key) {
    return KEY_BINDINGS.getOrDefault(key, SkillHotbarKeyBinding.UNBOUND);
  }

  public static int getKeyCode(SkillHotbarKey key) {
    return getBinding(key).keyCode();
  }

  public static void setBinding(SkillHotbarKey key, SkillHotbarKeyBinding newBinding) {
    SkillHotbarKeyBinding sanitized =
        newBinding == null ? SkillHotbarKeyBinding.UNBOUND : newBinding;
    SkillHotbarKeyBinding oldBinding = getBinding(key);
    if (oldBinding.equals(sanitized)) {
      return;
    }
    KEY_BINDINGS.put(key, sanitized);
    ModernUIKeyDispatcher.onKeyRebind(oldBinding, sanitized);
    persistAndSync();
  }

  @Deprecated
  public static void setKeyCode(SkillHotbarKey key, int newCode) {
    setBinding(key, SkillHotbarKeyBinding.of(newCode, false, false, false));
  }

  public static String describeBinding(SkillHotbarKeyBinding binding) {
    return binding == null ? SkillHotbarKeyBinding.UNBOUND.describe() : binding.describe();
  }

  public static String describeKey(int keyCode) {
    return describeBinding(SkillHotbarKeyBinding.of(keyCode, false, false, false));
  }

  public static boolean requestKeyCapture(
      SkillHotbarKey key, Consumer<SkillHotbarKeyBinding> callback) {
    if (captureSession != null) {
      return false;
    }
    KeyCaptureSession session = new KeyCaptureSession(key, callback);
    captureSession = session;
    broadcastStatus(buildStatus(session, System.nanoTime(), true, false, false));
    return true;
  }

  public static boolean isCapturing() {
    return captureSession != null;
  }

  public static SkillHotbarKey currentCaptureTarget() {
    return captureSession == null ? null : captureSession.target;
  }

  public static boolean processCapture(long windowHandle) {
    KeyCaptureSession session = captureSession;
    if (session == null) {
      return false;
    }

    long now = System.nanoTime();

    // Esc acts as an explicit cancel action and immediately ends the capture cycle.
    if (GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
      session.cancelledByUser = true;
      endCapture(session, null, CaptureEndReason.CANCELLED);
      return true;
    }

    boolean ctrlDown =
        GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    boolean altDown =
        GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
    boolean shiftDown =
        GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;

    int primaryKey = findPrimaryKey(windowHandle);
    boolean anyDown = ctrlDown || altDown || shiftDown || primaryKey > GLFW.GLFW_KEY_UNKNOWN;

    if (session.waitingForInitialRelease) {
      if (!anyDown) {
        session.waitingForInitialRelease = false;
        session.lastInputNanos = now;
        broadcastStatus(buildStatus(session, now, true, false, false));
      } else {
        if (now - session.lastInputNanos >= CAPTURE_TIMEOUT_NANOS) {
          endCapture(session, null, CaptureEndReason.TIMEOUT);
        } else {
          broadcastIfDue(session, now);
        }
      }
      return true;
    }

    if (session.awaitingConfirmation) {
      broadcastIfDue(session, now);
      return true;
    }

    if (anyDown && primaryKey > GLFW.GLFW_KEY_UNKNOWN) {
      SkillHotbarKeyBinding candidate =
          SkillHotbarKeyBinding.of(primaryKey, ctrlDown, altDown, shiftDown);
      session.preview = candidate;
      session.hasCandidate = candidate.isBound();
      session.lastInputNanos = now;
      broadcastStatus(buildStatus(session, now, true, false, false));
      return true;
    }

    if (anyDown) {
      session.lastInputNanos = now;
      broadcastIfDue(session, now);
      return true;
    }

    if (session.hasCandidate && session.preview != null) {
      session.awaitingConfirmation = true;
      broadcastStatus(buildStatus(session, now, true, false, false));
      return true;
    }

    long inactive = now - session.lastInputNanos;
    if (inactive >= CAPTURE_TIMEOUT_NANOS) {
      endCapture(session, null, CaptureEndReason.TIMEOUT);
      return false;
    }

    broadcastIfDue(session, now);
    return true;
  }

  public static boolean confirmKeyCapture() {
    KeyCaptureSession session = captureSession;
    if (session == null || !session.awaitingConfirmation || session.preview == null) {
      return false;
    }
    endCapture(session, session.preview, CaptureEndReason.CONFIRMED);
    return true;
  }

  public static void restartKeyCapture() {
    KeyCaptureSession session = captureSession;
    if (session == null) {
      return;
    }
    session.waitingForInitialRelease = true;
    session.awaitingConfirmation = false;
    session.hasCandidate = false;
    session.preview = null;
    session.cancelledByUser = false;
    session.lastInputNanos = System.nanoTime();
    broadcastStatus(buildStatus(session, session.lastInputNanos, true, false, false));
  }

  public static void cancelKeyCapture() {
    KeyCaptureSession session = captureSession;
    if (session == null) {
      return;
    }
    session.cancelledByUser = true;
    endCapture(session, null, CaptureEndReason.CANCELLED);
  }

  public static void addCaptureListener(CaptureListener listener) {
    CAPTURE_LISTENERS.add(listener);
  }

  public static void removeCaptureListener(CaptureListener listener) {
    CAPTURE_LISTENERS.remove(listener);
  }

  private static void endCapture(
      KeyCaptureSession session, SkillHotbarKeyBinding binding, CaptureEndReason reason) {
    if (captureSession != session) {
      return;
    }
    captureSession = null;
    long now = System.nanoTime();
    broadcastStatus(
        buildStatus(
            session,
            now,
            false,
            reason == CaptureEndReason.TIMEOUT,
            reason == CaptureEndReason.CANCELLED));
    if (reason == CaptureEndReason.CONFIRMED && binding != null) {
      Consumer<SkillHotbarKeyBinding> callback = session.callback;
      if (callback != null) {
        callback.accept(binding);
      } else {
        setBinding(session.target, binding);
      }
    }
  }

  private static int findPrimaryKey(long windowHandle) {
    for (int code = GLFW.GLFW_KEY_SPACE; code <= GLFW.GLFW_KEY_LAST; code++) {
      if (isModifierKey(code)) {
        continue;
      }
      if (GLFW.glfwGetKey(windowHandle, code) == GLFW.GLFW_PRESS) {
        return code;
      }
    }
    return GLFW.GLFW_KEY_UNKNOWN;
  }

  private static boolean isModifierKey(int code) {
    return switch (code) {
      case GLFW.GLFW_KEY_LEFT_CONTROL,
          GLFW.GLFW_KEY_RIGHT_CONTROL,
          GLFW.GLFW_KEY_LEFT_ALT,
          GLFW.GLFW_KEY_RIGHT_ALT,
          GLFW.GLFW_KEY_LEFT_SHIFT,
          GLFW.GLFW_KEY_RIGHT_SHIFT -> true;
      default -> false;
    };
  }

  private static void broadcastIfDue(KeyCaptureSession session, long now) {
    if (now - session.lastBroadcastNanos >= STATUS_BROADCAST_INTERVAL_NANOS) {
      broadcastStatus(buildStatus(session, now, true, false, false));
    }
  }

  private static void broadcastStatus(KeyCaptureStatus status) {
    if (status != null && captureSession != null) {
      captureSession.lastBroadcastNanos = System.nanoTime();
    }
    for (CaptureListener listener : CAPTURE_LISTENERS) {
      listener.onCaptureStatusChanged(status);
    }
  }

  private static KeyCaptureStatus buildStatus(
      KeyCaptureSession session, long now, boolean active, boolean timedOut, boolean cancelled) {
    long remainingMillis = 0L;
    if (active) {
      if (session.waitingForInitialRelease || session.awaitingConfirmation) {
        remainingMillis = TimeUnit.NANOSECONDS.toMillis(CAPTURE_TIMEOUT_NANOS);
      } else {
        long remaining = Math.max(0L, CAPTURE_TIMEOUT_NANOS - (now - session.lastInputNanos));
        remainingMillis = TimeUnit.NANOSECONDS.toMillis(remaining);
      }
    }
    return new KeyCaptureStatus(
        active,
        session.target,
        session.preview,
        session.hasCandidate,
        session.awaitingConfirmation,
        session.waitingForInitialRelease,
        timedOut,
        cancelled || session.cancelledByUser,
        remainingMillis);
  }

  private static SkillHotbarKeyBinding parseBinding(JsonElement element) {
    if (element == null || element.isJsonNull()) {
      return SkillHotbarKeyBinding.UNBOUND;
    }
    try {
      if (element.isJsonObject()) {
        JsonObject obj = element.getAsJsonObject();
        int keyCode =
            obj.has("key") && obj.get("key").isJsonPrimitive()
                ? obj.get("key").getAsInt()
                : GLFW.GLFW_KEY_UNKNOWN;
        boolean ctrl = obj.has("ctrl") && obj.get("ctrl").getAsBoolean();
        boolean alt = obj.has("alt") && obj.get("alt").getAsBoolean();
        boolean shift = obj.has("shift") && obj.get("shift").getAsBoolean();
        return SkillHotbarKeyBinding.of(keyCode, ctrl, alt, shift);
      }
      if (element.isJsonPrimitive()) {
        return SkillHotbarKeyBinding.of(element.getAsInt(), false, false, false);
      }
    } catch (Exception e) {
      ChestCavity.LOGGER.warn("[modernui][skill-hotbar] Invalid key binding entry in config", e);
    }
    return null;
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
        SkillHotbarKeyBinding binding = getBinding(key);
        JsonObject bindingObj = new JsonObject();
        bindingObj.addProperty("key", binding.keyCode());
        bindingObj.addProperty("ctrl", binding.ctrl());
        bindingObj.addProperty("alt", binding.alt());
        bindingObj.addProperty("shift", binding.shift());
        keysObj.add(key.id(), bindingObj);
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
        boolean changed = false;
        for (SkillHotbarKey key : SkillHotbarKey.values()) {
          if (!keysObj.has(key.id())) {
            continue;
          }
          JsonElement element = keysObj.get(key.id());
          SkillHotbarKeyBinding parsed = parseBinding(element);
          if (parsed == null) {
            continue;
          }
          SkillHotbarKeyBinding existing = getBinding(key);
          if (!parsed.equals(existing)) {
            KEY_BINDINGS.put(key, parsed);
            changed = true;
          }
        }
        if (changed) {
          ModernUIKeyDispatcher.resetKeyStates();
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
