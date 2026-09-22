package com.sao.saomenu.client.menu;

import com.sao.saomenu.SAOMenu;
import com.sao.saomenu.SAOMenuPlatform;
import com.sao.saomenu.client.hud.HudLayoutEditor;
import com.sao.saomenu.client.hud.SAOHud;
import com.sao.saomenu.client.hud.SAOMapPanel;
import com.sao.saomenu.config.SAOConfig;
import com.sao.saomenu.client.input.SAOMenuMovement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;

/**
 * SAO 菜单屏:Screen / MenuHost 适配器。状态在 {@link MenuSession},
 * 仿射在 {@link MenuTransform},绘制与输入在同包协作者。
 */
public class SAOMenuScreen extends Screen implements MenuHost {

    private final MenuSession session = new MenuSession();
    private final MenuTransform transform = new MenuTransform();
    private final HudLayoutEditor hudEditor = new HudLayoutEditor();
    private final float[] previewPt = new float[2];

    public SAOMenuScreen() {
        super(Component.translatable("saomenu.title"));
    }

    Font menuFont() {
        return this.font;
    }

    @Override
    protected void init() {
        session.onInit(this.width, this.height);
        playLauncher();
        SAOMenu.LOGGER.info("[SAOMenu] gui size {}x{} anchor {}x{} (fixed)",
                this.width, this.height, session.baseAnchorX, session.baseAnchorY);
    }

    /** 个人面板一级项数量(预览自检复用,直接问注册表,避免与面板定义脱钩)。 */
    public static int profileItemCount() {
        SaoPanel p = SaoMenuRegistry.byId(SaoPanels.PROFILE);
        return p == null ? 0 : p.items().get().size();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * 世界空间菜单板的显示强度:开合动画期间从 0 渐变到 1 再回落,
     * 与 HUD 菜单的开合节奏同源。
     */
    public float worldMenuAlpha() {
        return session.worldMenuAlpha();
    }

    /** 世界空间菜单板当前应高亮的主按钮;菜单收起或未选择时 -1。 */
    public int worldMenuMain() {
        return session.worldMenuMain();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        long now = MenuSession.now();
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null) {
            hudEditor.cancel();
            super.onClose();
            return;
        }

        SAOMenuMovement.pollKeys(mc, session.blocksMovement());

        if (session.closeFinished(now)) {
            hudEditor.cancel();
            super.onClose();
            return;
        }

        float openP = session.openP(now);
        float closeP = session.closeP(now);
        float globalAlpha = session.globalAlpha(now);
        int main = session.activeMain();
        int layoutAy = session.buttonY(Math.max(0, main), this.height);

        transform.update(this.width, this.height, session.baseAnchorX, layoutAy,
                session.childShiftTarget(this.height), mouseX, mouseY, now, session.closing, openP, closeP);

        if (!session.closing) {
            transform.toLocal(mouseX, mouseY);
            session.updateHovers(transform, this.width, this.height);
        }

        if (main >= 0 && !session.closing) {
            session.switchPanelIfChanged(main);
        }

        SAOHud.renderMenuUnderlay(g, mc, this.width, this.height, globalAlpha);

        var pose = g.pose();
        pose.pushPose();
        transform.apply(pose);
        if (main >= 0) {
            MenuCards.render(g, this, session, transform, main, mouseX, mouseY, globalAlpha, now);
            MenuColumns.renderItems(g, this, session, globalAlpha, now);
        }
        MenuColumns.renderMainButtons(g, this, session, globalAlpha);
        pose.popPose();

        MenuColumns.renderTooltip(g, this, session, mouseX, mouseY);
        SAOHud.renderMenuOverlay(g, mc, this.width, this.height, globalAlpha, mouseX, mouseY);
        MenuDialogs.render(g, this, session, mouseX, mouseY, globalAlpha, now);
        MenuColumns.renderPinGhost(g, this, session, transform, now);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        MenuInput.mouseMoved(this, session, hudEditor, mouseX, mouseY);
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (MenuInput.mouseReleased(this, session, transform, hudEditor, mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (session.closing) {
            return false;
        }
        if (MenuInput.mouseClicked(this, session, transform, hudEditor, mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        MenuInput.beginClose(this, session);
    }

    @Override
    public void removed() {
        hudEditor.cancel();
        SAOMenuMovement.releaseKeys(Minecraft.getInstance());
        super.removed();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (MenuInput.keyPressed(this, session, Minecraft.getInstance(), keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (SAOMenuMovement.setKeyState(Minecraft.getInstance(), keyCode, scanCode, false)) {
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    /** 移动是否被暂时封锁(确认弹窗/信息弹窗/关闭动画)。供 Mixin 输入接管读取。 */
    public boolean isMovementBlocked() {
        return session.blocksMovement();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (MenuInput.mouseScrolled(this, session, transform, mouseX, mouseY, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public Screen screen() {
        return this;
    }

    @Override
    public void selectMain(int index) {
        session.selectMain(index);
    }

    @Override
    public void openCloseConfirm() {
        session.openConfirm();
        SAOMenuMovement.releaseKeys(Minecraft.getInstance());
        playAlert();
    }

    @Override
    public void toggleMap() {
        SAOMapPanel.toggle();
    }

    @Override
    public MenuLayout.Rect localBoxToScreen(int lx, int ly, int w, int h) {
        return transform.boxToScreen(lx, ly, w, h);
    }

    @Override
    public float[] screenPointOf(float lx, float ly) {
        transform.toScreen(lx, ly, previewPt);
        return new float[]{previewPt[0], previewPt[1]};
    }

    private void playLauncher() {
        if (!SAOConfig.sounds()) {
            return;
        }
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SAOMenuPlatform.launcherSound(), 1.0F));
    }

    @Override
    public void playClick() {
        if (!SAOConfig.sounds()) {
            return;
        }
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SAOMenuPlatform.clickSound(), 1.0F));
    }

    @Override
    public void playPanel() {
        if (!SAOConfig.sounds()) {
            return;
        }
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SAOMenuPlatform.panelSound(), 1.0F));
    }

    @Override
    public void playAlert() {
        if (!SAOConfig.sounds()) {
            return;
        }
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SAOMenuPlatform.alertSound(), 1.0F));
    }
}
