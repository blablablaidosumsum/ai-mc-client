package dev.client26.mixin;

import dev.client26.hud.ModuleManager;
import dev.client26.hud.modules.ViewmodelModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "getCurrentSwingDuration", at = @At("RETURN"), cancellable = true, require = 0)
    private void client26$swingSpeed(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this != Minecraft.getInstance().player) return;
        ViewmodelModule v = ModuleManager.I.viewmodel;
        if (!v.enabled || v.swingSpeed == 1f) return;
        cir.setReturnValue(Math.max(1, Math.round(cir.getReturnValue() / v.swingSpeed)));
    }
}
