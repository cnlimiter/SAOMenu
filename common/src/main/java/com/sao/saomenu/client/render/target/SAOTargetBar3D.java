package com.sao.saomenu.client.render.target;

import com.sao.saomenu.client.hud.SAOBossBanner;
import com.sao.saomenu.config.SAOConfig;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * SAO 3D 环绕目标血条的世界/HUD 桥:扫描附近生物,委托追踪、几何、图元与标签。
 *
 * <p>只有玩家视线对准生物时才淡入显示。头顶另有正对相机的红色菱形标识,
 * 名称与血量文字则由 {@link #renderLabels} 画在 HUD 层。</p>
 *
 * <h2>为什么每一层都不重叠、都不透明</h2>
 * <p>旧实现把「暗槽 / 半透明玻璃壳 / 血量光带」画成半径互相穿插的多层曲面,
 * 靠绘制顺序与半透明混合叠出观感。装了 Iris/Oculus 光影(Photon 等)的客户端上
 * 这会整条退化成一堵白墙:自建的 POSITION_COLOR 图元被光影当作不透明几何写进
 * gbuffer,顶点 alpha 与 NO_DEPTH_TEST 都被忽略,批次先后也由光影自己的 pass 决定,
 * 于是亮白玻璃壳压住了血量带,血条只剩一圈白。</p>
 *
 * <p>所以现在整条带子是一个单薄壳体:半径 {@link TargetGeometry#BAND_THICK} 的外读数面、
 * 上下两圈环面(厚度)、内衬面。任意两个面在径向、纵向、角度三个维度上
 * 至少有一个维度完全不相交,颜色一律不透明——光影是否尊重 alpha 与绘制顺序,
 * 都不影响血量读数。</p>
 *
 * <h2>为什么弧带锚定相机而不是生物朝向</h2>
 * <p>锚定生物朝向时,绕到生物背后就只能看到壳的背面,读数面被自身挡住;
 * 旧代码为此给填充层单独开了「无深度测试」的 X 射线图元,而光影会忽略该状态。
 * 现在弧中点恒定朝向相机,读数面永远对着观察者,血量恒定自屏幕左侧起涨,
 * 不依赖任何深度技巧。</p>
 *
 * <p>视线锥判定({@link TargetGeometry#lookFactor})、弧带几何({@link TargetGeometry#arcSpan}、
 * {@link TargetGeometry#fillAngle})与淡入淡出({@link TargetGeometry#stepAlpha})
 * 是不依赖渲染的纯函数,可单元测试。</p>
 */
public final class SAOTargetBar3D {

    private SAOTargetBar3D() {
    }

    /**
     * 世界渲染阶段调用:扫描附近生物,为视线锥内的目标绘制环绕血条。
     *
     * @param pose 世界渲染的 PoseStack(已应用相机旋转、未应用相机平移)
     * @param src  世界渲染的缓冲源
     */
    public static void render(Minecraft mc, PoseStack pose, MultiBufferSource.BufferSource src,
                              float partialTick) {
        TargetLabels.beginFrame();
        if (!SAOConfig.showTargetBar() || mc.level == null || mc.player == null) {
            return;
        }
        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 camPos = cam.getPosition();
        org.joml.Vector3f lookV = cam.getLookVector();
        Vec3 look = new Vec3(lookV.x(), lookV.y(), lookV.z());
        long now = net.minecraft.Util.getMillis();
        // 帧步长(以 50ms/tick 为基准,钳到 3 tick):限速转动的推进量按帧缩放
        TargetTracker.beginFrame(mc, now);

        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity le) || le == mc.player || !le.isAlive()) {
                continue;
            }
            if (le instanceof Player && mc.player.isPassenger()) {
                continue;
            }
            // 队友:画绿三角,跳过敌对红菱形逻辑(不占目标名额)
            if (le instanceof Player && TargetTracker.isParty(le.getUUID())) {
                TargetTracker.markPresent(le.getId());
                TargetWorldDraw.drawPartyMarker(mc, pose, src, le, camPos, partialTick, now);
                continue;
            }
            Vec3 center = le.getPosition(partialTick)
                    .add(0, le.getBbHeight() * TargetGeometry.BAND_Y_FRAC, 0);
            // 距离平方早退:视野内实体多时省掉逐实体的 sqrt/normalize/acos
            double distSq = center.distanceToSqr(camPos);
            if (distSq > TargetGeometry.MAX_DISTANCE * TargetGeometry.MAX_DISTANCE || distSq < 0.16) {
                continue;
            }
            double dist = Math.sqrt(distSq);
            Vec3 dir = center.subtract(camPos).normalize();
            float angle = (float) Math.acos(Mth.clamp(look.dot(dir), -1.0, 1.0));
            float gate = TargetGeometry.lookFactor(angle,
                    TargetGeometry.angularRadius(le.getBbWidth(), le.getBbHeight(), dist));
            TargetTracker.addCandidate(le, center, dist, gate);
        }
        // 视线对准优先、同等对准下近的优先
        TargetTracker.sortCandidates();

        int shown = 0;
        int n = TargetTracker.candidateCount();
        for (int i = 0; i < n; i++) {
            TargetTracker.Candidate c = TargetTracker.candidate(i);
            LivingEntity le = c.entity;
            TargetTracker.markPresent(le.getId());
            float gate = c.gate;
            // 超出名额:门控清零走淡出(不是硬切),已淡完的直接跳过绘制
            if (gate > 0.01f && shown >= TargetGeometry.MAX_TARGETS) {
                gate = 0f;
            }
            // 隔墙目标不显示:相机到环带中心的视线被方块挡住时清零门控,
            // 环带/菱形/名称血量文字与 Boss 横幅共用 gate,一起随淡出收掉。
            // 只对已过视线锥且在名额内的候选做 clip,避免全场逐帧射线检测。
            if (gate > 0.01f && !TargetTracker.hasLineOfSight(mc, camPos, c.center)) {
                gate = 0f;
            }
            // Boss 横幅登记(视线门控,HUD 层绘制)
            if (SAOBossBanner.isBoss(le)) {
                SAOBossBanner.seen(le, gate);
            }
            float a = TargetTracker.stepAlphaToward(le.getId(), gate);
            if (a <= 0.01f) {
                continue;
            }
            if (gate > 0.01f) {
                shown++;
            }
            TargetWorldDraw.drawRing(mc, pose, src, le, c.center, camPos, a, now, partialTick);
        }
        TargetTracker.fadeAbsent();
    }

    /**
     * HUD 阶段调用:把本帧收集到的名称/血量文字画在屏幕上。
     */
    public static void renderLabels(GuiGraphics g, Minecraft mc) {
        TargetLabels.render(g, mc);
    }

    /** 世界或维度切换时清空淡入状态与标签。 */
    public static void reset() {
        TargetTracker.reset();
        TargetLabels.reset();
    }

    /** 当前正在显示血条的目标数(预览自检用)。 */
    public static int visibleCount() {
        return TargetTracker.visibleCount();
    }

    /** 本帧待绘制的 HUD 文字条数(预览自检用)。 */
    public static int labelCount() {
        return TargetLabels.count();
    }
}
