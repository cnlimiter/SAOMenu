package com.sao.saomenu.client.menu;

import com.sao.saomenu.client.hud.SAONotification;
import com.sao.saomenu.client.screen.SAOAdvancementsScreen;
import com.sao.saomenu.client.screen.settings.SAOSettingsScreen;
import com.sao.saomenu.config.SAOConfig;
import com.sao.saomenu.client.skill.SaoSkill;
import com.sao.saomenu.client.skill.SaoSkillRegistry;
import com.sao.saomenu.client.skill.SaoSkills;
import com.sao.saomenu.network.c2s.InviteC2S;
import com.sao.saomenu.network.c2s.LeaveC2S;
import com.sao.saomenu.ui.text.SaoText;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 内置面板:个人 / 队伍 / 好友 / 设置。
 *
 * <p>一级列是稳定列表;背包与邀请两列由本类持有的有界缓存供应。
 * 置顶/换序、打开菜单、世界结束时 {@link #resetSession()} 立即失效。</p>
 */
public final class SaoPanels {

    public static final String PROFILE = "profile";
    public static final String PARTY = "party";
    public static final String FRIENDS = "friends";
    public static final String SETTINGS = "settings";

    /** 背包/在线玩家列的重建间隔:菜单屏每帧都会取子列,不缓存就等于每帧重建。 */
    private static final long LIST_TTL_MS = 1000L;

    private static final MenuListCache INVENTORY = new MenuListCache(SaoPanels::inventoryItems, LIST_TTL_MS);
    private static final MenuListCache INVITES = new MenuListCache(SaoPanels::inviteItems, LIST_TTL_MS);

    private static final List<MenuEntry> PROFILE_ITEMS = List.of(
            MenuEntry.submenu("saomenu.menu.skill", "item_status", SaoPanels::skillItems),
            MenuEntry.submenu("saomenu.menu.equip", "item_weapon", SaoPanels::equipItems),
            MenuEntry.submenu("saomenu.menu.items", "item_bag", INVENTORY),
            MenuEntry.action("saomenu.menu.map", "item_map", ctx -> {
                ctx.host().playPanel();
                ctx.host().toggleMap();
            }));

    private static final List<MenuEntry> EQUIP_ITEMS = List.of(
            MenuEntry.equipColumn("saomenu.menu.weapon", "item_weapon", MenuEntry.EquipKind.WEAPON),
            MenuEntry.equipColumn("saomenu.menu.armor", "item_armor", MenuEntry.EquipKind.ARMOR),
            MenuEntry.equipColumn("saomenu.menu.trinket", "item_ring", MenuEntry.EquipKind.TRINKET));

    private static final List<MenuEntry> PARTY_ITEMS = List.of(
            MenuEntry.submenu("saomenu.menu.invite", "item_status", INVITES),
            MenuEntry.action("saomenu.menu.leave_team", "item_bag", ctx -> {
                new LeaveC2S().sendToServer();
                ctx.host().playClick();
            }));

    private static final List<MenuEntry> FRIENDS_ITEMS = List.of(
            MenuEntry.action("saomenu.menu.advancements", "item_status", ctx -> {
                ctx.host().playClick();
                ctx.openScreen(new SAOAdvancementsScreen(ctx.screen()));
            }),
            MenuEntry.action("saomenu.menu.refresh", "item_bag", SaoPanels::switchToParty));

    private static final List<MenuEntry> SETTINGS_ITEMS = List.of(
            MenuEntry.action("saomenu.menu.config", "item_config", ctx -> {
                ctx.host().playClick();
                ctx.openScreen(new SAOSettingsScreen(ctx.screen()));
            }),
            MenuEntry.action("saomenu.menu.options", "item_status", ctx -> {
                ctx.host().playClick();
                ctx.openScreen(new OptionsScreen(ctx.screen(), ctx.minecraft().options));
            }),
            MenuEntry.action("saomenu.menu.close", "item_logout", SaoPanels::requestClose));

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

    /**
     * 失效背包/邀请缓存。置顶换序、打开菜单、世界结束时由运行时调用。
     */
    public static void resetSession() {
        INVENTORY.invalidate();
        INVITES.invalidate();
    }

    private static List<MenuEntry> profileItems() {
        return PROFILE_ITEMS;
    }

    /**
     * 剑技列:直接由 {@link SaoSkillRegistry} 生成。
     *
     * <p>菜单项不再知道任何具体技能——加一个技能只需注册它,技能列自动出现,
     * 且与技能快捷键走同一个激活入口,两个入口行为一致。</p>
     */
    private static List<MenuEntry> skillItems() {
        List<MenuEntry> out = new ArrayList<>();
        for (SaoSkill skill : SaoSkillRegistry.skills()) {
            out.add(MenuEntry.action(skill.nameKey(), skill.icon(), ctx -> {
                ctx.host().playClick();
                SaoSkills.activate(ctx.player(), skill);
            }));
        }
        return out;
    }

    private static List<MenuEntry> equipItems() {
        return EQUIP_ITEMS;
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

    private static List<MenuEntry> partyItems() {
        return PARTY_ITEMS;
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

    private static List<MenuEntry> friendsItems() {
        return FRIENDS_ITEMS;
    }

    private static void switchToParty(MenuContext ctx) {
        ctx.host().selectMain(SaoMenuRegistry.indexOf(PARTY));
        ctx.host().playPanel();
    }

    private static List<MenuEntry> settingsItems() {
        return SETTINGS_ITEMS;
    }

    private static void requestClose(MenuContext ctx) {
        ctx.host().openCloseConfirm();
    }

    /** 有界 TTL 缓存:只由本类持有两份,不再往全局列表登记。 */
    private static final class MenuListCache implements Supplier<List<MenuEntry>> {
        private final Supplier<List<MenuEntry>> source;
        private final long ttlMs;
        private List<MenuEntry> value;
        private long stamp;

        MenuListCache(Supplier<List<MenuEntry>> source, long ttlMs) {
            this.source = source;
            this.ttlMs = ttlMs;
        }

        void invalidate() {
            value = null;
        }

        @Override
        public List<MenuEntry> get() {
            long now = Util.getMillis();
            if (value == null || now - stamp > ttlMs) {
                value = source.get();
                stamp = now;
            }
            return value;
        }
    }
}
