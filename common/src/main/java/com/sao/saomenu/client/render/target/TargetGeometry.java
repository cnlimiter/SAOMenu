package com.sao.saomenu.client.render.target;

import net.minecraft.util.Mth;

/**
 * 3D 环绕血条的纯几何与门控:视线锥、弧带角度、壳体尺寸、淡入淡出与目标评分。
 * 类内不持有状态,不依赖渲染。
 */
public final class TargetGeometry {

    /** 弧带跨越的总角度(度):主弧 + 间隙 + 尾块。 */
    public static final float ARC_DEGREES = 98f;
    /** 整条弧的分段数,越多越圆滑。 */
    public static final int SEGMENTS = 28;
    /** 尾块占总弧的角度(度)。 */
    public static final float TAIL_DEGREES = 12f;
    /** 主弧与尾块之间的角度间隙(度)。 */
    public static final float TAIL_GAP_DEGREES = 6f;

    /** 环绕半径相对身宽的外扩量(格)。 */
    public static final double RADIUS_MARGIN = 0.80;
    /**
     * 弧带纵向高度:按「身宽」而非体高缩放。
     * 高个子生物(巨人/末影人)若按体高算,环会变成一条肥得不协调的腰带;
     * 身宽才决定环的周长,带厚跟身宽走比例才稳定。
     */
    public static final float BAND_HEIGHT_PER_WIDTH = 0.34f;
    /** 弧带高度下限/上限(格)。 */
    public static final float BAND_H_MIN = 0.14f;
    public static final float BAND_H_MAX = 0.32f;
    /**
     * 弧带所在高度相对体高的比例。
     *
     * <p>0.80 会把环挂到高个子生物(巨人/末影人)的脖子和头上;
     * 0.55 对常规生物落在胸腹之间,对高个子也稳定落在躯干中段。</p>
     */
    public static final float BAND_Y_FRAC = 0.55f;
    /**
     * 壳体径向厚度(格)。
     *
     * <p>整条带子只有内外两个同心面,厚度就是两者的半径差。
     * 旧实现是「暗槽 + 玻璃壳 + 光带 + 包边」四层半径互相穿插的曲面,
     * 一旦渲染器不按提交顺序出批(光影),外层就会盖住读数。</p>
     */
    public static final double BAND_THICK = 0.05;
    /** 上下亮边条各占带高的比例;中间剩余部分才是血量读数区。 */
    public static final float EDGE_FRAC = 0.17f;
    /**
     * 视线偏离生物轮廓多少弧度以内算完全显示。
     *
     * <p>门控比的是「视线与生物轮廓的夹角」而非与中心点的夹角,
     * 所以走到近处、生物占满半个屏幕时,看向它身上任意位置都算看向它。</p>
     */
    public static final float LOOK_FULL_MARGIN = 0.06f;
    /** 超出轮廓多少弧度后完全隐藏(约 18°)。 */
    public static final float LOOK_FADE_MARGIN = 0.32f;
    /** 生效距离(格)。 */
    public static final double MAX_DISTANCE = 42;
    /**
     * 同时显示的目标上限。
     *
     * <p>视野里生物一多,满屏环带与文字会糊成一片、也读不出哪个是当前目标;
     * 按「视线对准程度 + 距离」排序只留前几个。落选目标走既有的淡出通道,
     * 不会硬切。</p>
     */
    public static final int MAX_TARGETS = 3;
    /** 排序权重:视线对准占比(其余归距离)。 */
    public static final float SCORE_LOOK_WEIGHT = 0.65f;
    /** 每 tick 的淡入/淡出步长。 */
    public static final float FADE_STEP = 0.18f;
    /**
     * 弧带朝向每 tick 最大转速(度)。
     *
     * <p>弧带锚定生物朝向,但怪物 AI 每 tick 都在调整身体角度,
     * 直接跟随会疯转;限速后环带只缓慢优雅地转动,小抖动被滤平。</p>
     */
    public static final float ROT_SPEED_DEG_PER_TICK = 2.5f;

    private TargetGeometry() {
    }

    /**
     * 目标优先级评分:越大越该显示。
     *
     * <p>两项加权:视线对准程度({@code gate},已含生物角半径,占
     * {@link #SCORE_LOOK_WEIGHT})与距离近度(占其余)。准星正对的优先,
     * 同样对准程度下近的优先。</p>
     *
     * @param gate 视线门控 0-1(见 {@link #lookFactor})
     * @param dist 相机到目标的距离(格)
     */
    public static float targetScore(float gate, double dist) {
        float near = (float) (1.0 - Mth.clamp(dist / MAX_DISTANCE, 0.0, 1.0));
        return Mth.clamp(gate, 0f, 1f) * SCORE_LOOK_WEIGHT
                + near * (1f - SCORE_LOOK_WEIGHT);
    }

    /**
     * 视线对准程度:1 表示看在生物身上,0 表示视线之外。
     *
     * @param angleToCenter 视线与「相机→生物中心」的夹角(弧度)
     * @param angularRadius 生物轮廓相对相机的角半径(弧度)
     */
    public static float lookFactor(float angleToCenter, float angularRadius) {
        float full = angularRadius + LOOK_FULL_MARGIN;
        float fade = angularRadius + LOOK_FADE_MARGIN;
        if (angleToCenter <= full) {
            return 1f;
        }
        if (angleToCenter >= fade) {
            return 0f;
        }
        return 1f - (angleToCenter - full) / (fade - full);
    }

    /**
     * 生物相对相机的角半径(弧度):体型越大、距离越近,张角越大。
     */
    public static float angularRadius(float bbWidth, float bbHeight, double distance) {
        double half = Math.max(bbWidth, bbHeight) * 0.5;
        return (float) Math.atan2(half, Math.max(0.5, distance));
    }

    /** 主弧跨越角度(弧度):总弧减去尾块与间隙。 */
    public static float arcSpan() {
        return (ARC_DEGREES - TAIL_DEGREES - TAIL_GAP_DEGREES) * Mth.DEG_TO_RAD;
    }

    /** 血量填充所占的弧角(弧度)。 */
    public static float fillAngle(float frac) {
        return arcSpan() * Mth.clamp(frac, 0f, 1f);
    }

    /** 弧带起始角(弧度):以正对相机的角为中点铺开主弧。 */
    public static float arcStart(float midAngleRad) {
        return midAngleRad - arcSpan() / 2f;
    }

    /**
     * 血量段的起始角(弧度)。
     *
     * <p>环上角 t 增大时点沿逆时针移动;血量段锚定在主弧的高角端、向低角方向生长。
     * 弧带锚定生物朝向后,生物怎么转、血量都恒定从同一端起读——
     * 环跟着身体转,读数像刻在环上一样随之转动(参照 SAO 实拍)。</p>
     */
    public static float fillStart(float start, float frac) {
        return start + arcSpan() - fillAngle(frac);
    }

    /** 环绕半径:随身宽增长,并给瘦长生物一个下限。 */
    public static double ringRadius(float bbWidth) {
        return Math.max(0.42, bbWidth * 0.5 + RADIUS_MARGIN);
    }

    /**
     * 弧带纵向高度:按身宽缩放并钳制。
     *
     * <p>刻意不按体高算——巨人这种高瘦生物按体高会把环撑成肥腰带;
     * 身宽决定环的周长,带厚跟身宽走比例,任何体型都协调。</p>
     */
    public static float bandHeight(float bbWidth) {
        return Mth.clamp(bbWidth * BAND_HEIGHT_PER_WIDTH, BAND_H_MIN, BAND_H_MAX);
    }

    /** 上下亮边条的高度(格);中间剩余部分才是血量读数区。 */
    public static float edgeHeight(float bandH) {
        return Math.max(0.012f, bandH * EDGE_FRAC);
    }

    /** 淡入淡出一步:朝目标值逼近固定步长。 */
    public static float stepAlpha(float current, float target) {
        if (current < target) {
            return Math.min(target, current + FADE_STEP);
        }
        return Math.max(target, current - FADE_STEP);
    }

    /** 弧带朝向一步:朝目标角沿最短方向推进,步长被限速钳制(纯函数,可测)。 */
    public static float approachAngle(float current, float target, float maxStepDeg) {
        float diff = Mth.wrapDegrees(target - current);
        diff = Mth.clamp(diff, -maxStepDeg, maxStepDeg);
        return current + diff;
    }

    /**
     * 环带贴脸淡出系数:相机到环心的水平距离低于此值时环带开始淡出,归一化到 0~1。
     *
     * <p>镜头贴近生物时环带近侧离相机只剩零点几格,被透视放大成
     * 「漂在生物旁边的一大片弧」;此时收掉环带,只留头顶菱形与 HUD 文字。</p>
     */
    public static float proximityFade(double horizontalDist) {
        return Mth.clamp((float) ((horizontalDist - 1.6) / (2.8 - 1.6)), 0f, 1f);
    }
}
