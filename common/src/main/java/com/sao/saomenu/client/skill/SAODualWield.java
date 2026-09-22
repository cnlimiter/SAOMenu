package com.sao.saomenu.client.skill;

import com.sao.saomenu.SAOMenu;
import com.sao.saomenu.skill.DualWieldSkill;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * 「二刀流」技能:主手 + 副手各装一把剑,并切换到史诗战斗(Epic Fight)的战斗模式。
 *
 * <p>装备走服务端权威路径({@code DualWieldC2S}),客户端只负责挑选槽位;
 * 战斗模式切换调用 Epic Fight 玩家补丁公开方法,由它同步自身模式。</p>
 *
 * <p>Epic Fight 通过反射按需接入:没装该模组时二刀流仍会装备双剑,
 * 只是不切换战斗模式(不抛异常、不产生硬依赖)。</p>
 */
public final class SAODualWield {

    /** Epic Fight 的能力入口与客户端玩家补丁类。 */
    private static final String EF_CAPS = "yesman.epicfight.world.capabilities.EpicFightCapabilities";
    private static final String EF_LOCAL_PATCH = "yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch";

    private static boolean efResolved;
    private static boolean efPresent;

    private SAODualWield() {
    }

    /** Epic Fight 是否可用(能力类能取到)。 */
    public static boolean epicFightPresent() {
        resolve();
        return efPresent;
    }

    private static void resolve() {
        if (efResolved) {
            return;
        }
        efResolved = true;
        try {
            Class.forName(EF_CAPS);
            Class.forName(EF_LOCAL_PATCH);
            efPresent = true;
        } catch (ReflectiveOperationException | LinkageError e) {
            SAOMenu.LOGGER.info("[SAOMenu] 未检测到史诗战斗,二刀流只装备双剑: {}", e.toString());
        }
    }

    /**
     * 挑两把剑的槽位。
     *
     * <p>返回 {@code [主手来源, 副手来源]};槽位是背包下标 0-35,
     * 或哨兵 {@link DualWieldSkill#KEEP_HAND}(该手已经握着剑)。找不到两把返回 null。</p>
     *
     * <p>此前把「主手已握剑」记成 {@code inventory.selected}(0-8),
     * 与「背包扫描时跳过该下标」混在一起,当剑在副手 + 背包各一把时
     * 计数会漏一把,表现为「副手有剑却提示需要两把」。现在主手/副手
     * 各自独立判定,背包扫描只跳过真正已被占用的下标。</p>
     */
    public static int[] findTwoSwords(Player p) {
        boolean mainIsSword = DualWieldSkill.isSword(p.getMainHandItem());
        boolean offIsSword = DualWieldSkill.isSword(p.getOffhandItem());
        if (mainIsSword && offIsSword) {
            return new int[]{DualWieldSkill.KEEP_HAND, DualWieldSkill.KEEP_HAND};
        }
        int first = -1;
        int selected = p.getInventory().selected;
        for (int i = 0; i < 36; i++) {
            if ((mainIsSword && i == selected) || !DualWieldSkill.isSword(p.getInventory().getItem(i))) {
                continue;
            }
            if (offIsSword) {
                return new int[]{i, DualWieldSkill.KEEP_HAND};
            }
            if (mainIsSword) {
                return new int[]{DualWieldSkill.KEEP_HAND, i};
            }
            if (first >= 0) {
                return new int[]{first, i};
            }
            first = i;
        }
        return null;
    }


    /** 待切战斗模式的剩余 tick;>0 时每 tick 递减,归零那帧执行切换。 */
    private static int pendingModeTicks;

    /**
     * 请求切战斗模式(延后 3 tick 执行)。
     *
     * <p>不能立即切:装备是服务端权威的,发包后主手的剑要等服务端回传才到位;
     * 而 Epic Fight 进战斗模式时按「当前主手武器」解析动作集,切太早会按
     * 空手解析,表现为进了战斗模式却没有持剑架势。</p>
     */
    public static void requestBattleMode() {
        pendingModeTicks = 3;
    }

    /** 客户端运行时每 tick 调用:到点执行延后的模式切换。 */
    public static void tick() {
        if (pendingModeTicks > 0 && --pendingModeTicks == 0) {
            toBattleMode();
        }
    }

    public static void reset() {
        pendingModeTicks = 0;
    }

    /**
     * 切到史诗战斗的战斗模式。
     *
     * <p>正确入口是 {@code EpicFightCapabilities.getEntityPatch(player, LocalPlayerPatch.class)
     * .toEpicFightMode(true)}——它内部会自己发 {@code CPChangePlayerMode} 包同步服务端,
     * 并处理相机/技能 UI 的连带切换。曾试过硬按它的 SWITCH_MODE 按键映射,
     * 但 Epic Fight 的按键走自家 InputManager 事件总线,KeyMapping.setDown
     * 不会触发,表现为点了没反应。</p>
     */
    public static void toBattleMode() {
        resolve();
        if (!efPresent) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        try {
            Class<?> caps = Class.forName(EF_CAPS);
            Class<?> patchCls = Class.forName(EF_LOCAL_PATCH);
            Object patch = caps.getMethod("getEntityPatch",
                    net.minecraft.world.entity.Entity.class, Class.class)
                    .invoke(null, mc.player, patchCls);
            if (patch != null) {
                patchCls.getMethod("toEpicFightMode", boolean.class).invoke(patch, true);
            }
        } catch (ReflectiveOperationException | LinkageError e) {
            SAOMenu.LOGGER.warn("[SAOMenu] 切换史诗战斗战斗模式失败: {}", e.toString());
        }
    }
}
