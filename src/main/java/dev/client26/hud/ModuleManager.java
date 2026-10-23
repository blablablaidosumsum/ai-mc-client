package dev.client26.hud;

import com.google.gson.*;
import dev.client26.ClientCore;
import dev.client26.gui.HudEditScreen;
import dev.client26.hud.modules.*;
import dev.client26.util.Compat;
import dev.client26.util.Gfx;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class ModuleManager {
    public static final ModuleManager I = new ModuleManager();
    public static final String PVP = "Competitive PvP";
    public static final String MIN = "Minimalist / Max FPS";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public final KeystrokesModule keystrokes = new KeystrokesModule();
    public final ArmorStatusModule armor = new ArmorStatusModule();
    public final InfoModules.Fps fps = new InfoModules.Fps();
    public final InfoModules.Ping ping = new InfoModules.Ping();
    public final InfoModules.Coords coords = new InfoModules.Coords();
    public final InfoModules.Compass compass = new InfoModules.Compass();
    public final ComboReachModule combo = new ComboReachModule();
    public final CrosshairModule crosshair = new CrosshairModule();
    public final ViewmodelModule viewmodel = new ViewmodelModule();
    public final LegacyModule legacy = new LegacyModule();
    public final PerformanceModule performance = new PerformanceModule();

    public final AbstractHudModule[] arr = {
            keystrokes, armor, fps, ping, coords, compass, combo, crosshair, viewmodel, legacy, performance };

    public final Map<String, JsonObject> profiles = new LinkedHashMap<>();
    public String activeProfile = PVP;
    private final Path file = FabricLoader.getInstance().getConfigDir().resolve("client26.json");

    private ModuleManager() {}

    public void init() {
        ComboReachModule.registerEvents(combo);
        load();

        Identifier hudId = Identifier.fromNamespaceAndPath(ClientCore.ID, "hud");
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, hudId, (g, delta) -> renderAll(g));

        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (g, delta) -> {
            Minecraft mc = Minecraft.getInstance();
            if (crosshair.enabled && mc.options.getCameraType().isFirstPerson() && !(Compat.screen() instanceof HudEditScreen)) {
                if (!Compat.hideGui()) crosshair.renderOverlay(Gfx.begin(g),
                        mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), false);
            } else {
                original.extractRenderState(g, delta);
            }
        });
    }

    private void renderAll(net.minecraft.client.gui.GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || Compat.hideGui() || Compat.screen() instanceof HudEditScreen) return;
        Gfx gfx = Gfx.begin(g);
        int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
        for (int i = 0; i < arr.length; i++) {
            AbstractHudModule m = arr[i];
            if (m.enabled && m.hasHud()) m.renderOverlay(gfx, sw, sh, false);
        }
    }

    public void tick(Minecraft mc) {
        if (mc.player == null) return;
        for (int i = 0; i < arr.length; i++) if (arr[i].enabled) arr[i].tick(mc);
    }

    public JsonObject snapshot() {
        JsonObject o = new JsonObject();
        for (AbstractHudModule m : arr) o.add(m.id, GSON.toJsonTree(m));
        return o;
    }

    public void applySnapshot(JsonObject snap) {
        for (AbstractHudModule m : arr) {
            JsonElement e = snap.get(m.id);
            if (e != null && e.isJsonObject()) loadInto(m, e.getAsJsonObject());
        }
    }

    public void applyProfile(String name) {
        JsonObject p = profiles.get(name);
        if (p == null) return;
        applySnapshot(p);
        activeProfile = name;
        save();
    }

    public void saveProfile(String name) { profiles.put(name, snapshot()); activeProfile = name; save(); }

    public String newProfile() {
        int i = 1;
        while (profiles.containsKey("Custom " + i)) i++;
        String n = "Custom " + i;
        saveProfile(n);
        return n;
    }

    private void buildDefaultProfiles() {
        Minecraft mc = Minecraft.getInstance();
        int sw = mc.getWindow() != null ? Math.max(1, mc.getWindow().getGuiScaledWidth()) : 480;
        compass.x = Math.max(0f, 0.5f - 90f / sw);
        profiles.put(PVP, snapshot());

        for (AbstractHudModule m : arr) m.enabled = false;
        fps.enabled = true;  fps.x = 0.01f;  fps.y = 0.01f; fps.scale = 0.9f; fps.opacity = 0.7f;
        coords.enabled = true; coords.x = 0.01f; coords.y = 0.06f; coords.scale = 0.9f; coords.opacity = 0.7f;
        performance.enabled = true; performance.entityDistance = 0.75f;
        profiles.put(MIN, snapshot());

        applySnapshot(profiles.get(PVP));
        activeProfile = PVP;
    }

    public void load() {
        if (!Files.exists(file)) { buildDefaultProfiles(); save(); return; }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            JsonObject profs = root.has("profiles") ? root.getAsJsonObject("profiles") : new JsonObject();
            for (Map.Entry<String, JsonElement> e : profs.entrySet()) profiles.put(e.getKey(), e.getValue().getAsJsonObject());
            if (root.has("modules")) applySnapshot(root.getAsJsonObject("modules"));
            if (root.has("activeProfile")) activeProfile = root.get("activeProfile").getAsString();
            if (profiles.isEmpty()) { buildDefaultProfiles(); save(); }
        } catch (Exception ex) {
            ClientCore.LOG.warn("client26.json unreadable, rebuilding defaults", ex);
            profiles.clear();
            buildDefaultProfiles();
            save();
        }
    }

    public void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("activeProfile", activeProfile);
            root.add("modules", snapshot());
            JsonObject profs = new JsonObject();
            profiles.forEach(profs::add);
            root.add("profiles", profs);
            Files.writeString(file, GSON.toJson(root));
        } catch (Exception ex) {
            ClientCore.LOG.warn("could not save client26.json", ex);
        }
    }

    private static void loadInto(AbstractHudModule m, JsonObject o) {
        for (Class<?> c = m.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                int mod = f.getModifiers();
                if (Modifier.isStatic(mod) || Modifier.isTransient(mod) || Modifier.isFinal(mod)) continue;
                JsonElement e = o.get(f.getName());
                if (e == null) continue;
                try {
                    f.setAccessible(true);
                    f.set(m, GSON.fromJson(e, f.getGenericType()));
                } catch (Exception ignored) {}
            }
        }
    }
}
