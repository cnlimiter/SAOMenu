package com.sao.saomenu.client.menu;

import com.sao.saomenu.config.SAOConfig;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;

import static com.sao.saomenu.ui.animation.SaoMotion.BOB_PERIOD_MS;
import static com.sao.saomenu.ui.animation.SaoMotion.easeOutBack;
import static com.sao.saomenu.ui.animation.SaoMotion.easeOutCubic;

/**
 * 当前帧菜单组仿射变换:缩放、Z 旋转、错切、展开左移、鼠标漂移。
 *
 * <p>绘制、命中、剪裁共用同一组参数。正向与逆向必须互逆,且不每帧 new 矩阵/数组。</p>
 */
final class MenuTransform {

    private float scale = 1f;
    private int anchorX;
    private int anchorY;
    private float shiftXs;
    private float followXs;
    private float followYs;
    private float followPxX;
    private float followPxY;
    private float swayXs;
    private float swayYs;

    private float localX;
    private float localY;

    private final float[] ptOut = new float[2];
    private final Matrix4f shear = new Matrix4f();
    private final Quaternionf rotZ = new Quaternionf();

    float scale() {
        return scale;
    }

    int anchorX() {
        return anchorX;
    }

    int anchorY() {
        return anchorY;
    }

    float localX() {
        return localX;
    }

    float localY() {
        return localY;
    }

    /**
     * 按本帧动画与指针更新变换,必须在悬停命中之前调用。
     *
     * @param layoutAy 活动按钮(未选则首按钮)的布局圆心 Y,尚未加浮动
     * @param childShift 二级列可见时的左移目标(列宽差),否则 0
     */
    void update(int width, int height, int baseAnchorX, int layoutAy, float childShift,
                int mouseX, int mouseY, long now, boolean closing, float openP, float closeP) {
        float bobY = Mth.sin(now / (float) BOB_PERIOD_MS * Mth.TWO_PI) * height * 0.006f * SAOConfig.bobAmp();
        scale = closing
                ? 1f - 0.15f * easeOutCubic(closeP)
                : (0.55f + 0.45f * easeOutBack(openP)) * SAOConfig.menuScale();
        anchorX = baseAnchorX;
        anchorY = layoutAy + Math.round(bobY);

        shiftXs += (childShift - shiftXs) * 0.18f;

        float fx = Mth.clamp((mouseX - width / 2f) / (float) width, -0.5f, 0.5f);
        float fy = Mth.clamp((mouseY - height / 2f) / (float) height, -0.5f, 0.5f);
        followXs += (fx - followXs) * 0.08f;
        followYs += (fy - followYs) * 0.08f;
        followPxX = followXs * width * 0.035f;
        followPxY = followYs * height * 0.035f;

        float tx = Mth.clamp((mouseX - anchorX) / (float) Math.max(1, width), -0.6f, 0.6f);
        float ty = Mth.clamp((mouseY - anchorY) / (float) Math.max(1, height), -0.6f, 0.6f);
        swayXs += (tx - swayXs) * 0.14f;
        swayYs += (ty - swayYs) * 0.14f;
    }

    /** 把当前变换乘到 pose 上(调用方负责 push/pop)。 */
    void apply(PoseStack pose) {
        pose.translate(anchorX, anchorY, 0);
        rotZ.rotationZ(swayXs * 0.03f);
        pose.mulPose(rotZ);
        shear.identity();
        shear.m10(swayXs * 0.06f);
        shear.m01(swayYs * 0.05f);
        pose.last().pose().mul(shear);
        pose.scale(scale, scale, 1f);
        pose.translate(-anchorX, -anchorY, 0);
        pose.translate(-shiftXs, 0.0F, 0.0F);
        pose.translate(followPxX, followPxY, 0.0F);
    }

    /**
     * 屏幕坐标 → 菜单本地,写入 {@link #localX}/{@link #localY}。
     *
     * <p>逆序:锚点 → 漂移 → 左移 → 缩放 → 错切 → Z 旋转 的逆。</p>
     */
    void toLocal(double mx, double my) {
        float dx = (float) mx - anchorX;
        float dy = (float) my - anchorY;
        float cos = Mth.cos(swayXs * 0.03f);
        float sin = Mth.sin(swayXs * 0.03f);
        float rx = cos * dx + sin * dy;
        float ry = -sin * dx + cos * dy;
        float a = swayXs * 0.06f;
        float b = swayYs * 0.05f;
        float det = Math.abs(1f - a * b) < 1.0e-4f ? 1.0e-4f : 1f - a * b;
        float ux = (rx - a * ry) / det;
        float uy = (ry - b * rx) / det;
        float s = Math.abs(scale) < 1.0e-4f ? 1.0e-4f : scale;
        localX = ux / s + anchorX + shiftXs - followPxX;
        localY = uy / s + anchorY - followPxY;
    }

    int localXi() {
        return Math.round(localX);
    }

    int localYi() {
        return Math.round(localY);
    }

    /**
     * 菜单本地 → 屏幕。与 {@link #toLocal} 互逆:
     * 漂移 → 左移 → 缩放 → 错切 → Z 旋转 → 锚点。
     */
    void toScreen(float lx, float ly, float[] out) {
        float x = (lx + followPxX - shiftXs - anchorX) * scale;
        float y = (ly + followPxY - anchorY) * scale;
        float a = swayXs * 0.06f;
        float b = swayYs * 0.05f;
        float sx = x + a * y;
        float sy = y + b * x;
        float cos = Mth.cos(swayXs * 0.03f);
        float sin = Mth.sin(swayXs * 0.03f);
        out[0] = cos * sx - sin * sy + anchorX;
        out[1] = sin * sx + cos * sy + anchorY;
    }

    MenuLayout.Rect boxToScreen(int lx, int ly, int w, int h) {
        toScreen(lx, ly, ptOut);
        float minX = ptOut[0];
        float maxX = ptOut[0];
        float minY = ptOut[1];
        float maxY = ptOut[1];
        toScreen(lx + w, ly, ptOut);
        minX = Math.min(minX, ptOut[0]);
        maxX = Math.max(maxX, ptOut[0]);
        minY = Math.min(minY, ptOut[1]);
        maxY = Math.max(maxY, ptOut[1]);
        toScreen(lx, ly + h, ptOut);
        minX = Math.min(minX, ptOut[0]);
        maxX = Math.max(maxX, ptOut[0]);
        minY = Math.min(minY, ptOut[1]);
        maxY = Math.max(maxY, ptOut[1]);
        toScreen(lx + w, ly + h, ptOut);
        minX = Math.min(minX, ptOut[0]);
        maxX = Math.max(maxX, ptOut[0]);
        minY = Math.min(minY, ptOut[1]);
        maxY = Math.max(maxY, ptOut[1]);
        int x0 = Math.round(minX);
        int y0 = Math.round(minY);
        return new MenuLayout.Rect(x0, y0, Math.max(1, Math.round(maxX) - x0), Math.max(1, Math.round(maxY) - y0));
    }
}
