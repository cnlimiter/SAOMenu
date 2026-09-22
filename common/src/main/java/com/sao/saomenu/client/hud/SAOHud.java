package com.sao.saomenu.client.hud;

import com.sao.saomenu.api.SaoUiRegistry;
import com.sao.saomenu.client.menu.SAOMenuScreen;
import com.sao.saomenu.client.render.target.SAOTargetBar3D;
import com.sao.saomenu.config.SAOConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

/**
 * 常驻 SAO HUD 入口:平台钩子、菜单两层组合、会话 tick/复位。
 *
 * <p>元件绘制由 {@link HudComposer} 遍历冻结注册表,菜单与世界不再各写一份清单。</p>
 */
public final class SAOHud {

    private SAOHud() {
    }

    /** 把内置 HUD 元件写入同一套注册表;由客户端 setup 在 freeze 前调用一次。 */
    public static void registerBuiltins(SaoUiRegistry registry) {
        HudBuiltins.register(registry);
    }

    /** 常驻渲染入口(平台 HUD 钩子调用)。 */
    public static void render(GuiGraphics g, Minecraft mc) {
        if (mc.options.hideGui) {
            return;
        }
        SAOTargetBar3D.renderLabels(g, mc);
        if (!(mc.screen instanceof SAOMenuScreen)) {
            HudComposer.world(g, mc);
        }
        HudComposer.gameOverlay(g, mc);
    }

    public static void renderMenuUnderlay(GuiGraphics g, Minecraft mc, int width, int height, float alpha) {
        HudComposer.menuUnderlay(g, mc, width, height, alpha);
    }

    public static void renderMenuOverlay(GuiGraphics g, Minecraft mc, int width, int height, float alpha,
                                         int mouseX, int mouseY) {
        HudComposer.menuOverlay(g, mc, width, height, alpha, mouseX, mouseY);
    }

    /**
     * 居中的原版饥饿条(由 GuiMixin 取消 renderPlayerHealth 后代画)。
     *
     * <p>原版血条/护甲行被取消,坐骑血条(renderVehicleHealth)是独立方法不受影响。</p>
     */
    public static void renderVanillaFoodCentered(GuiGraphics g, Player p) {
        SAOFoodBar.render(g, p);
    }

    /** 通知/血量事件检测;从 render 挪到 tick,避免每帧做状态跳变。 */
    public static void clientTick(Minecraft mc) {
        if (mc == null) {
            return;
        }
        SAOCombatHud.tick(mc);
        if (mc.player != null && SAOConfig.showHud()) {
            SAOPlayerPlate.detectEvents(mc.player);
        }
    }

    /**
     * 清战斗反馈、通知队列与未落盘拖动。不写配置,也不释放地图动态纹理
     * ({@link SAOMapPanel#reset()} / {@link SAOBossBanner#reset()} 仍是子系统入口)。
     */
    public static void resetSession() {
        SAOCombatHud.reset();
        SAOPlayerPlate.resetEvents();
        SAONotification.clear();
        HudLayoutEditor.dropUnsaved();
    }
}
