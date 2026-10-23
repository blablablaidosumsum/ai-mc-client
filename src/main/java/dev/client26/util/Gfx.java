package dev.client26.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

public final class Gfx {
    private static final Gfx I = new Gfx();
    private GuiGraphicsExtractor g;
    private Font font;

    private Gfx() {}

    public static Gfx begin(GuiGraphicsExtractor graphics) {
        I.g = graphics;
        I.font = Minecraft.getInstance().font;
        return I;
    }

    public void fill(int x1, int y1, int x2, int y2, int argb) { g.fill(x1, y1, x2, y2, argb); }
    public void rect(int x, int y, int w, int h, int argb) { g.fill(x, y, x + w, y + h, argb); }

    public void outline(int x, int y, int w, int h, int argb) {
        g.fill(x, y, x + w, y + 1, argb);
        g.fill(x, y + h - 1, x + w, y + h, argb);
        g.fill(x, y + 1, x + 1, y + h - 1, argb);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, argb);
    }

    public void text(String s, int x, int y, int argb) { g.text(font, s, x, y, argb, true); }
    public void textCentered(String s, int cx, int y, int argb) { text(s, cx - font.width(s) / 2, y, argb); }
    public int width(String s) { return font.width(s); }

    public void item(ItemStack s, int x, int y) { g.item(s, x, y); }
    public void itemDecorations(ItemStack s, int x, int y) { g.itemDecorations(font, s, x, y); }

    public void push() { g.pose().pushMatrix(); }
    public void pop() { g.pose().popMatrix(); }
    public void translate(float x, float y) { g.pose().translate(x, y); }
    public void scale(float s) { g.pose().scale(s, s); }

    public static int alpha(int argb, float mul) {
        int a = (int) ((argb >>> 24) * Math.max(0f, Math.min(1f, mul)));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    public static int lerp(int a, int b, float t) {
        int aa = (int) ((a >>> 24) + ((b >>> 24) - (a >>> 24)) * t);
        int ar = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int ag = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int ab = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return (aa << 24) | (ar << 16) | (ag << 8) | ab;
    }
}
