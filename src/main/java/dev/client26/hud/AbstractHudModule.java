package dev.client26.hud;

import dev.client26.util.Gfx;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractHudModule {
    public final transient String id;
    public final transient String name;

    public boolean enabled = true;
    public float x = 0.02f, y = 0.40f;
    public float scale = 1f, opacity = 1f;

    protected transient int w = 60, h = 14;
    protected transient float dim = 1f;
    private transient List<Field> settings;

    protected AbstractHudModule(String id, String name) { this.id = id; this.name = name; }

    public boolean hasHud() { return true; }
    public int width() { return w; }
    public int height() { return h; }
    public void tick(Minecraft mc) {}

    protected abstract void content(Gfx g, boolean preview);

    public void renderOverlay(Gfx g, int sw, int sh, boolean preview) {
        if (!enabled && !preview) return;
        dim = enabled ? 1f : 0.35f;
        g.push();
        g.translate(Math.round(x * sw), Math.round(y * sh));
        g.scale(scale);
        content(g, preview);
        g.pop();
    }

    protected final int c(int argb) { return Gfx.alpha(argb, opacity * dim); }

    protected final void textBox(Gfx g, String s, int color, boolean bg) {
        w = g.width(s) + 8;
        h = 14;
        if (bg) g.rect(0, 0, w, h, c(0x66000000));
        g.text(s, 4, 3, c(color));
    }

    public int left(int sw) { return Math.round(x * sw); }
    public int top(int sh) { return Math.round(y * sh); }
    public int pw() { return Math.max(8, Math.round(width() * scale)); }
    public int ph() { return Math.max(8, Math.round(height() * scale)); }

    public boolean contains(double mx, double my, int sw, int sh) {
        int l = left(sw), t = top(sh);
        return mx >= l - 2 && mx <= l + pw() + 2 && my >= t - 2 && my <= t + ph() + 2;
    }

    public void setPixelPos(int px, int py, int sw, int sh) {
        px = Math.max(0, Math.min(px, sw - pw()));
        py = Math.max(0, Math.min(py, sh - ph()));
        x = px / (float) sw;
        y = py / (float) sh;
    }

    public void clampToScreen(int sw, int sh) { setPixelPos(left(sw), top(sh), sw, sh); }

    public List<Field> settings() {
        if (settings == null) {
            settings = new ArrayList<>();
            for (Class<?> c = getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (f.isAnnotationPresent(Setting.class) && !Modifier.isStatic(f.getModifiers())) {
                        f.setAccessible(true);
                        settings.add(f);
                    }
                }
            }
        }
        return settings;
    }
}
