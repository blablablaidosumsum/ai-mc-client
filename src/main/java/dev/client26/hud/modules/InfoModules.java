package dev.client26.hud.modules;

import dev.client26.hud.AbstractHudModule;
import dev.client26.hud.Setting;
import dev.client26.util.Gfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.util.Mth;

public final class InfoModules {
    private InfoModules() {}

    public static final class Fps extends AbstractHudModule {
        @Setting("Background") public boolean background = true;
        @Setting(value = "Color", color = true) public int color = 0xFFFFFFFF;
        private transient int lastFps = -1;
        private transient String text = "";

        public Fps() { super("fps", "FPS"); x = 0.01f; y = 0.01f; }

        @Override protected void content(Gfx g, boolean preview) {
            int f = Minecraft.getInstance().getFps();
            if (f != lastFps) { lastFps = f; text = f + " FPS"; }
            textBox(g, text, color, background);
        }
    }

    public static final class Ping extends AbstractHudModule {
        @Setting("Background") public boolean background = true;
        private transient int ms = -1, lastMs = -2;
        private transient String text = "";

        public Ping() { super("ping", "Ping"); x = 0.01f; y = 0.07f; }

        @Override public void tick(Minecraft mc) {
            if (mc.getConnection() == null || mc.player == null) return;
            PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            ms = info == null ? 0 : info.getLatency();
        }

        @Override protected void content(Gfx g, boolean preview) {
            if (ms != lastMs) { lastMs = ms; text = ms + " ms"; }
            int col = ms < 60 ? 0xFF55FF55 : ms < 120 ? 0xFFFFFF55 : 0xFFFF5555;
            textBox(g, text, col, background);
        }
    }

    public static final class Coords extends AbstractHudModule {
        @Setting("Background") public boolean background = true;
        @Setting(value = "Color", color = true) public int color = 0xFFFFFFFF;
        private transient int cx, cy, cz, lx = Integer.MIN_VALUE, ly, lz;
        private transient String text = "";

        public Coords() { super("coords", "Coordinates"); x = 0.01f; y = 0.13f; }

        @Override public void tick(Minecraft mc) {
            cx = Mth.floor(mc.player.getX());
            cy = Mth.floor(mc.player.getY());
            cz = Mth.floor(mc.player.getZ());
        }

        @Override protected void content(Gfx g, boolean preview) {
            if (cx != lx || cy != ly || cz != lz) {
                lx = cx; ly = cy; lz = cz;
                text = "XYZ: " + cx + " / " + cy + " / " + cz;
            }
            textBox(g, text, color, background);
        }
    }

    public static final class Compass extends AbstractHudModule {
        private static final String[] DIR = { "S", "SW", "W", "NW", "N", "NE", "E", "SE" };
        private static final String[] DEG = new String[360];
        static { for (int i = 0; i < 360; i++) DEG[i] = i + "\u00B0"; }

        @Setting("Background") public boolean background = true;
        @Setting(value = "Color", color = true) public int color = 0xFFFFFFFF;

        public Compass() { super("compass", "Compass"); x = 0.31f; y = 0.01f; w = 180; h = 22; }

        @Override protected void content(Gfx g, boolean preview) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            w = 180; h = 22;
            float yaw = Mth.wrapDegrees(mc.player.getYRot());
            if (yaw < 0) yaw += 360f;
            if (background) g.rect(0, 0, w, 12, c(0x66000000));
            int start = (int) Math.floor((yaw - 90f) / 15f) * 15;
            for (int a = start; a <= yaw + 90f; a += 15) {
                float off = a - yaw;
                int px = 90 + Math.round(off);
                float fade = 1f - Math.abs(off) / 95f;
                int norm = ((a % 360) + 360) % 360;
                if (norm % 45 == 0) {
                    boolean card = norm % 90 == 0;
                    g.textCentered(DIR[norm / 45], px, 2, Gfx.alpha(c(card ? 0xFFFFFFFF : color), fade));
                } else {
                    g.rect(px, 4, 1, 4, Gfx.alpha(c(0xAAFFFFFF), fade));
                }
            }
            g.rect(90, 12, 1, 3, c(0xFFFF5555));
            g.textCentered(DEG[Math.min(359, Math.round(yaw) % 360)], 90, 14, c(color));
        }
    }
}
