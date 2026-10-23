package dev.client26.hud.modules;

import dev.client26.hud.AbstractHudModule;
import dev.client26.hud.Setting;
import dev.client26.util.CpsTracker;
import dev.client26.util.Gfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

public final class KeystrokesModule extends AbstractHudModule {
    public enum Anim { NONE, FADE, FADE_SHRINK }

    private static final String[] CPS = new String[100];
    static { for (int i = 0; i < CPS.length; i++) CPS[i] = i + " CPS"; }

    @Setting("Animation") public Anim anim = Anim.FADE_SHRINK;
    @Setting(value = "Fade (ms)", min = 0, max = 300) public int fadeMs = 90;
    @Setting("Show mouse + CPS") public boolean showMouse = true;
    @Setting("Show space bar") public boolean showSpace = true;
    @Setting("Show sneak/sprint") public boolean showStatus = true;
    @Setting(value = "Idle color", color = true) public int idleColor = 0x66000000;
    @Setting(value = "Pressed color", color = true) public int pressedColor = 0xCCFFFFFF;
    @Setting(value = "Text color", color = true) public int textColor = 0xFFFFFFFF;
    @Setting(value = "Pressed text", color = true) public int pressedText = 0xFF000000;

    private final transient float[] a = new float[9];
    private transient long last = System.nanoTime();

    public KeystrokesModule() { super("keystrokes", "Keystrokes"); x = 0.02f; y = 0.68f; }

    @Override
    protected void content(Gfx g, boolean preview) {
        Minecraft mc = Minecraft.getInstance();
        Options o = mc.options;
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1e9f);
        last = now;

        boolean atk = o.keyAttack.isDown(), use = o.keyUse.isDown();
        CpsTracker.poll(0, atk);
        CpsTracker.poll(1, use);

        final int K = 22, G = 2, W3 = 3 * K + 2 * G;
        int yy = 0;
        key(g, 0, dt, o.keyUp.isDown(), K + G, yy, K, K, "W");
        yy += K + G;
        key(g, 1, dt, o.keyLeft.isDown(), 0, yy, K, K, "A");
        key(g, 2, dt, o.keyDown.isDown(), K + G, yy, K, K, "S");
        key(g, 3, dt, o.keyRight.isDown(), 2 * (K + G), yy, K, K, "D");
        yy += K + G;

        if (showMouse) {
            int bw = (W3 - G) / 2;
            key(g, 4, dt, atk, 0, yy, bw, 26, "LMB");
            key(g, 5, dt, use, bw + G, yy, bw, 26, "RMB");
            cps(g, bw / 2, yy + 14, CpsTracker.cps(0));
            cps(g, bw + G + bw / 2, yy + 14, CpsTracker.cps(1));
            yy += 26 + G;
        }
        if (showSpace) {
            key(g, 6, dt, o.keyJump.isDown(), 0, yy, W3, 14, "SPACE");
            yy += 14 + G;
        }
        if (showStatus) {
            int bw = (W3 - G) / 2;
            key(g, 7, dt, o.keyShift.isDown(), 0, yy, bw, 14, "Sneak");
            key(g, 8, dt, o.keySprint.isDown() || (mc.player != null && mc.player.isSprinting()), bw + G, yy, bw, 14, "Sprint");
            yy += 14 + G;
        }
        w = W3;
        h = yy - G;
    }

    private void cps(Gfx g, int cx, int y, int v) {
        g.push();
        g.translate(cx, y);
        g.scale(0.75f);
        g.textCentered(CPS[Math.min(v, 99)], 0, 0, c(textColor));
        g.pop();
    }

    private void key(Gfx g, int i, float dt, boolean down, int x, int y, int bw, int bh, String label) {
        float target = down ? 1f : 0f;
        if (anim == Anim.NONE || fadeMs <= 0) a[i] = target;
        else {
            float step = dt / (fadeMs / 1000f);
            a[i] = a[i] < target ? Math.min(target, a[i] + step) : Math.max(target, a[i] - step);
        }
        int inset = anim == Anim.FADE_SHRINK ? Math.round(a[i] * 1.5f) : 0;
        g.rect(x + inset, y + inset, bw - 2 * inset, bh - 2 * inset, c(Gfx.lerp(idleColor, pressedColor, a[i])));
        int ty = bh > 20 ? y + 3 : y + (bh - 8) / 2 + 1;
        g.textCentered(label, x + bw / 2, ty, c(Gfx.lerp(textColor, pressedText, a[i])));
    }
}
