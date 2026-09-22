package com.sao.saomenu.client.render.target;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 目标追踪状态:淡入强度、弧带朝向、本帧候选与名额/遮挡门控。
 * 容器与候选对象跨帧复用,不每帧整批拷贝。
 */
final class TargetTracker {

    /** 每个实体的当前显示强度(视线离开后平滑淡出)。 */
    private static final Map<Integer, Float> ALPHA = new HashMap<>();
    /** 每个实体的弧带当前朝向角(度,限速追赶生物朝向)。 */
    private static final Map<Integer, Float> BAND_ROT = new HashMap<>();
    private static final List<Candidate> CANDS = new ArrayList<>();
    private static final Set<Integer> PRESENT = new HashSet<>();
    private static final Set<UUID> PARTY_IDS = new HashSet<>();

    /** 上一帧时间戳(算帧步长用)。 */
    private static long lastFrameAt;
    private static float dtTicks;
    private static int candCount;

    private TargetTracker() {
    }

    /** 本帧的目标候选(第一趟收集、评分排序后第二趟才绘制)。 */
    static final class Candidate {
        LivingEntity entity;
        Vec3 center;
        double dist;
        float gate;

        void set(LivingEntity entity, Vec3 center, double dist, float gate) {
            this.entity = entity;
            this.center = center;
            this.dist = dist;
            this.gate = gate;
        }

        void clear() {
            entity = null;
            center = null;
        }
    }

    static void beginFrame(Minecraft mc, long now) {
        dtTicks = lastFrameAt == 0L ? 1f : Mth.clamp((now - lastFrameAt) / 50f, 0f, 3f);
        lastFrameAt = now;
        PRESENT.clear();
        candCount = 0;
        collectPartyIds(mc);
    }

    static boolean isParty(UUID id) {
        return PARTY_IDS.contains(id);
    }

    static void markPresent(int entityId) {
        PRESENT.add(entityId);
    }

    static void addCandidate(LivingEntity entity, Vec3 center, double dist, float gate) {
        Candidate c;
        if (candCount < CANDS.size()) {
            c = CANDS.get(candCount);
        } else {
            c = new Candidate();
            CANDS.add(c);
        }
        c.set(entity, center, dist, gate);
        candCount++;
    }

    static void sortCandidates() {
        if (candCount > 1) {
            CANDS.subList(0, candCount).sort((c1, c2) -> Float.compare(
                    TargetGeometry.targetScore(c2.gate, c2.dist),
                    TargetGeometry.targetScore(c1.gate, c1.dist)));
        }
    }

    static int candidateCount() {
        return candCount;
    }

    static Candidate candidate(int i) {
        return CANDS.get(i);
    }

    static float stepAlphaToward(int entityId, float gate) {
        float a = TargetGeometry.stepAlpha(ALPHA.getOrDefault(entityId, 0f), gate);
        ALPHA.put(entityId, a);
        return a;
    }

    static float advanceBandRot(int entityId, float targetRot) {
        float bandRot = TargetGeometry.approachAngle(
                BAND_ROT.getOrDefault(entityId, targetRot), targetRot,
                TargetGeometry.ROT_SPEED_DEG_PER_TICK * dtTicks);
        BAND_ROT.put(entityId, bandRot);
        return bandRot;
    }

    /**
     * 相机到环带中心之间是否有无遮挡的视线。
     *
     * <p>用原版 {@code clip}(实体碰撞形状,不检测液体)做一次方块射线:
     * 命中点到相机的距离小于到目标的距离,说明中间有方块挡着(隔墙透视)。
     * 目标自身贴着的方块不会误挡——环带中心悬在生物腰身外侧,
     * 命中点距离只会小于「目标距离 − 身宽半径」这类明显差距才判遮挡。
     * clipContext 的 collidable=false 顺便排除碰撞形状存在但不可选中的方块
     * (如发光地衣附着的方块)造成的边缘误判。</p>
     */
    static boolean hasLineOfSight(Minecraft mc, Vec3 camPos, Vec3 target) {
        BlockHitResult hit = mc.level.clip(
                new ClipContext(camPos, target,
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        null));
        return hit.getType() == BlockHitResult.Type.MISS;
    }

    /** 离开视野的实体逐步淡出后清理。 */
    static void fadeAbsent() {
        for (Iterator<Map.Entry<Integer, Float>> it = ALPHA.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Float> en = it.next();
            if (PRESENT.contains(en.getKey())) {
                continue;
            }
            float a = TargetGeometry.stepAlpha(en.getValue(), 0f);
            if (a <= 0.01f) {
                it.remove();
                BAND_ROT.remove(en.getKey());
            } else {
                en.setValue(a);
            }
        }
    }

    static void reset() {
        ALPHA.clear();
        BAND_ROT.clear();
        lastFrameAt = 0L;
        PRESENT.clear();
        PARTY_IDS.clear();
        for (int i = 0; i < CANDS.size(); i++) {
            CANDS.get(i).clear();
        }
        candCount = 0;
    }

    /** 当前正在显示血条的目标数(预览自检用)。 */
    static int visibleCount() {
        int n = 0;
        for (float a : ALPHA.values()) {
            if (a > 0.5f) {
                n++;
            }
        }
        return n;
    }

    /** 当前队伍里在线(可渲染)队友的 UUID 集;无队伍为空集。 */
    private static void collectPartyIds(Minecraft mc) {
        PARTY_IDS.clear();
        if (!com.sao.saomenu.client.party.SAOClientPartyState.inParty()) {
            return;
        }
        String self = mc.player != null ? mc.player.getGameProfile().getName() : "";
        var conn = mc.getConnection();
        if (conn == null) {
            return;
        }
        for (String name : com.sao.saomenu.client.party.SAOClientPartyState.teamMembers()) {
            if (name.equals(self)) {
                continue;
            }
            var info = conn.getPlayerInfo(name);
            if (info != null) {
                PARTY_IDS.add(info.getProfile().getId());
            }
        }
    }
}
