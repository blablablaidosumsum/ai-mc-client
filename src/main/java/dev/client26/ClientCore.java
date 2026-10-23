package dev.client26;

import dev.client26.gui.HudEditScreen;
import dev.client26.hud.ModuleManager;
import dev.client26.util.ScreenOpener;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClientCore implements ClientModInitializer {
    public static final String ID = "client26";
    public static final Logger LOG = LoggerFactory.getLogger("Client26");
    public static KeyMapping OPEN_GUI;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(ID, "main"));
        OPEN_GUI = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.client26.open", GLFW.GLFW_KEY_RIGHT_SHIFT, category));

        ModuleManager.I.init();

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (OPEN_GUI.consumeClick()) {
                if (mc.player != null && mc.screen == null) ScreenOpener.open(new HudEditScreen());
            }
            ModuleManager.I.tick(mc);
        });
        LOG.info("Client26 loaded");
    }
}
