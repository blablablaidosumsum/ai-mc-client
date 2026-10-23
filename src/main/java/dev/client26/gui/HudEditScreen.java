package dev.client26.gui;

import dev.client26.hud.AbstractHudModule;
import dev.client26.hud.ModuleManager;
import dev.client26.hud.Setting;
import dev.client26.util.Gfx;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;

public final class HudEditScreen extends Screen {
    private static final int[] PALETTE = {
            0xFFFFFFFF, 0xFFFF5555, 0xFF55FF55, 0xFF5555FF, 0xFFFFFF55, 0xFF55FFFF, 0xFFFF55FF, 0xFFFFAA00, 0xFF000000 };
    private static final int SIDE_W = 122, PANEL_W = 180, ROW = 14;

    private static final class Hit {
        int x, y, w, h; Runnable click; DoubleConsumer drag;
        boolean in(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private final ModuleManager mm = ModuleManager.I;
    private final List<Hit> hits = new ArrayList<>();
    private AbstractHudModule selected, dragging;
    private Hit activeDrag;
    private int grabX, grabY, panelScroll;
    private boolean snapX, snapY;

    public HudEditScreen() { super(Component.literal("Client26")); }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ge, int mx, int my, float partial) {
        Gfx g = Gfx.begin(ge);
        hits.clear();
        g.fill(0, 0, width, height, 0x88000000);

        for (AbstractHudModule m : mm.arr) {
            m.renderOverlay(g, width, height, true);
            if (!m.hasHud()) continue;
            int col = m == selected ? 0xFFFFD84A : m.enabled ? 0x88FFFFFF : 0x66FF5555;
            g.outline(m.left(width) - 2, m.top(height) - 2, m.pw() + 4, m.ph() + 4, col);
        }
        if (dragging != null) {
            if (snapX) g.fill(width / 2, 0, width / 2 + 1, height, 0x88FFD84A);
            if (snapY) g.fill(0, height / 2, width, height / 2 + 1, 0x88FFD84A);
        }

        drawSidebar(g);
        drawProfileBar(g);
        if (selected != null) drawPanel(g);
        g.textCentered("Drag modules  |  Scroll = scale  |  RShift/Esc = close", width / 2, height - 10, 0xFFAAAAAA);
    }

    private void drawSidebar(Gfx g) {
        int x = 6, y = 6;
        g.rect(x - 2, y - 2, SIDE_W, mm.arr.length * ROW + 18, 0xAA000000);
        g.text("Client26", x + 2, y + 1, 0xFFFFD84A);
        y += 14;
        for (AbstractHudModule m : mm.arr) {
            final AbstractHudModule mod = m;
            g.rect(x, y + 2, 8, 8, m.enabled ? 0xFF55FF55 : 0xFF553333);
            button(g, x, y + 1, 10, 10, () -> mod.enabled = !mod.enabled);
            g.text(m.name, x + 14, y + 2, m == selected ? 0xFFFFD84A : 0xFFFFFFFF);
            button(g, x + 12, y, SIDE_W - 16, ROW, () -> selected = mod);
            y += ROW;
        }
    }

    private void drawProfileBar(Gfx g) {
        int x = SIDE_W + 14, y = 6;
        for (String name : mm.profiles.keySet()) {
            int bw = g.width(name) + 10;
            boolean on = name.equals(mm.activeProfile);
            g.rect(x, y, bw, 14, on ? 0xCC3A7BD5 : 0xAA000000);
            g.text(name, x + 5, y + 3, 0xFFFFFFFF);
            final String n = name;
            button(g, x, y, bw, 14, () -> mm.applyProfile(n));
            x += bw + 3;
        }
        x = addBtn(g, x, y, "Save", () -> mm.saveProfile(mm.activeProfile));
        addBtn(g, x, y, "+ New", () -> mm.newProfile());
    }

    private int addBtn(Gfx g, int x, int y, String label, Runnable r) {
        int bw = g.width(label) + 10;
        g.rect(x, y, bw, 14, 0xAA224422);
        g.text(label, x + 5, y + 3, 0xFFFFFFFF);
        button(g, x, y, bw, 14, r);
        return x + bw + 3;
    }

    private void drawPanel(Gfx g) {
        AbstractHudModule m = selected;
        int px = width - PANEL_W - 6, py = 6;
        int contentH = height - 28;
        g.rect(px - 4, py - 2, PANEL_W + 4, contentH, 0xAA000000);
        int y = py + 2 - panelScroll;
        g.text(m.name, px, y, 0xFFFFD84A); y += ROW;
        y = toggle(g, px, y, "Enabled", m.enabled, v -> m.enabled = v);
        if (m.hasHud()) {
            y = slider(g, px, y, "Scale", m.scale, 0.5f, 3f, false, v -> { m.scale = (float) v; m.clampToScreen(width, height); });
            y = slider(g, px, y, "Opacity", m.opacity, 0.1f, 1f, false, v -> m.opacity = (float) v);
        }
        for (Field f : m.settings()) {
            Setting s = f.getAnnotation(Setting.class);
            String label = s.value().isEmpty() ? f.getName() : s.value();
            Class<?> t = f.getType();
            try {
                if (t == boolean.class) {
                    y = toggle(g, px, y, label, f.getBoolean(m), v -> set(f, m, v));
                } else if (t == int.class && s.color()) {
                    y = colorRow(g, px, y, label, f, m);
                } else if (t == int.class || t == float.class) {
                    boolean isInt = t == int.class;
                    double cur = isInt ? f.getInt(m) : f.getFloat(m);
                    y = slider(g, px, y, label, cur, s.min(), s.max(), isInt, v -> set(f, m, isInt ? (Object) (int) Math.round(v) : (Object) (float) v));
                } else if (t.isEnum()) {
                    Object cur = f.get(m);
                    y = cycle(g, px, y, label, f, m, cur);
                }
            } catch (IllegalAccessException ignored) {}
        }
        if (m == mm.crosshair) {
            y = action(g, px, y, "Simulate attack bloom", () -> mm.crosshair.kick());
        }
        int maxScroll = Math.max(0, (y + panelScroll) - (py + contentH));
        panelScroll = Math.min(panelScroll, maxScroll);
    }

    private void button(Gfx g, int x, int y, int w, int h, Runnable r) {
        Hit hit = new Hit(); hit.x = x; hit.y = y; hit.w = w; hit.h = h; hit.click = r; hits.add(hit);
    }

    private int toggle(Gfx g, int x, int y, String label, boolean val, java.util.function.Consumer<Boolean> set) {
        g.rect(x, y, PANEL_W - 8, ROW - 2, 0x33FFFFFF);
        g.text(label, x + 3, y + 2, 0xFFFFFFFF);
        g.rect(x + PANEL_W - 28, y + 2, 16, ROW - 6, val ? 0xFF55FF55 : 0xFF884444);
        button(g, x, y, PANEL_W - 8, ROW - 2, () -> set.accept(!val));
        return y + ROW;
    }

    private int slider(Gfx g, int x, int y, String label, double val, double min, double max, boolean isInt,
                       DoubleConsumer set) {
        int w = PANEL_W - 8, h = ROW - 2;
        double r = Math.max(0, Math.min(1, (val - min) / (max - min)));
        g.rect(x, y, w, h, 0x44FFFFFF);
        g.rect(x, y, (int) (w * r), h, 0x883A7BD5);
        g.text(label, x + 3, y + 2, 0xFFFFFFFF);
        String v = isInt ? Integer.toString((int) Math.round(val)) : String.format("%.2f", val);
        g.text(v, x + w - g.width(v) - 3, y + 2, 0xFFDDDDDD);
        Hit hit = new Hit(); hit.x = x; hit.y = y; hit.w = w; hit.h = h;
        final int sx = x;
        hit.drag = mx -> set.accept(min + (max - min) * Math.max(0, Math.min(1, (mx - sx) / (double) w)));
        hits.add(hit);
        return y + ROW;
    }

    private int colorRow(Gfx g, int x, int y, String label, Field f, AbstractHudModule m) throws IllegalAccessException {
        int cur = f.getInt(m);
        g.rect(x, y, PANEL_W - 8, ROW - 2, 0x33FFFFFF);
        g.text(label, x + 3, y + 2, 0xFFFFFFFF);
        g.rect(x + PANEL_W - 28, y + 1, 16, ROW - 4, 0xFF000000 | cur);
        button(g, x, y, PANEL_W - 8, ROW - 2, () -> {
            int idx = 0;
            for (int i = 0; i < PALETTE.length; i++) if ((PALETTE[i] & 0xFFFFFF) == (cur & 0xFFFFFF)) idx = i;
            int next = PALETTE[(idx + 1) % PALETTE.length];
            set(f, m, (cur & 0xFF000000) | (next & 0xFFFFFF));
        });
        return y + ROW;
    }

    private int cycle(Gfx g, int x, int y, String label, Field f, AbstractHudModule m, Object cur) {
        g.rect(x, y, PANEL_W - 8, ROW - 2, 0x33FFFFFF);
        g.text(label, x + 3, y + 2, 0xFFFFFFFF);
        String v = cur.toString();
        g.text(v, x + PANEL_W - 8 - g.width(v) - 3, y + 2, 0xFFFFD84A);
        button(g, x, y, PANEL_W - 8, ROW - 2, () -> {
            Object[] all = f.getType().getEnumConstants();
            set(f, m, all[(((Enum<?>) cur).ordinal() + 1) % all.length]);
        });
        return y + ROW;
    }

    private int action(Gfx g, int x, int y, String label, Runnable r) {
        g.rect(x, y, PANEL_W - 8, ROW - 2, 0x88224422);
        g.textCentered(label, x + (PANEL_W - 8) / 2, y + 2, 0xFFFFFFFF);
        button(g, x, y, PANEL_W - 8, ROW - 2, r);
        return y + ROW;
    }

    private static void set(Field f, Object o, Object v) {
        try { f.set(o, v); } catch (IllegalAccessException ignored) {}
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        double mx = e.x(), my = e.y();
        if (e.button() != 0) return super.mouseClicked(e, doubleClick);
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!h.in(mx, my)) continue;
            if (h.click != null) h.click.run();
            if (h.drag != null) { h.drag.accept(mx); activeDrag = h; }
            return true;
        }
        for (int i = mm.arr.length - 1; i >= 0; i--) {
            AbstractHudModule m = mm.arr[i];
            if (m.hasHud() && m.contains(mx, my, width, height)) {
                selected = m; dragging = m;
                grabX = (int) mx - m.left(width);
                grabY = (int) my - m.top(height);
                return true;
            }
        }
        return super.mouseClicked(e, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (activeDrag != null) { activeDrag.drag.accept(e.x()); return true; }
        if (dragging != null) {
            int nx = (int) e.x() - grabX, ny = (int) e.y() - grabY;
            int cx = width / 2 - dragging.pw() / 2, cy = height / 2 - dragging.ph() / 2;
            snapX = Math.abs(nx - cx) <= 4; snapY = Math.abs(ny - cy) <= 4;
            if (snapX) nx = cx;
            if (snapY) ny = cy;
            dragging.setPixelPos(nx, ny, width, height);
            return true;
        }
        return super.mouseDragged(e, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        boolean was = activeDrag != null || dragging != null;
        activeDrag = null; dragging = null; snapX = snapY = false;
        if (was) mm.save();
        return was || super.mouseReleased(e);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (selected != null && mx >= width - PANEL_W - 10) {
            panelScroll = Math.max(0, panelScroll - (int) (sy * 12));
            return true;
        }
        AbstractHudModule m = selected;
        if (m != null && m.hasHud()) {
            m.scale = Math.max(0.5f, Math.min(3f, m.scale + (float) sy * 0.05f));
            m.clampToScreen(width, height);
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (e.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) { onClose(); return true; }
        return super.keyPressed(e);
    }

    @Override
    public void onClose() {
        mm.save();
        super.onClose();
    }
}
