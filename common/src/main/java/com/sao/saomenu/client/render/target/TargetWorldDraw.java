package com.sao.saomenu.client.render.target;

import com.sao.saomenu.client.hud.SAOCombatHud;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 世界空间图元:单薄壳体弧带、头顶红菱形、队友绿三角。
 *
 * <p>壳体任意两个面在径向、纵向、角度三个维度上至少有一个维度完全不相交,
 * 颜色一律不透明。读写深度,由几何位置定内外面胜负。</p>
 */
final class TargetWorldDraw {

    // 配色:全部不透明。alpha 只在淡入淡出期间参与,且淡入完成后恒为 FF,
    // 这样即使光影丢弃顶点 alpha,稳定态的观感也与原设计一致。
    private static final int EDGE_LIGHT = 0xFFF4FBFF;
    private static final int TRACK_DARK = 0xFF141A1E;

    private static final int KITE_FILL = 0xFFD81E3C;
    private static final int KITE_EDGE = 0xFFFF6A82;

    // 队友绿三角(参照 SAO 队友头顶标识)
    private static final int PARTY_FILL = 0xFF3ED44F;
    private static final int PARTY_EDGE = 0xFFB9FFC2;

    /**
     * 世界空间 UI 图元:纯色 + 双面 + 读写深度。
     *
     * <p>必须<strong>写</strong>深度。壳体的内衬面与外读数面角度区间相同、只差半径,
     * 不写深度时两者谁盖谁完全取决于提交顺序,而提交顺序在光影客户端上不受控——
     * 表现就是内衬的暗色整片盖住血量读数。写深度后由几何位置定胜负:
     * 离相机更近的外读数面永远在前,任何渲染器上都一致。</p>
     *
     * <p>环带、尾块、头顶菱形共用这一个类型:BufferSource 对自建类型
     * 按哈希序出批,拆成多个类型后与其他缓冲交错会把标识冲掉。</p>
     *
     * <p>延迟创建:{@code RenderType.create} 会触碰 RenderStateShard 的静态初始化,
     * 在无 GL 的单元测试环境里会抛 ExceptionInInitializerError,
     * 所以不能作为类的静态字段直接初始化。</p>
     */
    private static RenderType uiQuads;

    private TargetWorldDraw() {
    }

    private static RenderType uiQuads() {
        if (uiQuads == null) {
            uiQuads = RenderType.create(
                    "saomenu_target_ring",
                    DefaultVertexFormat.POSITION_COLOR,
                    VertexFormat.Mode.QUADS,
                    1536,
                    false,
                    false,
                    RenderType.CompositeState.builder()
                            .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                            .setCullState(RenderStateShard.NO_CULL)
                            .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                            .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                            .createCompositeState(false));
        }
        return uiQuads;
    }

    /**
     * 队友头顶绿色倒三角(上边宽、下端收尖,与敌方红菱形区分),随距离轻微缩放,
     * 无视线门控——队友标识 SAO 原作中常显,越远越小即可。
     */
    static void drawPartyMarker(Minecraft mc, PoseStack pose, MultiBufferSource src,
                                LivingEntity mate, Vec3 camPos, float partialTick, long now) {
        Vec3 head = mate.getPosition(partialTick).add(0, mate.getBbHeight() + 0.55, 0);
        double dist = Math.max(2.0, head.distanceTo(camPos));
        // 距离缩放:近大远小,钳制在 0.5x..1.6x(基准按 10 格观感)
        float size = Mth.clamp((float) (10.0 / dist), 0.5f, 1.6f);
        float bob = SAOTargetBar.kiteBob(now) * 0.6f;

        pose.pushPose();
        pose.translate(head.x - camPos.x, head.y - camPos.y, head.z - camPos.z);
        pose.mulPose(mc.gameRenderer.getMainCamera().rotation());
        pose.scale(0.04f, -0.04f, 0.04f);
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = src.getBuffer(uiQuads());

        // 倒三角:13 行,顶行最宽(halfW),向下线性收到 1px 尖端
        int kh = 13;
        int maxHalf = Math.max(3, Math.round(5 * size));
        for (int i = 0; i < kh; i++) {
            float t = i / (float) (kh - 1);
            int half = Math.max(1, Math.round(maxHalf * (1f - t)));
            float yy = i + bob;
            if (half > 1) {
                quad(vc, m, -half + 1, yy, half - 1, yy + 1, PARTY_FILL, 1f);
            }
            quad(vc, m, -half, yy, -half + 1, yy + 1, PARTY_EDGE, 1f);
            quad(vc, m, half - 1, yy, half, yy + 1, PARTY_EDGE, 1f);
        }
        pose.popPose();
    }

    static void drawRing(Minecraft mc, PoseStack pose, MultiBufferSource.BufferSource src,
                         LivingEntity le, Vec3 center, Vec3 camPos,
                         float alpha, long now, float partialTick) {
        float frac = le.getMaxHealth() <= 0f ? 0f
                : Mth.clamp(le.getHealth() / le.getMaxHealth(), 0f, 1f);
        // 菱形与文字不受贴脸淡出约束,近距离仍要能读表
        float markerAlpha = alpha;
        float ringAlpha = Math.min(alpha, TargetGeometry.proximityFade(
                Math.hypot(center.x - camPos.x, center.z - camPos.z)));
        if (ringAlpha > 0.01f) {
            drawBand(pose, src, le, center, camPos, ringAlpha, frac, now, partialTick);
        }
        drawMarker(mc, pose, src, le, center, camPos, frac, markerAlpha, now, partialTick);
    }

    /**
     * 环绕弧带:单薄壳体,层与层在径向/纵向/角度上互不重叠。
     *
     * <p>纵向自上而下是「亮边条 / 血量读数区 / 亮边条」三段,加上顶底两圈
     * 表现厚度的环面与一层内衬。读数区内,血量段与空槽段占互不相交的角度区间。</p>
     */
    private static void drawBand(PoseStack pose, MultiBufferSource.BufferSource src,
                                 LivingEntity le, Vec3 center, Vec3 camPos,
                                 float alpha, float frac, long now, float partialTick) {
        double rIn = TargetGeometry.ringRadius(le.getBbWidth());
        double rOut = rIn + TargetGeometry.BAND_THICK;
        float bandH = TargetGeometry.bandHeight(le.getBbWidth());
        float flash = SAOTargetBar.flashStrength(now, SAOCombatHud.hurtAt(le.getId()));

        float span = TargetGeometry.arcSpan();
        // 弧带锚定生物身体朝向,但转速被限速阻尼:怪物 AI 每 tick 调整朝向,
        // 直接跟随会疯转;限速后环带只缓慢转动,小幅抖动被滤平。
        // 中点角 = 90° − bodyRot(MC 朝向向量 (sin yRot, cos yRot) 与弧角方向重合)
        float targetRot = 90f - Mth.rotLerp(partialTick, le.yBodyRotO, le.yBodyRot);
        float bandRot = TargetTracker.advanceBandRot(le.getId(), targetRot);
        float start = TargetGeometry.arcStart((float) Math.toRadians(bandRot));
        float tailStart = start + span + TargetGeometry.TAIL_GAP_DEGREES * Mth.DEG_TO_RAD;
        float tailSpan = TargetGeometry.TAIL_DEGREES * Mth.DEG_TO_RAD;

        float y1 = bandH / 2f;
        float y0 = -y1;
        float edge = TargetGeometry.edgeHeight(bandH);
        float yr1 = y1 - edge;
        float yr0 = y0 + edge;

        int hp = SAOTargetBar.hpColor(frac) | 0xFF000000;
        if (flash > 0f) {
            hp = SAOTargetBar.lerpColor(hp, 0xFFFFFFFF, flash * 0.42f);
        }
        float fillSpan = TargetGeometry.fillAngle(frac);
        float fillFrom = TargetGeometry.fillStart(start, frac);

        pose.pushPose();
        pose.translate(center.x - camPos.x, center.y - camPos.y, center.z - camPos.z);
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = src.getBuffer(uiQuads());

        for (int pass = 0; pass < 2; pass++) {
            boolean tail = pass == 1;
            float from = tail ? tailStart : start;
            float len = tail ? tailSpan : span;
            // 顺序也是一道保险:内衬与顶底环面先画,读数面最后画。
            // 内衬(rIn)与读数面(rOut)只差半径,深度写入负责让更近的面胜出;
            // 万一渲染器忽略深度,后画的读数面同样在上。
            // 内衬面双面读数:与外表面完全相同的角度布局——环带是一个整体,
            // 两个面花纹一致,斜着同时看到两个面时色带才衔接得上;
            // 若按视角镜像(内外锚点对调),三视角下色带会看起来左右错位。
            arcBand(vc, m, rIn, rIn, y0, yr0, from, len, EDGE_LIGHT, alpha);
            arcBand(vc, m, rIn, rIn, yr1, y1, from, len, EDGE_LIGHT, alpha);
            arcBand(vc, m, rIn, rOut, y1, y1, from, len, EDGE_LIGHT, alpha);
            arcBand(vc, m, rIn, rOut, y0, y0, from, len, EDGE_LIGHT, alpha);
            // 内衬读数区:与外表面同角度区间
            if (tail) {
                arcBand(vc, m, rIn, rIn, yr0, yr1, from, len,
                        frac > 0.98f ? hp : TRACK_DARK, alpha);
            } else {
                if (fillSpan < len) {
                    arcBand(vc, m, rIn, rIn, yr0, yr1, from, len - fillSpan,
                            TRACK_DARK, alpha);
                }
                if (fillSpan > 0f) {
                    arcBand(vc, m, rIn, rIn, yr0, yr1, fillFrom, fillSpan, hp, alpha);
                }
            }
            // 外表面上下亮边条:与读数区纵向不重叠
            arcBand(vc, m, rOut, rOut, yr1, y1, from, len, EDGE_LIGHT, alpha);
            arcBand(vc, m, rOut, rOut, y0, yr0, from, len, EDGE_LIGHT, alpha);
            // 读数区:空槽段与血量段角度互斥,谁都盖不住谁
            if (tail) {
                arcBand(vc, m, rOut, rOut, yr0, yr1, from, len,
                        frac > 0.98f ? hp : TRACK_DARK, alpha);
            } else {
                if (fillSpan < len) {
                    arcBand(vc, m, rOut, rOut, yr0, yr1, from, len - fillSpan,
                            TRACK_DARK, alpha);
                }
                if (fillSpan > 0f) {
                    arcBand(vc, m, rOut, rOut, yr0, yr1, fillFrom, fillSpan, hp, alpha);
                }
            }
        }
        pose.popPose();
    }

    /**
     * 头顶红色菱形 + 记录文字投影位置。
     *
     * <p>菱形是纯色块,走世界空间没问题;文字改由 {@link TargetLabels} 在 HUD 层绘制,
     * 这里只把头顶上方的屏幕坐标算出来存进标签表。</p>
     *
     * <p>公告板 {@code scale(0.04, -0.04, 0.04)}:局部 y=0 是菱形顶部。
     * 不要把标签当底锚去挪位置。</p>
     */
    private static void drawMarker(Minecraft mc, PoseStack pose, MultiBufferSource src,
                                   LivingEntity le, Vec3 center, Vec3 camPos,
                                   float frac, float alpha, long now, float partialTick) {
        if (alpha <= 0.01f) {
            return;
        }
        Vec3 head = le.getPosition(partialTick).add(0, le.getBbHeight() + 0.62, 0);
        float bob = SAOTargetBar.kiteBob(now) * 0.8f;

        pose.pushPose();
        pose.translate(head.x - camPos.x, head.y - camPos.y, head.z - camPos.z);
        pose.mulPose(mc.gameRenderer.getMainCamera().rotation());
        // 世界单位 → 像素单位:公告板内部按 1/25 格每像素作图。
        // Y 取负:像素坐标系 +y 向下,与世界 +y 相反。y=0 是顶部。
        pose.scale(0.04f, -0.04f, 0.04f);
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = src.getBuffer(uiQuads());

        // 菱形:上段迅速张开、下段收成长尖(与 2D 版同一轮廓函数)
        int kh = 13;
        int maxHalf = 4;
        for (int i = 0; i < kh; i++) {
            float t = i / (float) (kh - 1);
            int half = Math.max(1, Math.round(maxHalf * SAOTargetBar.kiteHalfWidth(t)));
            float yy = i + bob;
            // 中段填充与两侧亮边横向不重叠
            if (half > 1) {
                quad(vc, m, -half + 1, yy, half - 1, yy + 1, KITE_FILL, alpha);
            }
            quad(vc, m, -half, yy, -half + 1, yy + 1, KITE_EDGE, alpha);
            quad(vc, m, half - 1, yy, half, yy + 1, KITE_EDGE, alpha);
        }
        pose.popPose();

        TargetLabels.capture(mc, le.getId(), head, frac, alpha);
    }

    /**
     * 沿圆周铺一段带子:每段一个四边形。
     *
     * <p>{@code r0 == r1} 得到竖直面,{@code yLo == yHi} 得到水平环面,
     * 两者都用得到,所以半径与高度都各留两个参数。</p>
     *
     * @param r0   底边(yLo 侧)半径
     * @param r1   顶边(yHi 侧)半径
     * @param from 起始角(弧度)
     * @param span 跨越角(弧度)
     */
    private static void arcBand(VertexConsumer vc, Matrix4f m,
                                double r0, double r1, float yLo, float yHi,
                                float from, float span, int argb, float alpha) {
        if (span <= 0f) {
            return;
        }
        int segs = Math.max(1, Math.round(TargetGeometry.SEGMENTS * span
                / (TargetGeometry.ARC_DEGREES * Mth.DEG_TO_RAD)));
        int a = Math.round(((argb >>> 24) & 0xFF) * Mth.clamp(alpha, 0f, 1f));
        if (a <= 0) {
            return;
        }
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        for (int i = 0; i < segs; i++) {
            float t0 = from + span * i / segs;
            float t1 = from + span * (i + 1) / segs;
            float c0 = Mth.cos(t0);
            float s0 = Mth.sin(t0);
            float c1 = Mth.cos(t1);
            float s1 = Mth.sin(t1);
            vertex(vc, m, (float) (r0 * c0), yLo, (float) (r0 * s0), r, g, b, a);
            vertex(vc, m, (float) (r0 * c1), yLo, (float) (r0 * s1), r, g, b, a);
            vertex(vc, m, (float) (r1 * c1), yHi, (float) (r1 * s1), r, g, b, a);
            vertex(vc, m, (float) (r1 * c0), yHi, (float) (r1 * s0), r, g, b, a);
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f m,
                             float x0, float y0, float x1, float y1, int argb, float alpha) {
        int a = Math.round(((argb >>> 24) & 0xFF) * Mth.clamp(alpha, 0f, 1f));
        if (a <= 0) {
            return;
        }
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        vertex(vc, m, x0, y0, 0f, r, g, b, a);
        vertex(vc, m, x0, y1, 0f, r, g, b, a);
        vertex(vc, m, x1, y1, 0f, r, g, b, a);
        vertex(vc, m, x1, y0, 0f, r, g, b, a);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, float x, float y, float z,
                               int r, int g, int b, int a) {
        vc.vertex(m, x, y, z).color(r, g, b, a).endVertex();
    }
}
