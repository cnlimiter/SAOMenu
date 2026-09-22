package com.sao.saomenu.client.menu;

import com.sao.saomenu.SAOMenu;
import com.sao.saomenu.api.SaoUi;
import com.sao.saomenu.api.SaoUiRegistry;
import com.sao.saomenu.api.menu.MenuEntry;
import com.sao.saomenu.api.menu.MenuIcon;
import com.sao.saomenu.api.menu.SaoPanel;
import com.sao.saomenu.api.menu.MenuContext;
import com.sao.saomenu.client.screen.SAOStatsScreen;
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
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Builtin panels: profile / party / friends / settings.
 *
 * <p>First-level rows are stable lists. Inventory and invite columns are owned by
 * bounded caches on this class. Pin reorder, opening the menu, and leaving a world
 * call {@link #resetSession()}.</p>
 */
public final class SaoPanels {

    public static final ResourceLocation PROFILE = id("profile");
    public static final ResourceLocation PARTY = id("party");
    public static final ResourceLocation FRIENDS = id("friends");
    public static final ResourceLocation SETTINGS = id("settings");

    private static final long LIST_TTL_MS = 1000L;

    private static final MenuListCache INVENTORY = new MenuListCache(SaoPanels::inventoryItems, LIST_TTL_MS);
    private static final MenuListCache INVITES = new MenuListCache(SaoPanels::inviteItems, LIST_TTL_MS);
    private static final MenuListCache SKILLS = new MenuListCache(SaoPanels::skillItems, LIST_TTL_MS);

    private static final List<MenuEntry> PROFILE_ITEMS = List.of(
            MenuEntry.submenu(id("profile/skill"), tr("saomenu.menu.skill"), itemIcon("item_status"), SKILLS),
            MenuEntry.submenu(id("profile/equip"), tr("saomenu.menu.equip"), itemIcon("item_weapon"), SaoPanels::equipItems),
            MenuEntry.submenu(id("profile/items"), tr("saomenu.menu.items"), itemIcon("item_bag"), INVENTORY),
            MenuEntry.action(id("profile/map"), tr("saomenu.menu.map"), itemIcon("item_map"), ctx -> {
                ctx.host().playPanel();
                ctx.host().toggleMap();
            }),
            MenuEntry.action(id("profile/inventory"), Component.translatable("container.inventory"),
                    itemIcon("item_bag"), ctx -> ctx.openScreen(new InventoryScreen(ctx.player()))),
            MenuEntry.action(id("profile/attributes"), tr("saomenu.menu.attributes"),
                    itemIcon("item_status"), ctx -> ctx.openScreen(new SAOStatsScreen(ctx.screen(), ctx.player()))));

    private static final List<MenuEntry> EQUIP_ITEMS = List.of(
            MenuEntry.equipColumn(id("equip/weapon"), tr("saomenu.menu.weapon"), itemIcon("item_weapon"), MenuEntry.EquipKind.WEAPON),
            MenuEntry.equipColumn(id("equip/armor"), tr("saomenu.menu.armor"), itemIcon("item_armor"), MenuEntry.EquipKind.ARMOR),
            MenuEntry.equipColumn(id("equip/trinket"), tr("saomenu.menu.trinket"), itemIcon("item_ring"), MenuEntry.EquipKind.TRINKET));

    private static final List<MenuEntry> PARTY_ITEMS = List.of(
            MenuEntry.submenu(id("party/invite"), tr("saomenu.menu.invite"), itemIcon("item_status"), INVITES),
            MenuEntry.action(id("party/leave"), tr("saomenu.menu.leave_team"), itemIcon("item_bag"), ctx -> {
                new LeaveC2S().sendToServer();
                ctx.host().playClick();
            }));

    private static final List<MenuEntry> FRIENDS_ITEMS = List.of(
            MenuEntry.action(id("friends/advancements"), tr("saomenu.menu.advancements"), itemIcon("item_status"), ctx -> {
                ctx.host().playClick();
                ctx.openScreen(new AdvancementsScreen(ctx.player().connection.getAdvancements()));
            }),
            MenuEntry.action(id("friends/statistics"), Component.translatable("gui.stats"),
                    itemIcon("item_status"), ctx -> ctx.openScreen(new StatsScreen(ctx.screen(), ctx.player().getStats()))),
            MenuEntry.action(id("friends/refresh"), tr("saomenu.menu.refresh"), itemIcon("item_bag"), SaoPanels::switchToParty));

    private static final List<MenuEntry> SETTINGS_ITEMS = List.of(
            MenuEntry.action(id("settings/config"), tr("saomenu.menu.config"), itemIcon("item_config"), ctx -> {
                ctx.host().playClick();
                ctx.openScreen(new SAOSettingsScreen(ctx.screen()));
            }),
            MenuEntry.action(id("settings/options"), tr("saomenu.menu.options"), itemIcon("item_status"), ctx -> {
                ctx.host().playClick();
                ctx.openScreen(new OptionsScreen(ctx.screen(), ctx.minecraft().options));
            }),
            MenuEntry.action(id("settings/close"), tr("saomenu.menu.close"), itemIcon("item_logout"), SaoPanels::requestClose));

    private SaoPanels() {
    }

    /** Register the four builtin panels. Duplicate ids fail in the shared registry. */
    public static void registerBuiltins(SaoUiRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        registry.menu(SaoPanel.of(PROFILE, 100, tr("saomenu.panel.profile"), symbol("info"),
                SaoPanels::profileItems, MenuCards.playerCard()));
        registry.menu(SaoPanel.of(PARTY, 200, tr("saomenu.party"), symbol("party"),
                SaoPanels::partyItems, MenuCards.teamCard()));
        registry.menu(SaoPanel.of(FRIENDS, 300, tr("saomenu.friends"), symbol("msg"),
                SaoPanels::friendsItems, MenuCards.friendsCard()));
        registry.menu(SaoPanel.of(SETTINGS, 400, tr("saomenu.panel.settings"), symbol("setting"),
                SaoPanels::settingsItems));
    }

    public static void resetSession() {
        INVENTORY.invalidate();
        INVITES.invalidate();
        SKILLS.invalidate();
    }

    private static List<MenuEntry> profileItems() {
        return PROFILE_ITEMS;
    }

    private static List<MenuEntry> skillItems() {
        List<MenuEntry> out = new ArrayList<>();
        for (SaoSkill skill : SaoSkillRegistry.skills()) {
            out.add(MenuEntry.action(id("skill/" + safePath(skill.id())), tr(skill.nameKey()),
                    itemIcon(skill.icon()), ctx -> {
                        ctx.host().playClick();
                        SaoSkills.activate(ctx.player(), skill);
                    }));
        }
        return out;
    }

    private static List<MenuEntry> equipItems() {
        return EQUIP_ITEMS;
    }

    private static List<MenuEntry> inventoryItems() {
        List<MenuEntry> list = new ArrayList<>();
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null) {
            for (int i = 0; i < 36; i++) {
                ItemStack s = p.getInventory().getItem(i);
                if (!s.isEmpty()) {
                    list.add(MenuEntry.item(id("inv/" + i), s, i));
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
            list.add(MenuEntry.of(id("inv/empty"), tr("saomenu.inv.empty"), itemIcon("item_bag")));
        }
        return list;
    }

    public static int pinOrderOf(ItemStack st) {
        if (st == null || st.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        return SAOConfig.pinOrder(itemId(st));
    }

    public static int orderIndexOf(ItemStack st) {
        if (st == null || st.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        return SAOConfig.orderIndex(itemId(st));
    }

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
                list.add(MenuEntry.action(id("invite/" + safePath(name)), Component.literal(name),
                        itemIcon("item_status"), SaoPanels::invite));
            }
        }
        if (list.isEmpty()) {
            list.add(MenuEntry.of(id("invite/empty"), tr("saomenu.panel.no_players"), itemIcon("item_status")));
        }
        return list;
    }

    private static void invite(MenuContext ctx) {
        String target = ctx.entry() == null ? null : ctx.entry().label().getString();
        if (target != null && !target.isEmpty()) {
            new InviteC2S(target).sendToServer();
            SaoUi.notify(Component.translatable("saomenu.party.notify.sent.title"),
                    Component.literal(SaoText.tr("saomenu.party.notify.sent.msg", target)));
        }
        ctx.host().playClick();
    }

    private static List<MenuEntry> friendsItems() {
        return FRIENDS_ITEMS;
    }

    private static void switchToParty(MenuContext ctx) {
        ctx.host().selectPanel(PARTY);
        ctx.host().playPanel();
    }

    private static List<MenuEntry> settingsItems() {
        return SETTINGS_ITEMS;
    }

    private static void requestClose(MenuContext ctx) {
        ctx.host().openCloseConfirm();
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(SAOMenu.MOD_ID, path);
    }

    private static ResourceLocation itemIcon(String name) {
        return new ResourceLocation(SAOMenu.MOD_ID, "textures/gui/" + name + ".png");
    }

    private static MenuIcon symbol(String name) {
        return new MenuIcon(
                new ResourceLocation(SAOMenu.MOD_ID, "textures/gui/symbol_" + name + "_normal.png"),
                new ResourceLocation(SAOMenu.MOD_ID, "textures/gui/symbol_" + name + "_hover.png"));
    }

    private static Component tr(String key) {
        return Component.translatable(key);
    }

    static String safePath(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "x";
        }
        String s = raw.toLowerCase(Locale.ROOT);
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '-' || c == '/' || c == '.') {
                b.append(c);
            } else {
                b.append('_');
            }
        }
        return b.isEmpty() ? "x" : b.toString();
    }

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
