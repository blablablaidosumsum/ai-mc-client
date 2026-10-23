package dev.client26.hud.modules;

import dev.client26.hud.AbstractHudModule;
import dev.client26.hud.Setting;
import dev.client26.util.Gfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;

public final class ArmorStatusModule extends AbstractHudModule {
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

    @Setting("Armor") public boolean showArmor = true;
    @Setting("Durability numbers") public boolean showNumbers = true;
    @Setting("Held item + count") public boolean showHeld = true;
    @Setting("Potion timers") public boolean showEffects = true;

    private final transient ItemStack[] stacks = new ItemStack[5];
    private final transient String[] durText = new String[5];
    private final transient int[] durColor = new int[5];
    private final transient String[] effText = new String[8];
    private transient int effCount;
    private transient String heldText = "";

    public ArmorStatusModule() {
        super("armor", "Armor & Status");
        x = 0.90f; y = 0.30f;
        java.util.Arrays.fill(stacks, ItemStack.EMPTY);
    }

    @Override
    public void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null) return;
        for (int i = 0; i < 4; i++) {
            ItemStack s = p.getItemBySlot(SLOTS[i]);
            stacks[i] = s;
            durInfo(i, s);
        }
        ItemStack held = p.getMainHandItem();
        stacks[4] = held;
        durInfo(4, held);
        if (!held.isEmpty() && held.getMaxStackSize() > 1) {
            int total = 0, n = p.getInventory().getContainerSize();
            for (int i = 0; i < n; i++) {
                ItemStack s = p.getInventory().getItem(i);
                if (ItemStack.isSameItemSameComponents(held, s)) total += s.getCount();
            }
            heldText = "x" + total;
        } else heldText = "";

        effCount = 0;
        Collection<MobEffectInstance> effects = p.getActiveEffects();
        for (MobEffectInstance e : effects) {
            if (effCount >= effText.length) break;
            String time;
            if (e.isInfiniteDuration()) time = "**";
            else {
                int sec = e.getDuration() / 20;
                time = (sec / 60) + ":" + (sec % 60 < 10 ? "0" : "") + (sec % 60);
            }
            effText[effCount++] = e.getEffect().value().getDisplayName().getString()
                    + (e.getAmplifier() > 0 ? " " + (e.getAmplifier() + 1) : "") + " " + time;
        }
    }

    private void durInfo(int i, ItemStack s) {
        if (s.isEmpty() || !s.isDamageableItem()) { durText[i] = null; return; }
        int left = s.getMaxDamage() - s.getDamageValue();
        float f = left / (float) s.getMaxDamage();
        durText[i] = Integer.toString(left);
        durColor[i] = f > 0.5f ? 0xFF55FF55 : f > 0.25f ? 0xFFFFFF55 : 0xFFFF5555;
    }

    @Override
    protected void content(Gfx g, boolean preview) {
        int yy = 0, maxW = 40;
        if (showArmor) {
            for (int i = 0; i < 4; i++) {
                ItemStack s = stacks[i];
                if (s.isEmpty()) continue;
                g.item(s, 0, yy);
                g.itemDecorations(s, 0, yy);
                if (showNumbers && durText[i] != null) {
                    g.text(durText[i], 20, yy + 5, c(durColor[i]));
                    maxW = Math.max(maxW, 20 + g.width(durText[i]));
                }
                yy += 18;
            }
        }
        if (showHeld && !stacks[4].isEmpty()) {
            g.item(stacks[4], 0, yy);
            g.itemDecorations(stacks[4], 0, yy);
            String t = durText[4] != null && showNumbers ? durText[4] : heldText;
            if (!t.isEmpty()) {
                g.text(t, 20, yy + 5, c(durText[4] != null ? durColor[4] : 0xFFFFFFFF));
                maxW = Math.max(maxW, 20 + g.width(t));
            }
            yy += 18;
        }
        if (showEffects) {
            for (int i = 0; i < effCount; i++) {
                g.text(effText[i], 0, yy + 1, c(0xFFFFFFFF));
                maxW = Math.max(maxW, g.width(effText[i]));
                yy += 11;
            }
        }
        w = maxW;
        h = Math.max(18, yy);
        if (preview && yy == 0) g.text("Armor / Status", 0, 4, c(0xFFAAAAAA));
    }
}
