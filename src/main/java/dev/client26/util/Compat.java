package dev.client26.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Finds fields/methods that moved in 26.2 by reflection. Resolved once, then cached. */
public final class Compat {
    private Compat() {}

    private static Object guiObj(Minecraft mc) {
        try { return Minecraft.class.getField("gui").get(mc); } catch (ReflectiveOperationException ignored) {}
        try { return Minecraft.class.getMethod("gui").invoke(mc); } catch (ReflectiveOperationException ignored) {}
        return null;
    }

    // ---- current screen ----
    private static Method sMethod;
    private static Field sField;
    private static boolean sOnGui, sResolved;

    public static Screen screen() {
        Minecraft mc = Minecraft.getInstance();
        if (!sResolved) {
            sResolved = true;
            Object[] targets = { mc, guiObj(mc) };
            for (int i = 0; i < 2 && sMethod == null && sField == null; i++) {
                Object t = targets[i];
                if (t == null) continue;
                for (String n : new String[] { "screen", "getScreen" }) {
                    try {
                        Method m = t.getClass().getMethod(n);
                        if (Screen.class.isAssignableFrom(m.getReturnType())) { sMethod = m; sOnGui = i == 1; break; }
                    } catch (ReflectiveOperationException ignored) {}
                }
                if (sMethod == null) {
                    try {
                        Field f = t.getClass().getField("screen");
                        if (Screen.class.isAssignableFrom(f.getType())) { sField = f; sOnGui = i == 1; }
                    } catch (ReflectiveOperationException ignored) {}
                }
            }
        }
        try {
            Object t = sOnGui ? guiObj(mc) : mc;
            if (t == null) return null;
            if (sMethod != null) return (Screen) sMethod.invoke(t);
            if (sField != null) return (Screen) sField.get(t);
        } catch (ReflectiveOperationException ignored) {}
        return null;
    }

    // ---- F1 hide-GUI flag ----
    private static Field hField;
    private static Method hMethod;
    private static boolean hResolved;

    public static boolean hideGui() {
        Object o = Minecraft.getInstance().options;
        if (!hResolved) {
            hResolved = true;
            try { hField = o.getClass().getField("hideGui"); } catch (ReflectiveOperationException ignored) {}
            if (hField == null) {
                try { hMethod = o.getClass().getMethod("hideGui"); } catch (ReflectiveOperationException ignored) {}
            }
        }
        try {
            if (hField != null) return hField.getBoolean(o);
            if (hMethod != null) return (Boolean) hMethod.invoke(o);
        } catch (ReflectiveOperationException ignored) {}
        return false;
    }
}
