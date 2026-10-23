package dev.client26.hud.modules;

import dev.client26.hud.AbstractHudModule;
import dev.client26.hud.Setting;
import dev.client26.util.Gfx;

public final class ViewmodelModule extends AbstractHudModule {
    @Setting(value = "Offset X", min = -1f, max = 1f) public float offsetX = 0f;
    @Setting(value = "Offset Y", min = -1f, max = 1f) public float offsetY = 0f;
    @Setting(value = "Offset Z", min = -1f, max = 1f) public float offsetZ = 0f;
    @Setting(value = "Item scale", min = 0.3f, max = 1.5f) public float itemScale = 1f;
    @Setting(value = "FOV scale (holding item)", min = 0.6f, max = 1.4f) public float fovScale = 1f;
    @Setting(value = "Swing speed", min = 0.25f, max = 3f) public float swingSpeed = 1f;

    public ViewmodelModule() { super("viewmodel", "Viewmodel"); enabled = false; }
    @Override public boolean hasHud() { return false; }
    @Override public void renderOverlay(Gfx g, int sw, int sh, boolean preview) {}
    @Override protected void content(Gfx g, boolean preview) {}
}
