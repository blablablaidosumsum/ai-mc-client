package dev.client26.hud.modules;

import dev.client26.hud.AbstractHudModule;
import dev.client26.hud.Setting;
import dev.client26.util.Gfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class CrosshairModule extends AbstractHudModule {
    public enum Shape { CROSS, CIRCLE, DOT }

    @Setting("Shape") public Shape shape = Shape.CROSS;
    @Setting(value = "Size", min = 1, max = 20) public int size = 5;
    @Setting(value = "Thickness", min = 1, max = 4) public int thickness = 1;
    @Setting(value = "Gap", min = 0, max = 12) public int gap = 3;
    @Setting(value = "Color", color = true) public int color = 0xFFFFFFFF;
    @Setting(value = "Alpha", min = 0.1f, max = 1f) public float alpha = 1f;
    @Setting("Outline") public boolean outline = true;
    @Setting(value = "Outline color", color = true) public int outlineColor = 0xFF000000;
    @Setting("Dynamic bloom") public boolean dynamic = true;
    @Setting(value = "Attack bloom", min = 0, max = 10) public int attackBloom = 4;
    @Setting(value = "Move spread", min = 0, max = 8) public int moveSpread = 2;

    private transient float bloom, spread;
    private transient boolean prevAtk;
    private transient long last = System.nanoTime();

    public CrosshairModule() { super("crosshair", "Crosshair"); }

    @Override public boolean hasHud() { return false; }
    @Override protected void content(Gfx g, boolean preview) {}

    public void kick() { bloom = attackBloom; }

    @Override
    public void renderOverlay(Gfx g, int sw, int sh, boolean preview) {
        if (!enabled && !preview) return;
        dim = enabled ? 1f : 0.35f;
        Minecraft mc = Minecraft.getInstance();
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1e9f);
        last = now;

        LocalPlayer p = mc.player;
        if (dynamic && p != null) {
            boolean atk = mc.options.keyAttack.isDown();
            if (atk && !prevAtk) bloom = attackBloom;
            prevAtk = atk;
            bloom = Math.max(0f, bloom - dt * (6f + bloom * 4f));
            Vec3 dm = p.getDeltaMovement();
            float target = 0f;
            if (dm.x * dm.x + dm.z * dm.z > 0.0004) target += moveSpread * 0.5f;
            if (p.isSprinting()) target += moveSpread * 0.5f;
            if (!p.onGround()) target += moveSpread * 0.5f;
            spread += (target - spread) * Math.min(1f, dt * 12f);
        } else { bloom = 0; spread = 0; }

        int eg = gap + Math.round(dynamic ? bloom + spread : 0f);
        int cx = sw / 2, cy = sh / 2, t = thickness, half = t / 2;
        float am = alpha * opacity * dim;
        int col = Gfx.alpha(color, am), oc = Gfx.alpha(outlineColor, am);

        switch (shape) {
            case CROSS -> {
                if (outline) { arms(g, cx, cy, eg, half, t, oc, 1); }
                arms(g, cx, cy, eg, half, t, col, 0);
            }
            case DOT -> {
                int d = Math.max(1, size / 2);
                if (outline) g.rect(cx - d / 2 - 1, cy - d / 2 - 1, d + 2, d + 2, oc);
                g.rect(cx - d / 2, cy - d / 2, d, d, col);
            }
            case CIRCLE -> {
                int r = eg + size;
                if (outline) { ring(g, cx, cy, r + 1, t + 2, oc); }
                ring(g, cx, cy, r, t, col);
            }
        }
    }

    private void arms(Gfx g, int cx, int cy, int eg, int half, int t, int col, int pad) {
        int s = size;
        g.rect(cx - half - pad, cy - eg - s - pad, t + 2 * pad, s + 2 * pad, col);
        g.rect(cx - half - pad, cy + eg + 1 - pad, t + 2 * pad, s + 2 * pad, col);
        g.rect(cx - eg - s - pad, cy - half - pad, s + 2 * pad, t + 2 * pad, col);
        g.rect(cx + eg + 1 - pad, cy - half - pad, s + 2 * pad, t + 2 * pad, col);
    }

    private static void ring(Gfx g, int cx, int cy, int r, int t, int col) {
        int ri = r - t;
        for (int dy = -r; dy <= r; dy++) {
            int outer = (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy)));
            if (ri > 0 && Math.abs(dy) < ri) {
                int inner = (int) Math.round(Math.sqrt(Math.max(0, ri * ri - dy * dy)));
                g.fill(cx - outer, cy + dy, cx - inner, cy + dy + 1, col);
                g.fill(cx + inner + 1, cy + dy, cx + outer + 1, cy + dy + 1, col);
            } else {
                g.fill(cx - outer, cy + dy, cx + outer + 1, cy + dy + 1, col);
            }
        }
    }
}
