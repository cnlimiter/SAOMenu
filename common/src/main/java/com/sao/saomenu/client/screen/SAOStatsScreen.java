package com.sao.saomenu.client.screen;

import com.sao.saomenu.ui.theme.SaoTheme;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * SAO 风格属性面板:菜单"技能"项打开,完整展示玩家全部核心属性。
 * 属性名走原版翻译(attribute.name.*),数值实时读取。
 */
public class SAOStatsScreen extends Screen {

    private static final int PANEL_BG = 0xE6232729;
    private static final int SHADOW = 0x3A000000;
    private static final int ROW_HOVER = 0x1AF9F9F9;
    private static final int TEXT_WHITE = 0xFFF9F9F9;
    private static final int TEXT_GRAY = 0xFF9A9B9D;

    private static final int ROW_H = 14;

    /**
     * 已知属性的展示顺序。
     *
     * <p>属性表<b>不再写死</b>:遍历玩家身上真实存在的属性,模组新增的属性因此自动出现。
     * 但这个列表里的属性优先按原顺序排,其余按注册名追加在后面 —— 既保证同一玩家每次
     * 打开的顺序稳定,又不改变原版属性的既有排列。</p>
     */
    private static final List<Attribute> PREFERRED = List.of(
            Attributes.MAX_HEALTH,
            Attributes.ARMOR,
            Attributes.ARMOR_TOUGHNESS,
            Attributes.ATTACK_DAMAGE,
            Attributes.ATTACK_SPEED,
            Attributes.ATTACK_KNOCKBACK,
            Attributes.KNOCKBACK_RESISTANCE,
            Attributes.MOVEMENT_SPEED,
            Attributes.LUCK
    );

    private final Screen lastScreen;
    private final Player player;
    private final List<String[]> rows = new ArrayList<>();

    public SAOStatsScreen(Screen lastScreen, Player player) {
        super(Component.translatable("saomenu.menu.skill"));
        this.lastScreen = lastScreen;
        this.player = player;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        rows.clear();
        // 玩家实际拥有哪些属性由同步过来的属性表决定:平台或模组没注册的自然不在其中
        // (Fabric 1.20.1 没有 attack_knockback,它会自动缺席,不需要逐个判空)
        List<Attribute> present = new ArrayList<>();
        for (Attribute a : net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE) {
            if (player.getAttribute(a) != null) {
                present.add(a);
            }
        }
        present.sort(java.util.Comparator
                .comparingInt((Attribute a) -> {
                    int i = PREFERRED.indexOf(a);
                    return i < 0 ? Integer.MAX_VALUE : i;
                })
                .thenComparing(Attribute::getDescriptionId));
        for (Attribute a : present) {
            String name = Component.translatable(a.getDescriptionId()).getString();
            String value = trim(player.getAttributeValue(a));
            rows.add(new String[]{name, value});
        }
    }

    // ------------------------------------------------------------ 布局

    /** 仅供预览自检:当前属性行"名=值",按渲染顺序。 */
    public List<String> debugRowLabels() {
        List<String> out = new ArrayList<>();
        for (String[] r : rows) {
            out.add(r[0] + "=" + r[1]);
        }
        return out;
    }

    private int panelW() {
        return Math.min(320, this.width - 20);
    }

    private int panelH() {
        return 30 + rows.size() * ROW_H + 30;
    }

    private int panelX() {
        return (this.width - panelW()) / 2;
    }

    private int panelY() {
        return Math.max(10, (this.height - panelH()) / 2);
    }

    private boolean inDone(int mx, int my) {
        int y = panelY() + panelH() - 24;
        return mx >= panelX() + panelW() - 72 && mx < panelX() + panelW() - 12
                && my >= y && my < y + 18;
    }

    // ------------------------------------------------------------ 渲染

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackground(g);
        int px = panelX();
        int py = panelY();
        int pw = panelW();
        int ph = panelH();
        g.fill(px + 3, py + 3, px + pw + 3, py + ph + 3, SHADOW);
        g.fill(px, py, px + pw, py + ph, PANEL_BG);

        String title = Component.translatable("saomenu.menu.skill").getString();
        g.drawString(this.font, title, px + pw / 2 - this.font.width(title) / 2,
                py + 10, TEXT_WHITE, false);

        for (int i = 0; i < rows.size(); i++) {
            int y = py + 28 + i * ROW_H;
            boolean hover = mouseX >= px + 10 && mouseX <= px + pw - 10 && mouseY >= y && mouseY < y + ROW_H;
            if (hover) {
                g.fill(px + 10, y, px + pw - 10, y + ROW_H, ROW_HOVER);
            }
            String[] row = rows.get(i);
            g.drawString(this.font, row[0], px + 14, y + 3, TEXT_WHITE, false);
            g.drawString(this.font, row[1], px + pw - 14 - this.font.width(row[1]), y + 3,
                    SaoTheme.accent(), false);
        }

        // 完成按钮
        int by = py + ph - 24;
        boolean hoverDone = inDone(mouseX, mouseY);
        g.fill(px + pw - 72, by, px + pw - 12, by + 18,
                hoverDone ? lighten(SaoTheme.accent()) : SaoTheme.accent());
        String done = Component.translatable("saomenu.inventory.done").getString();
        g.drawString(this.font, done, px + pw - 72 + (60 - this.font.width(done)) / 2,
                by + 5, 0xFF232323, false);
    }

    private static int lighten(int argb) {
        int r = Math.min(255, ((argb >> 16) & 0xFF) + 30);
        int g = Math.min(255, ((argb >> 8) & 0xFF) + 30);
        int b = Math.min(255, (argb & 0xFF) + 30);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static String trim(double v) {
        float f = (float) v;
        float r = Math.round(f * 100f) / 100f;
        return (r == Math.rint(r)) ? String.valueOf((int) r) : String.valueOf(r);
    }

    // ------------------------------------------------------------ 交互

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && inDone((int) mouseX, (int) mouseY)) {
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
        if (this.minecraft != null) {
            this.minecraft.setScreen(lastScreen);
        }
    }
}
