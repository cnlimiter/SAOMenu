package com.sao.saomenu.client.screen.settings;

import com.sao.saomenu.SAOMenuPlatform;
import com.sao.saomenu.api.settings.NativeSetting;
import com.sao.saomenu.api.settings.Setting;
import com.sao.saomenu.api.settings.SettingsGroup;
import com.sao.saomenu.api.settings.SliderSetting;
import com.sao.saomenu.api.theme.ThemeDefinition;
import com.sao.saomenu.api.SaoUi;
import com.sao.saomenu.api.layout.UiRect;
import com.sao.saomenu.api.widget.SaoTextField;
import com.sao.saomenu.config.SAOConfig;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 *   <li>根页面:登记的 {@link SettingsGroup} 阶梯排布,超出四条时滚动;
 *       平时只有色条+大字,不遮挡背景;悬停时半透明斜板展开、文字变亮</li>
 *   <li>点击分类:主题色斜切色带扫过全屏(转场),色带移开时设置行带过冲
 *       逐条弹出;返回:反向转场,分类条目从左侧滑回</li>
 * </ul>
 *
 * <p>内置组写入 {@link SAOConfig};附加组只调用自己的 save/reset。</p>
 */
public class SAOSettingsScreen extends Screen {

    private final Screen lastScreen;
    private final SettingsTimeline timeline = new SettingsTimeline();
    private final SettingsSkin skin = new SettingsSkin();
    private final SettingsNavigator navigator = new SettingsNavigator();
    private final Map<ResourceLocation, SaoTextField> nativeFields = new LinkedHashMap<>();
    private ResourceLocation nativeFieldsGroup;

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
        skin.bind(this.font, this.width, this.height, SettingsCatalog.groups(), navigator);
        remountNativeFields();
    }

    @Override
    public void tick() {
        super.tick();
        for (EditBox box : nativeFields.values()) {
            if (box.visible && box.active) {
                box.tick();
            }
        }
    }

    @Override
    public void removed() {
        flushMountedFields();
        releaseNativeFocus();
        super.removed();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        long now = Util.getMillis();
        float dt = timeline.tickDt(now);
        List<SettingsGroup> groups = SettingsCatalog.groups();
        skin.bind(this.font, this.width, this.height, groups, navigator);

        timeline.renderVideoBackground(g, this.width, this.height, now);
        skin.renderScrim(g);
        skin.renderDecorStripes(g, timeline, now);
        skin.updateHovers(timeline, dt, mouseX, mouseY);

        boolean inTransition = timeline.inTransition();
        long trT = timeline.transitionElapsed(now);
        boolean pageRoot = timeline.pageIsRoot(now);
        SettingsGroup pageGroup = groupAt(groups, timeline.pageGroupIndex(now));
        skin.renderPage(g, timeline, pageRoot, pageGroup, mouseX, mouseY, now, inTransition, trT);
        layoutNativeFields(pageRoot ? null : pageGroup);

        if (inTransition) {
            skin.renderWipe(g, timeline, trT);
            if (timeline.tick(now)) {
                playPanel();
                rebuildNativeFields();
            }
        }
        super.render(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (timeline.inTransition()) {
            return true;
        }
        List<SettingsGroup> groups = SettingsCatalog.groups();
        skin.bind(this.font, this.width, this.height, groups, navigator);
        int mx = (int) mouseX;
        int my = (int) mouseY;
        long now = Util.getMillis();

        if (skin.hitReset(mx, my)) {
            resetCurrent();
            playClick();
            return true;
        }
        if (skin.hitDone(mx, my)) {
            playClick();
            onClose();
            return true;
        }

        if (timeline.isRoot()) {
            int cat = skin.hitCategory(mx, my);
            if (cat >= 0) {
                navigator.enterGroup(cat);
                timeline.beginEnterCategory(cat, now);
                playClick();
                return true;
            }
        } else {
            if (skin.backHovered(mx, my)) {
                persistCurrent();
                navigator.returnToRoot();
                timeline.beginReturnToRoot(now);
                rebuildNativeFields();
                playClick();
                return true;
            }
            SettingsGroup page = navigator.currentGroup();
            if (skin.applyChoiceClick(page, mx, my)) {
                persistCurrent();
                playClick();
                return true;
            }
            if (page != null) {
                for (int i = 0; i < page.options().size(); i++) {
                    if (!skin.rowHovered(page, i, mx, my)) {
                        continue;
                    }
                    Setting spec = page.options().get(i);
                    navigator.focusOption(i);
                    if (spec instanceof NativeSetting nativeSetting) {
                        clickNativeField(nativeSetting, mouseX, mouseY, button);
                        return true;
                    }
                    if (spec instanceof SliderSetting) {
                        skin.beginSliderDrag(i);
                        skin.applySliderAt(page, i, mx);
                        persistCurrent();
                    } else if (skin.activateRow(page, i)) {
                        persistCurrent();
                    } else {
                        continue;
                    }
                    playClick();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && skin.dragRow() >= 0 && !timeline.inTransition()) {
            skin.bind(this.font, this.width, this.height, SettingsCatalog.groups(), navigator);
            skin.applySliderAt(navigator.currentGroup(), skin.dragRow(), (int) mouseX);
            persistCurrent();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            skin.endDrag();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta == 0) {
            return false;
        }
        if (timeline.inTransition()) {
            return true;
        }
        skin.bind(this.font, this.width, this.height, SettingsCatalog.groups(), navigator);
        int steps = delta > 0 ? -1 : 1;
        boolean moved = timeline.isRoot() ? navigator.scrollGroups(steps) : navigator.scrollOptions(steps);
        if (moved) {
            layoutNativeFields(navigator.currentGroup());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    /** 仅供预览自检:当前色相下会被高亮的预设 id(null = 色相被自定义,不指向任何预设)。 */
    public String debugHighlightedPreset() {
        try {
            int hue = Math.round(SAOConfig.accentHue());
            for (ThemeDefinition theme : SaoUi.themes()) {
                if (Math.round(theme.defaultHue()) == hue) {
                    return SettingsSkin.debugId(theme.id());
                }
            }
        } catch (RuntimeException ignored) {
            return null;
        }
        return null;
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
        int index = SettingsCatalog.debugIndex(name);
        if (index == Integer.MIN_VALUE) {
            return;
        }
        long now = Util.getMillis();
        if (index < 0) {
            navigator.returnToRoot();
            timeline.debugShowPage(true, -1, now);
        } else {
            navigator.enterGroup(index);
            timeline.debugShowPage(false, index, now);
        }
        rebuildNativeFields();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_O && !(getFocused() instanceof EditBox)) {
            onClose();
            return true;
        }
        if (getFocused() instanceof EditBox && keyCode != GLFW.GLFW_KEY_ESCAPE
                && keyCode != GLFW.GLFW_KEY_TAB) {
            boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
            if (handled || (keyCode != GLFW.GLFW_KEY_UP && keyCode != GLFW.GLFW_KEY_DOWN)) {
                return handled;
            }
        }
        if (timeline.inTransition()) {
            return true;
        }
        skin.bind(this.font, this.width, this.height, SettingsCatalog.groups(), navigator);
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!timeline.isRoot()) {
                persistCurrent();
                navigator.returnToRoot();
                timeline.beginReturnToRoot(Util.getMillis());
                rebuildNativeFields();
                playClick();
                return true;
            }
        }
        if (timeline.isRoot()) {
            if (keyCode == GLFW.GLFW_KEY_UP && navigator.moveGroup(-1)) {
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN && navigator.moveGroup(1)) {
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_SPACE
                    || keyCode == GLFW.GLFW_KEY_RIGHT) {
                if (navigator.groupIndex() >= 0) {
                    int cat = navigator.groupIndex();
                    navigator.enterGroup(cat);
                    timeline.beginEnterCategory(cat, Util.getMillis());
                    playClick();
                    return true;
                }
            }
        } else {
            if (keyCode == GLFW.GLFW_KEY_UP && navigator.moveOption(-1)) {
                layoutNativeFields(navigator.currentGroup());
                focusNativeIfNeeded();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN && navigator.moveOption(1)) {
                layoutNativeFields(navigator.currentGroup());
                focusNativeIfNeeded();
                return true;
            }
            Setting focused = navigator.currentSetting();
            if (focused instanceof SliderSetting slider) {
                if (keyCode == GLFW.GLFW_KEY_LEFT) {
                    slider.nudge(-1);
                    persistCurrent();
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                    slider.nudge(1);
                    persistCurrent();
                    return true;
                }
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
                SettingsGroup group = navigator.currentGroup();
                if (focused instanceof NativeSetting) {
                    focusNativeIfNeeded();
                    return true;
                }
                if (group != null && skin.activateRow(group, navigator.optionIndex())) {
                    persistCurrent();
                    playClick();
                    return true;
                }
            }
            if (keyCode == GLFW.GLFW_KEY_LEFT && focused instanceof com.sao.saomenu.api.settings.ChoiceSetting choice) {
                choice.cycle(-1);
                persistCurrent();
                playClick();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_RIGHT && focused instanceof com.sao.saomenu.api.settings.ChoiceSetting choice) {
                choice.cycle(1);
                persistCurrent();
                playClick();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        persistCurrent();
        SettingsCatalog.saveConfig();
        if (this.minecraft != null) {
            this.minecraft.setScreen(lastScreen);
        }
    }

    private void resetCurrent() {
        if (timeline.isRoot()) {
            SAOConfig.reset();
            SettingsCatalog.saveConfig();
            return;
        }
        SettingsGroup group = navigator.currentGroup();
        if (group != null) {
            group.reset();
            group.save();
            rebuildNativeFields();
        }
    }

    private void persistCurrent() {
        SettingsGroup group = navigator.currentGroup();
        if (group != null) {
            flushNativeFields(group);
            group.save();
        } else {
            SettingsCatalog.saveConfig();
        }
    }

    private void remountNativeFields() {
        SettingsGroup group = timeline.isRoot() ? null : navigator.currentGroup();
        if (group == null || this.font == null || !group.id().equals(nativeFieldsGroup)
                || nativeFields.isEmpty() || !fieldsMatch(group)) {
            rebuildNativeFields();
            return;
        }
        GuiEventListener focus = getFocused();
        for (EditBox box : nativeFields.values()) {
            addRenderableWidget(box);
        }
        layoutNativeFields(group);
        if (focus instanceof EditBox box && nativeFields.containsValue(box) && box.visible) {
            setFocused(box);
            box.setFocused(true);
        } else {
            releaseNativeFocus();
        }
    }

    private boolean fieldsMatch(SettingsGroup group) {
        int expected = 0;
        for (Setting setting : group.options()) {
            if (setting instanceof NativeSetting nativeSetting) {
                expected++;
                if (!nativeFields.containsKey(nativeSetting.id())) {
                    return false;
                }
            }
        }
        return expected == nativeFields.size();
    }

    private void rebuildNativeFields() {
        flushMountedFields();
        releaseNativeFocus();
        this.clearWidgets();
        nativeFields.clear();
        nativeFieldsGroup = null;
        SettingsGroup group = timeline.isRoot() ? null : navigator.currentGroup();
        if (group == null || this.font == null) {
            return;
        }
        nativeFieldsGroup = group.id();
        for (Setting setting : group.options()) {
            if (!(setting instanceof NativeSetting nativeSetting)) {
                continue;
            }
            SaoTextField box = new SaoTextField(this.font, 0, 0, 80, 12, nativeSetting.label());
            box.setMaxLength(nativeSetting.maxLength());
            box.setValue(nativeSetting.value());
            box.setResponder(nativeSetting::set);
            nativeFields.put(nativeSetting.id(), box);
            addRenderableWidget(box);
        }
        layoutNativeFields(group);
    }

    private void layoutNativeFields(SettingsGroup group) {
        for (EditBox box : nativeFields.values()) {
            box.visible = false;
            box.active = false;
        }
        if (group == null || timeline.inTransition() || timeline.isRoot()) {
            releaseNativeFocus();
            return;
        }
        EditBox focused = getFocused() instanceof EditBox box ? box : null;
        boolean focusedVisible = false;
        List<Setting> options = group.options();
        for (int i = 0; i < options.size(); i++) {
            if (!(options.get(i) instanceof NativeSetting nativeSetting) || !skin.rowVisible(group, i)) {
                continue;
            }
            SaoTextField box = nativeFields.get(nativeSetting.id());
            if (box == null) {
                continue;
            }
            int[] r = skin.nativeRect(group, i);
            int x = r[0] + 4;
            int y = r[1] + 1;
            int w = Math.max(16, r[2] - 8);
            int h = Math.max(8, r[3] - 2);
            box.setBounds(new UiRect(x, y, w, h));
            box.visible = true;
            box.active = true;
            if (box == focused) {
                focusedVisible = true;
            }
        }
        if (focused != null && nativeFields.containsValue(focused) && !focusedVisible) {
            focused.setFocused(false);
            setFocused(null);
        }
    }

    private void flushMountedFields() {
        SettingsGroup group = mountedGroup();
        if (group != null) {
            flushNativeFields(group);
        }
    }

    private SettingsGroup mountedGroup() {
        if (nativeFieldsGroup != null) {
            for (SettingsGroup group : SettingsCatalog.groups()) {
                if (nativeFieldsGroup.equals(group.id())) {
                    return group;
                }
            }
        }
        return navigator.currentGroup();
    }

    private void flushNativeFields(SettingsGroup group) {
        for (Setting setting : group.options()) {
            if (setting instanceof NativeSetting nativeSetting) {
                EditBox box = nativeFields.get(nativeSetting.id());
                if (box != null) {
                    nativeSetting.set(box.getValue());
                }
            }
        }
    }

    private void clickNativeField(NativeSetting setting, double mouseX, double mouseY, int button) {
        layoutNativeFields(navigator.currentGroup());
        EditBox box = nativeFields.get(setting.id());
        if (box == null || !box.visible) {
            releaseNativeFocus();
            return;
        }
        setFocused(box);
        box.setFocused(true);
        if (box.isMouseOver(mouseX, mouseY)) {
            box.mouseClicked(mouseX, mouseY, button);
        }
    }

    private void focusNativeIfNeeded() {
        Setting setting = navigator.currentSetting();
        if (setting instanceof NativeSetting nativeSetting) {
            EditBox box = nativeFields.get(nativeSetting.id());
            if (box != null && box.visible && box.active) {
                setFocused(box);
                box.setFocused(true);
                return;
            }
        }
        releaseNativeFocus();
    }

    private void releaseNativeFocus() {
        if (getFocused() instanceof EditBox box && nativeFields.containsValue(box)) {
            box.setFocused(false);
            setFocused(null);
        }
    }

    private static SettingsGroup groupAt(List<SettingsGroup> groups, int index) {
        if (index < 0 || index >= groups.size()) {
            return null;
        }
        return groups.get(index);
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
