package com.sao.saomenu.client.hud;

import com.sao.saomenu.SAOMenuPlatform;
import com.sao.saomenu.config.SAOConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

/**
 * 菜单内 HUD 拖动/图钉交互。坐标是 GUI 屏幕像素,不是菜单组局部坐标。
 *
 * <p>拖动会话只在本实例上,松手才 {@link SAOConfig#save()};{@link #cancel()}
 * 恢复按下时的锚点且不写盘。</p>
 */
public final class HudLayoutEditor {

    private enum Handle {
        CLOCK, SKILL, PLATE, FOOD, MAP
    }

    private static HudLayoutEditor live;

    private Handle handle;
    private float grabFx;
    private float grabFy;
    private float origX;
    private float origY;
    private boolean moved;

    public boolean mouseClicked(Minecraft mc, int width, int height, double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        int mx = (int) mouseX;
        int my = (int) mouseY;
        boolean hud = SAOConfig.showHud();
        if (hud && SAOClockPanel.hitCard(width, height, mx, my)) {
            begin(Handle.CLOCK, SAOConfig.clockPanelX(), SAOConfig.clockPanelY(),
                    (mx - SAOClockPanel.panelX(width)) / (float) Math.max(1, SAOClockPanel.panelW()),
                    (my - SAOClockPanel.panelY(height)) / (float) Math.max(1, SAOClockPanel.panelH()));
            return true;
        }
        if (hud && SaoSkillBar.hitSkillBar(width, height, mx, my)) {
            int bw = SaoSkillBar.barWidth(height, SaoSkillBar.slotCount());
            int bh = SaoSkillBar.slotSize(height);
            begin(Handle.SKILL, SAOConfig.skillBarX(), SAOConfig.skillBarY(),
                    (mx - SaoSkillBar.barX(width, bw)) / (float) Math.max(1, bw),
                    (my - SaoSkillBar.barY(height, bh)) / (float) Math.max(1, bh));
            return true;
        }
        if (hud && SAOPlayerPlate.hit(mc, width, height, mx, my)) {
            int pw = SAOPlayerPlate.plateW(width);
            int gh = SAOPlayerPlate.plateGroupH(width, mc);
            begin(Handle.PLATE, SAOConfig.platePanelX(), SAOConfig.platePanelY(),
                    (mx - SAOPlayerPlate.plateX(width)) / (float) Math.max(1, pw),
                    (my - SAOPlayerPlate.plateY(height)) / (float) Math.max(1, gh));
            return true;
        }
        if (SAOFoodBar.hit(width, height, mx, my)) {
            begin(Handle.FOOD, SAOConfig.foodPanelX(), SAOConfig.foodPanelY(),
                    (mx - SAOFoodBar.foodX(width)) / (float) SAOFoodBar.FOOD_W,
                    (my - SAOFoodBar.foodY(height)) / (float) SAOFoodBar.FOOD_H);
            return true;
        }
        if (SAOMapPanel.isShown()) {
            if (SAOMapPanel.hitPin(width, height, mx, my)) {
                SAOMapPanel.togglePin();
                playClick(mc);
                return true;
            }
            if (SAOMapPanel.hitCard(width, height, mx, my)) {
                begin(Handle.MAP, SAOConfig.mapPanelX(), SAOConfig.mapPanelY(),
                        (mx - SAOMapPanel.cardX(width, height)) / (float) Math.max(1, SAOMapPanel.panelW(height)),
                        (my - SAOMapPanel.cardY(width, height)) / (float) Math.max(1, SAOMapPanel.panelH(height)));
                return true;
            }
        }
        return false;
    }

    public void mouseMoved(Minecraft mc, int width, int height, double mouseX, double mouseY) {
        if (handle == null) {
            return;
        }
        int mx = (int) mouseX;
        int my = (int) mouseY;
        switch (handle) {
            case CLOCK -> SAOClockPanel.moveTo(width, height, grabFx, grabFy, mx, my);
            case SKILL -> SaoSkillBar.moveTo(width, height, grabFx, grabFy, mx, my);
            case PLATE -> SAOPlayerPlate.moveTo(mc, width, height, grabFx, grabFy, mx, my);
            case FOOD -> SAOFoodBar.moveTo(width, height, grabFx, grabFy, mx, my);
            case MAP -> SAOMapPanel.moveTo(width, height, grabFx, grabFy, mx, my);
        }
        moved = true;
    }

    public boolean mouseReleased(Minecraft mc, int width, int height, double mouseX, double mouseY, int button) {
        if (handle == null) {
            return false;
        }
        if (moved) {
            SAOConfig.save();
        }
        clear();
        return true;
    }

    /** 放弃未落盘的拖动:锚点回到按下时,不写配置。 */
    public void cancel() {
        if (handle != null) {
            restore();
        }
        clear();
    }

    static void dropUnsaved() {
        if (live != null) {
            live.cancel();
        }
    }

    private void begin(Handle next, float ox, float oy, float gx, float gy) {
        live = this;
        handle = next;
        origX = ox;
        origY = oy;
        grabFx = gx;
        grabFy = gy;
        moved = false;
    }

    private void restore() {
        switch (handle) {
            case CLOCK -> {
                SAOConfig.setClockPanelX(origX);
                SAOConfig.setClockPanelY(origY);
            }
            case SKILL -> {
                SAOConfig.setSkillBarX(origX);
                SAOConfig.setSkillBarY(origY);
            }
            case PLATE -> {
                SAOConfig.setPlatePanelX(origX);
                SAOConfig.setPlatePanelY(origY);
            }
            case FOOD -> {
                SAOConfig.setFoodPanelX(origX);
                SAOConfig.setFoodPanelY(origY);
            }
            case MAP -> {
                SAOConfig.setMapPanelX(origX);
                SAOConfig.setMapPanelY(origY);
            }
        }
    }

    private void clear() {
        handle = null;
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
