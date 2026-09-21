package com.sao.saomenu.client.menu;

import com.sao.saomenu.client.SAOConfig;
import com.sao.saomenu.client.SAODualWield;
import com.sao.saomenu.client.SAONotification;
import com.sao.saomenu.client.SAOAdvancementsScreen;
import com.sao.saomenu.client.SAOSettingsScreen;
import com.sao.saomenu.client.SAOStatsScreen;
import com.sao.saomenu.party.InviteC2S;
import com.sao.saomenu.party.LeaveC2S;
import com.sao.saomenu.party.DualWieldC2S;
import com.sao.saomenu.ui.SaoText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 内置面板:个人 / 队伍 / 好友 / 设置。
 *
 * <p>菜单的全部条目与行为都在这里,菜单屏只负责渲染与命中。新增一个面板 =
 * 写一个 {@link SaoPanel} 再 {@link SaoMenuRegistry#register} 即可,
 * 不必碰菜单屏——这正是"自由添加菜单"的落点。</p>
 *
 * <p>注册顺序决定主按钮列顺序;预览自检按主按钮下标点击,所以顺序不能随意改。</p>
 */
public final class SaoPanels {

    public static final String PROFILE = "profile";
    public static final String PARTY = "party";
    public static final String FRIENDS = "friends";
    public static final String SETTINGS = "settings";

    /** 背包/在线玩家列的重建间隔:菜单屏每帧都会取子列,不缓存就等于每帧重建。 */
    private static final long LIST_TTL_MS = 1000L;

    private SaoPanels() {
    }

    /** 内置面板,按主按钮列从上到下的顺序。 */
    public static List<SaoPanel> all() {
        return List.of(
                SaoPanel.of(PROFILE, "info", SaoPanels::profileItems),
                SaoPanel.of(PARTY, "party", SaoPanels::partyItems),
                SaoPanel.of(FRIENDS, "msg", SaoPanels::friendsItems),
                SaoPanel.of(SETTINGS, "setting", SaoPanels::settingsItems));
    }

    // ------------------------------------------------------------ 个人

    private static List<MenuEntry> profileItems() {
        return List.of(
                MenuEntry.submenu("saomenu.menu.skill", "item_status", SaoPanels::skillItems),
                MenuEntry.submenu("saomenu.menu.equip", "item_weapon", SaoPanels::equipItems),
                MenuEntry.submenu("saomenu.menu.items", "item_bag",
                        MenuContext.cached(SaoPanels::inventoryItems, LIST_TTL_MS)),
                MenuEntry.action("saomenu.menu.map", "item_map", ctx -> {
                    ctx.host().playPanel();
                    ctx.host().toggleMap();
                }));
    }

    /**
     * 剑技列。
     *
     * <p>目前除二刀流外都还没有系统支撑,点一下给"暂未开放"提示。
     * 将来把技能注册表接上来之后,这里改为遍历注册的技能即可,菜单屏无需改动。</p>
     */
    private static List<MenuEntry> skillItems() {
        return List.of(
                MenuEntry.action("saomenu.skill.dual_wield", "item_weapon", SaoPanels::dualWield),
                skill("saomenu.skill.horizontal"),
                skill("saomenu.skill.slant"),
                skill("saomenu.skill.vertical"),
                skill("saomenu.skill.linear"),
                MenuEntry.action("saomenu.skill.sonic_leap", "item_run", SaoPanels::notYet),
                skill("saomenu.skill.starburst"));
    }

    private static MenuEntry skill(String labelKey) {
        return MenuEntry.action(labelKey, "item_weapon", SaoPanels::notYet);
    }

    private static void notYet(MenuContext ctx) {
        ctx.host().playClick();
        SAONotification.push(SaoText.tr("saomenu.coming_soon"), "");
    }

    private static List<MenuEntry> equipItems() {
        return List.of(
                MenuEntry.equipColumn("saomenu.menu.weapon", "item_weapon", MenuEntry.EquipKind.WEAPON),
                MenuEntry.equipColumn("saomenu.menu.armor", "item_armor", MenuEntry.EquipKind.ARMOR),
                MenuEntry.equipColumn("saomenu.menu.trinket", "item_ring", MenuEntry.EquipKind.TRINKET));
    }

    /**
     * 背包条目:快捷栏 + 主背包的非空物品。
     *
     * <p>排序优先级:置顶(按置顶先后)→ 手动拖动顺序 → 背包槽位。
     * 键用物品注册名而不是槽位,所以丢掉后重新捡起仍然保持置顶/顺序。</p>
     */
    private static List<MenuEntry> inventoryItems() {
        List<MenuEntry> list = new ArrayList<>();
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null) {
            for (int i = 0; i < 36; i++) {
                ItemStack s = p.getInventory().getItem(i);
                if (!s.isEmpty()) {
                    list.add(MenuEntry.item(s, i));
                }
            }
            list.sort((a, b) -> {
                int pa = pinOrderOf(a.stack());
                int pb = pinOrderOf(b.stack());
                if (pa != pb) {
                    return Integer.compare(pa, pb);
                }
                int oa = orderIndexOf(a.stack());
                int ob = orderIndexOf(b.stack());
                if (oa != ob) {
                    return Integer.compare(oa, ob);
                }
                return Integer.compare(a.invSlot(), b.invSlot());
            });
        }
        if (list.isEmpty()) {
            list.add(MenuEntry.of("saomenu.inv.empty", "item_bag"));
        }
        return list;
    }

    /** 物品的置顶顺序号;未置顶为 MAX_VALUE(排序时沉底)。 */
    public static int pinOrderOf(ItemStack st) {
        if (st == null || st.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        return SAOConfig.pinOrder(itemId(st));
    }

    /** 物品的手动顺序号;不在自定义顺序里为 MAX_VALUE。 */
    public static int orderIndexOf(ItemStack st) {
        if (st == null || st.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        return SAOConfig.orderIndex(itemId(st));
    }

    /** 物品注册名;取不到返回空串。 */
    public static String itemId(ItemStack st) {
        if (st == null || st.isEmpty()) {
            return "";
        }
        var key = BuiltInRegistries.ITEM.getKey(st.getItem());
        return key == null ? "" : key.toString();
    }

    // ------------------------------------------------------------ 队伍

    private static List<MenuEntry> partyItems() {
        return List.of(
                MenuEntry.submenu("saomenu.menu.invite", "item_status",
                        MenuContext.cached(SaoPanels::inviteItems, LIST_TTL_MS)),
                MenuEntry.action("saomenu.menu.leave_team", "item_bag", ctx -> {
                    new LeaveC2S().sendToServer();
                    ctx.host().playClick();
                }));
    }

    /** 在线玩家列(被邀请人);自己不在其中,一个都没有时给占位行。 */
    private static List<MenuEntry> inviteItems() {
        List<MenuEntry> list = new ArrayList<>();
        var conn = Minecraft.getInstance().getConnection();
        LocalPlayer self = Minecraft.getInstance().player;
        if (conn != null) {
            for (PlayerInfo info : conn.getOnlinePlayers()) {
                String name = info.getProfile().getName();
                if (self != null && name.equals(self.getGameProfile().getName())) {
                    continue;
                }
                list.add(MenuEntry.action(name, "item_status", SaoPanels::invite));
            }
        }
        if (list.isEmpty()) {
            list.add(MenuEntry.of("saomenu.panel.no_players", "item_status"));
        }
        return list;
    }

    private static void invite(MenuContext ctx) {
        String target = ctx.entry() == null ? null : ctx.entry().label();
        if (target != null && !target.isEmpty()) {
            new InviteC2S(target).sendToServer();
            SAONotification.push(SaoText.tr("saomenu.party.notify.sent.title"),
                    SaoText.tr("saomenu.party.notify.sent.msg", target));
        }
        ctx.host().playClick();
    }

    // ------------------------------------------------------------ 好友

    private static List<MenuEntry> friendsItems() {
        return List.of(
                MenuEntry.action("saomenu.menu.advancements", "item_status", ctx -> {
                    ctx.host().playClick();
                    ctx.openScreen(new SAOAdvancementsScreen(ctx.screen()));
                }),
                MenuEntry.action("saomenu.menu.refresh", "item_bag", SaoPanels::switchToParty));
    }

    private static void switchToParty(MenuContext ctx) {
        ctx.host().selectMain(SaoMenuRegistry.indexOf(PARTY));
        ctx.host().playPanel();
    }

    // ------------------------------------------------------------ 设置

    private static List<MenuEntry> settingsItems() {
        return List.of(
                MenuEntry.action("saomenu.menu.config", "item_config", ctx -> {
                    ctx.host().playClick();
                    ctx.openScreen(new SAOSettingsScreen(ctx.screen()));
                }),
                MenuEntry.action("saomenu.menu.options", "item_status", ctx -> {
                    ctx.host().playClick();
                    ctx.openScreen(new OptionsScreen(ctx.screen(), ctx.minecraft().options));
                }),
                MenuEntry.action("saomenu.menu.close", "item_logout", SaoPanels::requestClose));
    }

    private static void requestClose(MenuContext ctx) {
        ctx.host().openCloseConfirm();
    }

    // ------------------------------------------------------------ 二刀流

    /**
     * 二刀流:挑两把剑交给服务端,并延后切史诗战斗的战斗模式。
     *
     * <p>延后切模式是必须的:Epic Fight 进战斗模式时按"当前主手武器"解析动作集,
     * 必须等服务端把剑同步回客户端之后再切,否则它按空手解析,
     * 表现为切了模式但没进入持剑架势。</p>
     */
    private static void dualWield(MenuContext ctx) {
        ctx.host().playClick();
        LocalPlayer p = ctx.player();
        int[] slots = p == null ? null : SAODualWield.findTwoSwords(p);
        if (slots == null) {
            SAONotification.push(SaoText.tr("saomenu.skill.dual_wield.need_two"), "");
            return;
        }
        new DualWieldC2S(slots[0], slots[1]).sendToServer();
        SAODualWield.requestBattleMode();
        SAONotification.push(SaoText.tr("saomenu.skill.dual_wield"),
                SAODualWield.epicFightPresent()
                        ? SaoText.tr("saomenu.skill.dual_wield.on")
                        : SaoText.tr("saomenu.skill.dual_wield.no_ef"));
    }
}
