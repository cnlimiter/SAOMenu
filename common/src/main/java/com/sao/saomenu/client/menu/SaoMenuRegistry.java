package com.sao.saomenu.client.menu;

import com.sao.saomenu.SAOMenu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 面板注册表:菜单有哪些页,由注册顺序决定。
 *
 * <p>注册顺序 = 主按钮列从上到下的顺序,所以内置面板必须按
 * 个人 → 队伍 → 好友 → 设置 注册(预览自检按主按钮下标点击,顺序变了会点错)。</p>
 *
 * <p>重复 id 会被拒绝并记日志而不是静默覆盖:菜单是每帧遍历注册表的,
 * 一个重复项会让同一页出现两次且难以定位。</p>
 */
public final class SaoMenuRegistry {

    private static final List<SaoPanel> PANELS = new ArrayList<>();
    private static boolean builtinsRegistered;

    private SaoMenuRegistry() {
    }

    /** 注册一个面板;id 重复时忽略并警告。 */
    public static void register(SaoPanel panel) {
        if (panel == null || panel.id() == null || panel.id().isEmpty()) {
            SAOMenu.LOGGER.warn("[SAOMenu] 拒绝注册无 id 的面板");
            return;
        }
        for (SaoPanel p : PANELS) {
            if (p.id().equals(panel.id())) {
                SAOMenu.LOGGER.warn("[SAOMenu] 面板 id 重复,已忽略: {}", panel.id());
                return;
            }
        }
        PANELS.add(panel);
    }

    /** 已注册面板(只读)。 */
    public static List<SaoPanel> panels() {
        return Collections.unmodifiableList(PANELS);
    }

    public static int size() {
        return PANELS.size();
    }

    /** 按 id 取面板;不存在返回 {@code null}。 */
    public static SaoPanel byId(String id) {
        for (SaoPanel p : PANELS) {
            if (p.id().equals(id)) {
                return p;
            }
        }
        return null;
    }

    /** 按 id 取下标;不存在返回 -1。 */
    public static int indexOf(String id) {
        for (int i = 0; i < PANELS.size(); i++) {
            if (PANELS.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    /** 注册内置面板(幂等)。客户端初始化时调用一次。 */
    public static void registerBuiltins() {
        if (builtinsRegistered) {
            return;
        }
        builtinsRegistered = true;
        for (SaoPanel p : SaoPanels.all()) {
            register(p);
        }
    }

    /** 清空(仅测试用)。 */
    static void clearForTest() {
        PANELS.clear();
        builtinsRegistered = false;
    }
}
