package com.sao.saomenu.client.render.target;

import com.sao.saomenu.ui.render.SaoDraw;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD 名称/血量文字:世界阶段投影,HUD 阶段绘制。
 *
 * <p>菱形公告板负 Y 缩放后局部 y=0 是顶部;文字从菱形顶往上排,不按底锚挪位置。</p>
 */
final class TargetLabels {

    /** 本帧需要在 HUD 层补画名称/血量文字的目标。 */
    private static final List<Label> LABELS = new ArrayList<>();
    private static final Matrix4f VIEW = new Matrix4f();
    private static final Matrix4f MVP = new Matrix4f();
    private static final Vector4f CLIP = new Vector4f();
    private static final float[] PROJ = new float[2];
    private static final float[] PROJ_TOP = new float[2];

    private TargetLabels() {
    }

    /** HUD 文字的一次性投影结果(世界渲染阶段算好,HUD 阶段消费)。 */
    private record Label(int entityId, float x, float y, float frac, float alpha, float kiteH) {
    }

    static void beginFrame() {
        LABELS.clear();
    }

    static void reset() {
        LABELS.clear();
    }

    static int count() {
        return LABELS.size();
    }

    /**
     * 把头顶菱形的屏幕坐标算出来存进本帧标签表。
     *
     * <p>{@code head} 是菱形顶部(公告板 y=0)。投影高度用头顶上方 0.52 格
     * 估菱形像素高,供字号微调;文字仍从 {@code y - kite} 往上排。</p>
     */
    static void capture(Minecraft mc, int entityId, Vec3 head, float frac, float alpha) {
        if (!project(mc, head, PROJ)) {
            return;
        }
        float kiteH = 12f;
        if (project(mc, head.add(0, 0.52, 0), PROJ_TOP)) {
            kiteH = Math.max(6f, Math.abs(PROJ_TOP[1] - PROJ[1]));
        }
        LABELS.add(new Label(entityId, PROJ[0], PROJ[1], frac, alpha, kiteH));
    }

    /**
     * HUD 阶段调用:把本帧收集到的名称/血量文字画在屏幕上。
     *
     * <p>文字刻意不留在世界空间。世界里的 {@code font.drawInBatch} 依赖
     * 半透明字形与自身的 RenderType 排序,光影客户端上会被 gbuffer pass
     * 洗成纯色块或整体消失(用户实拍中名称与血量数字都不见了)。
     * 走 HUD 层则完全绕开光影管线,任何画质设置下都稳定可读。</p>
     */
    static void render(GuiGraphics g, Minecraft mc) {
        if (LABELS.isEmpty() || mc.level == null) {
            return;
        }
        var font = mc.font;
        for (Label lb : LABELS) {
            Entity e = mc.level.getEntity(lb.entityId());
            if (!(e instanceof LivingEntity le)) {
                continue;
            }
            String name = le.getDisplayName().getString();
            String hp = trim(le.getHealth()) + " / " + trim(le.getMaxHealth());
            int a = Math.round(255 * Mth.clamp(lb.alpha(), 0f, 1f)) << 24;
            if (a == 0) {
                continue;
            }
            // 字号以屏幕 9px 为底,距离只做 ±25% 微调。菱形投影(约 11px)不能当行高,
            // 否则 10 格外会缩到 ~3.7px。文字从菱形顶往上排,不压在图形上。
            float kite = Math.max(8f, lb.kiteH());
            float s = Mth.clamp(kite / 18f, 0.75f, 1.25f);
            float textH = font.lineHeight * s;
            int colorName = 0xF2F5F8 | a;
            int colorHp = (SAOTargetBar.hpColor(lb.frac()) & 0xFFFFFF) | a;
            SaoDraw.drawCentered(g, font, SaoDraw.clipTo(font, name, 120),
                    lb.x(), lb.y() - kite - textH * 1.5f, s, colorName, true);
            SaoDraw.drawCentered(g, font, hp,
                    lb.x(), lb.y() - kite - textH * 0.5f, s, colorHp, true);
        }
    }

    /**
     * 世界坐标 → GUI 屏幕坐标;身后/屏幕外返回 false。
     *
     * <p>视图旋转必须与 MC 世界渲染一致:GameRenderer 用
     * {@code XP.rotationDegrees(camera.getXRot())} 再
     * {@code YP.rotationDegrees(camera.getYRot() + 180f)} 级联构建。
     * {@code Axis.rotation} 收的是弧度,必须显式换算,否则会放大 57 倍。</p>
     */
    private static boolean project(Minecraft mc, Vec3 pos, float[] out) {
        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 rel = pos.subtract(cam.getPosition());
        float deg2rad = (float) (Math.PI / 180.0);
        VIEW.identity()
                .rotate(com.mojang.math.Axis.XP.rotation(cam.getXRot() * deg2rad))
                .rotate(com.mojang.math.Axis.YP.rotation((cam.getYRot() + 180f) * deg2rad));
        MVP.set(mc.gameRenderer.getProjectionMatrix(mc.options.fov().get())).mul(VIEW);
        CLIP.set((float) rel.x, (float) rel.y, (float) rel.z, 1f).mul(MVP);
        if (CLIP.w <= 0.001f) {
            return false;
        }
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        out[0] = (CLIP.x / CLIP.w * 0.5f + 0.5f) * w;
        out[1] = (0.5f - CLIP.y / CLIP.w * 0.5f) * h;
        return true;
    }

    private static String trim(float v) {
        float r = Math.round(v * 10f) / 10f;
        return (r == Math.rint(r)) ? String.valueOf((int) r) : String.valueOf(r);
    }
}
