package dev.client26.hud.modules;

import dev.client26.hud.AbstractHudModule;
import dev.client26.hud.Setting;
import dev.client26.util.Gfx;

public final class LegacyModule extends AbstractHudModule {
    @Setting("1.8 block-hit pose") public boolean blockHit = true;
    @Setting("Swing while using item") public boolean swingWhileUsing = true;
    @Setting("Low fire overlay") public boolean lowFire = true;
    @Setting(value = "Fire height", min = 0f, max = 0.5f) public float fireOffset = 0.25f;

    public LegacyModule() { super("legacy", "Legacy Visuals"); enabled = false; }
    @Override public boolean hasHud() { return false; }
    @Override public void renderOverlay(Gfx g, int sw, int sh, boolean preview) {}
    @Override protected void content(Gfx g, boolean preview) {}
}
