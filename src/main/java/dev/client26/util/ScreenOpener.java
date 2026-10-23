package dev.client26.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Method;

public final class ScreenOpener {
    private ScreenOpener() {}

    public static void open(Screen screen) {
        Minecraft mc = Minecraft.getInstance();
        if (call(mc, "setScreenAndShow", screen)) return;
        try {
            Object gui = Minecraft.class.getField("gui").get(mc);
            if (gui != null && call(gui, "setScreen", screen)) return;
        } catch (ReflectiveOperationException ignored) {}
        call(mc, "setScreen", screen);
    }

    private static boolean call(Object target, String name, Screen screen) {
        for (Method m : target.getClass().getMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == 1
                    && m.getParameterTypes()[0].isAssignableFrom(screen.getClass())) {
                try { m.invoke(target, screen); return true; } catch (ReflectiveOperationException ignored) {}
            }
        }
        return false;
    }
}
