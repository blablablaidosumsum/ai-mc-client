package dev.client26.hud.modules;

import dev.client26.hud.AbstractHudModule;
import dev.client26.hud.Setting;
import dev.client26.util.Gfx;
import net.minecraft.client.Minecraft;

public final class PerformanceModule extends AbstractHudModule {
    @Setting(value = "Entity render distance", min = 0.5f, max = 5f) public float entityDistance = 1.0f;
    private transient float applied = -1f;

    public PerformanceModule() { super("performance", "Performance"); enabled = false; }
    @Override public boolean hasHud() { return false; }
    @Override public void renderOverlay(Gfx g, int sw, int sh, boolean preview) {}
    @Override protected void content(Gfx g, boolean preview) {}

    @Override public void tick(Minecraft mc) {
        if (entityDistance != applied) {
            mc.options.entityDistanceScaling().set((double) entityDistance);
            applied = entityDistance;
        }
    }
}
