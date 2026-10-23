package dev.client26.hud.modules;

import dev.client26.hud.AbstractHudModule;
import dev.client26.hud.Setting;
import dev.client26.util.Gfx;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ComboReachModule extends AbstractHudModule {
    @Setting("Show combo") public boolean showCombo = true;
    @Setting("Show reach") public boolean showReach = true;
    @Setting(value = "Combo timeout (ms)", min = 500, max = 5000) public int timeoutMs = 2500;

    private transient int combo, prevHurt;
    private transient long lastHit;
    private transient String comboText = "Combo: 0", reachText = "";

    public ComboReachModule() { super("combo", "Combo & Reach"); x = 0.55f; y = 0.52f; }

    public static void registerEvents(ComboReachModule m) {
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            Minecraft mc = Minecraft.getInstance();
            if (player == mc.player && entity instanceof LivingEntity le && le.isAlive()) m.onHit(player.getEyePosition(), entity.getBoundingBox());
            return InteractionResult.PASS;
        });
    }

    private void onHit(Vec3 eye, AABB bb) {
        double dx = Math.max(Math.max(bb.minX - eye.x, 0), eye.x - bb.maxX);
        double dy = Math.max(Math.max(bb.minY - eye.y, 0), eye.y - bb.maxY);
        double dz = Math.max(Math.max(bb.minZ - eye.z, 0), eye.z - bb.maxZ);
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        combo++;
        lastHit = System.currentTimeMillis();
        comboText = "Combo: " + combo;
        reachText = "Reach: " + (Math.round(d * 100.0) / 100.0);
    }

    @Override
    public void tick(Minecraft mc) {
        int hurt = mc.player.hurtTime;
        if (hurt > 0 && prevHurt == 0 && combo != 0) { combo = 0; comboText = "Combo: 0"; }
        prevHurt = hurt;
        if (combo > 0 && System.currentTimeMillis() - lastHit > timeoutMs) { combo = 0; comboText = "Combo: 0"; }
    }

    @Override
    protected void content(Gfx g, boolean preview) {
        boolean fresh = System.currentTimeMillis() - lastHit < 3000;
        int yy = 0, mw = 40;
        if (showCombo && (combo > 0 || preview)) {
            g.text(comboText, 0, yy, c(0xFFFFD84A)); mw = Math.max(mw, g.width(comboText)); yy += 10;
        }
        if (showReach && ((fresh && !reachText.isEmpty()) || preview)) {
            String t = reachText.isEmpty() ? "Reach: 0.00" : reachText;
            g.text(t, 0, yy, c(0xFF55FFFF)); mw = Math.max(mw, g.width(t)); yy += 10;
        }
        w = mw; h = Math.max(10, yy);
    }
}
