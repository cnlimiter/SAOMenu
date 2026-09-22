package com.sao.saomenu.client.hud;

import com.sao.saomenu.SAOMenuPlatform;
import com.sao.saomenu.api.SaoUi;
import com.sao.saomenu.api.hud.HudBox;
import com.sao.saomenu.api.hud.HudElement;
import com.sao.saomenu.api.hud.HudLayoutBinding;
import com.sao.saomenu.api.hud.HudPass;
import com.sao.saomenu.config.SAOConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

import java.util.List;

/**
 * 菜单内 HUD 拖动/图钉交互。坐标是 GUI 屏幕像素,不是菜单组局部坐标。
 *
 * <p>拖动会话只在本实例上,左键松手才 {@link HudLayoutBinding#save()};{@link #cancel()}
 * 恢复按下时的锚点且不写盘。非左键松开不得结束左键拖动。</p>
 */
public final class HudLayoutEditor {

    private static final int MAP_PIN_PRIORITY = 15;

    private static HudLayoutEditor live;

    private final List<HudElement> override;
    private final HudFrame hitFrame = new HudFrame();

    private HudLayoutBinding binding;
    private float grabFx;
    private float grabFy;
    private float origX;
    private float origY;
    private boolean moved;

    public HudLayoutEditor() {
        this.override = null;
    }

    HudLayoutEditor(List<HudElement> override) {
        this.override = override;
    }

    public boolean mouseClicked(Minecraft mc, int width, int height, double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        int mx = (int) mouseX;
        int my = (int) mouseY;
        hitFrame.begin(null, mc, width, height, 1f, 1f, HudPass.MENU_OVERLAY, mx, my);
        List<HudElement> elements = elements();
        HudLayoutBinding best = null;
        int bestPri = Integer.MIN_VALUE;
        boolean pinHit = false;
        for (int i = 0, n = elements.size(); i < n; i++) {
            HudElement element = elements.get(i);
            if (!element.visible(hitFrame)) {
                continue;
            }
            if (HudBuiltins.MAP.equals(element.id())
                    && SAOMapPanel.isShown()
                    && SAOMapPanel.hitPin(width, height, mx, my)
                    && MAP_PIN_PRIORITY > bestPri) {
                bestPri = MAP_PIN_PRIORITY;
                pinHit = true;
                best = null;
            }
            HudLayoutBinding layout = element.layout();
            if (layout == null || !layout.hit(mc, width, height, mx, my)) {
                continue;
            }
            if (layout.priority() > bestPri) {
                bestPri = layout.priority();
                pinHit = false;
                best = layout;
            }
        }
        hitFrame.clear();
        if (pinHit) {
            SAOMapPanel.togglePin();
            playClick(mc);
            return true;
        }
        if (best == null) {
            return false;
        }
        HudBox box = best.box(mc, width, height);
        float gx = box.width() <= 0 ? 0f : (mx - box.x()) / (float) box.width();
        float gy = box.height() <= 0 ? 0f : (my - box.y()) / (float) box.height();
        begin(best, best.anchorX(), best.anchorY(), gx, gy);
        return true;
    }

    public void mouseMoved(Minecraft mc, int width, int height, double mouseX, double mouseY) {
        if (binding == null) {
            return;
        }
        binding.moveTo(mc, width, height, grabFx, grabFy, (int) mouseX, (int) mouseY);
        moved = true;
    }

    public boolean mouseReleased(Minecraft mc, int width, int height, double mouseX, double mouseY, int button) {
        if (binding == null) {
            return false;
        }
        if (button != 0) {
            return false;
        }
        if (moved) {
            binding.save();
        }
        clear();
        return true;
    }

    /** 放弃未落盘的拖动:锚点回到按下时,不写配置。 */
    public void cancel() {
        if (binding != null) {
            binding.restore(origX, origY);
        }
        clear();
    }

    /** Roll back the pending transaction before level-change listeners observe the new session. */
    public static void dropUnsaved() {
        if (live != null) {
            live.cancel();
        }
    }

    private List<HudElement> elements() {
        return override != null ? override : SaoUi.hudElements();
    }

    private void begin(HudLayoutBinding next, float ox, float oy, float gx, float gy) {
        live = this;
        binding = next;
        origX = ox;
        origY = oy;
        grabFx = gx;
        grabFy = gy;
        moved = false;
    }

    private void clear() {
        binding = null;
        moved = false;
        if (live == this) {
            live = null;
        }
    }

    private static void playClick(Minecraft mc) {
        if (!SAOConfig.sounds() || mc == null) {
            return;
        }
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SAOMenuPlatform.clickSound(), 1.0F));
    }
}
