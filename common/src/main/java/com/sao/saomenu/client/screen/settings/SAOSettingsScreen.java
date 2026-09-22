package com.sao.saomenu.client.screen.settings;

import com.sao.saomenu.SAOMenuPlatform;
import com.sao.saomenu.config.SAOConfig;
import com.sao.saomenu.ui.theme.SaoTheme;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * 女神异闻录(P5)风格模组设置界面,以「设置背景.mp4」帧动画为背景:
 *
 * <ul>
 *   <li>背景:视频帧贴图集({@code settings_bg.png},480x270@15fps,16 列网格)按
 *       播放状态机取帧——打开界面 4 倍速扫入桐人眼睛特写(帧 {@link SettingsTimeline#BASE_FRAME},
 *       视频 t≈5s)后定格,点击分类从定格处继续正播到亚斯娜脸特写
 *       (帧 {@link SettingsTimeline#END_FRAME},视频 t≈10s)停住,点击返回从当前帧
 *       倒放回定格点;全屏 cover 拉伸 + 轻微渐变叠加,尽量露出背景</li>
 *   <li>双主题:根页面为桐谷和人蓝白系,点击分类进入设置页后整体切换为
 *       亚斯娜粉白系(与背景从蓝色章节进入紫色章节同步),返回时切回蓝白</li>
 *   <li>根页面:4 个阶梯排布的分类条目(布局/战斗/界面/主题)从左侧级联滑入,
 *       平时只有色条+大字,不遮挡背景;悬停时半透明斜板展开、文字变亮</li>
 *   <li>点击分类:主题色斜切色带扫过全屏(转场),色带移开时设置行带过冲
 *       逐条弹出;返回:反向转场,分类条目从左侧滑回</li>
 * </ul>
 *
 * <p>覆盖 {@link SAOConfig} 的全部界面配置项,
 * 修改即时生效并持久化到 {@code config/saomenu.json}。</p>
 */
public class SAOSettingsScreen extends Screen {

    private final Screen lastScreen;
    private final SettingsTimeline timeline = new SettingsTimeline();
    private final SettingsSkin skin = new SettingsSkin();

    public SAOSettingsScreen(Screen lastScreen) {
        super(Component.translatable("saomenu.settings.title"));
        this.lastScreen = lastScreen;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        timeline.initOnce(Util.getMillis());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        long now = Util.getMillis();
        float dt = timeline.tickDt(now);
        skin.bind(this.font, this.width, this.height);

        timeline.renderVideoBackground(g, this.width, this.height, now);
        skin.renderScrim(g);
        skin.renderDecorStripes(g, timeline, now);
        skin.updateHovers(timeline, dt, mouseX, mouseY);

        boolean inTransition = timeline.inTransition();
        long trT = timeline.transitionElapsed(now);
        SettingsPage toRender = timeline.pageToRender(now);
        skin.renderPage(g, timeline, toRender, mouseX, mouseY, now, inTransition, trT);

        if (inTransition) {
            skin.renderWipe(g, timeline, trT);
            if (timeline.tick(now)) {
                playPanel();
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (timeline.inTransition()) {
            return true;
        }
        skin.bind(this.font, this.width, this.height);
        int mx = (int) mouseX;
        int my = (int) mouseY;
        long now = Util.getMillis();

        if (skin.hitReset(mx, my)) {
            SAOConfig.reset();
            saveNow();
            playClick();
            return true;
        }
        if (skin.hitDone(mx, my)) {
            playClick();
            onClose();
            return true;
        }

        if (timeline.page() == SettingsPage.ROOT) {
            int cat = skin.hitCategory(mx, my);
            if (cat >= 0) {
                timeline.beginEnterCategory(cat, now);
                playClick();
                return true;
            }
        } else {
            if (skin.backHovered(mx, my)) {
                timeline.beginReturnToRoot(now);
                playClick();
                return true;
            }
            if (timeline.page() == SettingsPage.THEME && skin.applyPresetClick(timeline.page(), mx, my)) {
                saveNow();
                playClick();
                return true;
            }
            SettingsPage page = timeline.page();
            for (int i = 0; i < SettingsCatalog.rowCount(page); i++) {
                OptionSpec spec = SettingsCatalog.spec(page, i);
                if (spec != null && spec.kind() == OptionSpec.Kind.PRESET) {
                    continue;
                }
                if (!skin.rowHovered(page, i, mx, my)) {
                    continue;
                }
                if (spec != null && spec.isSlider()) {
                    skin.beginSliderDrag(i);
                    skin.applySliderAt(page, i, mx);
                    saveNow();
                } else {
                    skin.flipToggle(page, i);
                    saveNow();
                }
                playClick();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (skin.dragRow() >= 0 && !timeline.inTransition()) {
            skin.bind(this.font, this.width, this.height);
            skin.applySliderAt(timeline.page(), skin.dragRow(), (int) mouseX);
            saveNow();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        skin.endDrag();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** 仅供预览自检:当前色相下会被高亮的预设 id(null = 色相被自定义,不指向任何预设)。 */
    public String debugHighlightedPreset() {
        return SaoTheme.matchingPreset(SAOConfig.accentHue());
    }

    /**
     * 仅供预览自检:上一次渲染预设行时,<b>每个色块实际用到的</b> id、是否选中、以及它的 x。
     *
     * <p>记录的是渲染当时的取值而不是另算一遍,所以据此裁图/取样必然对得上真东西。</p>
     */
    public String debugLastPresetRow() {
        return skin.lastPresetDebug();
    }

    /** 仅供预览自检:直接切到某个分类页(或 ROOT),绕开分类按钮的悬停与转场状态机以保证可复现。 */
    public void debugShowPage(String name) {
        SettingsPage want = SettingsPage.named(name);
        if (want == null) {
            return;
        }
        timeline.debugShowPage(want, Util.getMillis());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_O) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        saveNow();
        if (this.minecraft != null) {
            this.minecraft.setScreen(lastScreen);
        }
    }

    private void saveNow() {
        SAOConfig.save();
    }

    private void playClick() {
        if (SAOConfig.sounds()) {
            Minecraft.getInstance().getSoundManager()
                    .play(SimpleSoundInstance.forUI(SAOMenuPlatform.clickSound(), 1.0F));
        }
    }

    private void playPanel() {
        if (SAOConfig.sounds()) {
            Minecraft.getInstance().getSoundManager()
                    .play(SimpleSoundInstance.forUI(SAOMenuPlatform.panelSound(), 1.0F));
        }
    }
}
