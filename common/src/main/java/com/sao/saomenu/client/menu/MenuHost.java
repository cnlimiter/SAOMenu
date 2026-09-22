package com.sao.saomenu.client.menu;

import com.sao.saomenu.client.menu.MenuLayout;
import net.minecraft.client.gui.screens.Screen;

/**
 * 菜单宿主(由菜单屏实现)提供给面板的能力。
 *
 * <p>把"菜单自身状态"和"屏幕变换"收敛成一个接口,面板代码因此不必 import
 * 菜单屏这一具体类;以后要换一个宿主(例如世界空间菜单板)也只需再实现一次。</p>
 */
public interface MenuHost {

    /** 宿主界面本身,用作子界面的返回目标。 */
    Screen screen();

    // ------------------------------------------------------------ 菜单自身状态

    /** 选中第 index 个主按钮;传 -1 收起面板。 */
    void selectMain(int index);

    /** 弹出"确认关闭菜单"对话框。 */
    void openCloseConfirm();

    /** 开/关地图面板。 */
    void toggleMap();

    // ------------------------------------------------------------ 音效

    /** 菜单打开 / 一级项点击。 */
    void playClick();

    /** 面板展开 / 子列切换。 */
    void playPanel();

    /** 关闭 / 弹窗。 */
    void playAlert();

    // ------------------------------------------------------------ 屏幕变换

    /**
     * 菜单本地坐标 → 屏幕坐标。
     *
     * <p>菜单整组带缩放、错切、左移与漂移;面板若要自己画屏幕空间图元
     * (剪裁框、跟随光标的东西),必须过这一层,不能拿本地坐标当屏幕坐标用。</p>
     */
    float[] screenPointOf(float localX, float localY);

    /** 菜单本地矩形 → 屏幕轴对齐包围盒(剪裁框用)。 */
    MenuLayout.Rect localBoxToScreen(int localX, int localY, int w, int h);
}
