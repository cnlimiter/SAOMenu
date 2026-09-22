package com.sao.saomenu.dev.preview;

import com.sao.saomenu.SAOMenu;
import com.sao.saomenu.api.SaoUi;
import com.sao.saomenu.api.SaoUiRegistry;
import com.sao.saomenu.api.hud.HudElement;
import com.sao.saomenu.api.hud.HudPass;
import com.sao.saomenu.api.world.WorldOverlay;
import com.sao.saomenu.api.widget.SaoScrollPane;
import com.sao.saomenu.client.hud.SAONotification;
import com.sao.saomenu.client.menu.MenuLayout;
import com.sao.saomenu.client.menu.SAOMenuScreen;
import com.sao.saomenu.client.screen.SAOStatsScreen;
import com.sao.saomenu.client.screen.settings.SAOSettingsScreen;
import com.sao.saomenu.client.screen.vanilla.VanillaUiPolicy;
import com.sao.saomenu.config.SAOConfig;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.CompletableFuture;

/** Native screen/input/protocol scenario; only compiled into the isolated preview run. */
public final class FrameworkRecoveryPreview {
    private static int ticks;
    private static int stage;
    private static int age;
    private static int hudFrames;
    private static int worldFrames;
    private static int pausedHudFrames;
    private static int pausedWorldFrames;
    private static boolean done;
    private static boolean configuredInitially;
    private static Screen title;
    private static Screen pause;
    private static InventoryScreen inventory;
    private static CompletableFuture<Void> serverWork;

    private FrameworkRecoveryPreview() {
    }

    public static boolean requested() {
        return SAOMenuPreview.requested() && Boolean.getBoolean("saomenu.preview.recovery");
    }

    public static void register() {
        ClientTickEvent.CLIENT_POST.register(FrameworkRecoveryPreview::tick);
    }

    public static void registerUi(SaoUiRegistry registry) {
        if (!requested()) return;
        ResourceLocation marker = new ResourceLocation("saomenu_preview", "recovery_probe");
        registry.hud(HudElement.builder(marker, 2000, Component.literal("Recovery probe"), context -> {
            hudFrames++;
            context.graphics().fill(3, 72, 7, 76, 0xFFDC35D5);
        }).visible(context -> context.player() != null).passes(HudPass.WORLD).build());
        registry.world(WorldOverlay.of(marker, 2000, (pose, camera, projection, partialTick) -> worldFrames++));
    }

    private static void tick(Minecraft client) {
        if (done) return;
        if (++ticks > 2400) throw new IllegalStateException("Native recovery preview timed out at " + stage + ":" + age);
        if (stage == 0) {
            if (client.getOverlay() != null || SAOConfig.path() == null) return;
            client.options.pauseOnLostFocus = false;
            client.options.guiScale().set(2);
            client.resizeDisplay();
            client.setScreen(new TitleScreen());
            title = client.screen;
            configuredInitially = SAOConfig.frameworkEnabled();
            stage = 1;
            age = 0;
            return;
        }
        if (stage == 1) {
            title(client, ++age);
            return;
        }
        if (stage == 2) {
            if (client.player == null || client.level == null || client.screen != null) return;
            stage = 3;
            age = 0;
            var server = client.getSingleplayerServer();
            var id = client.player.getUUID();
            serverWork = CompletableFuture.runAsync(() -> {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                check(player != null, "Missing native server player");
                player.setGameMode(GameType.SURVIVAL);
                player.getInventory().setItem(0, new ItemStack(Items.IRON_SWORD));
                player.getInventory().setItem(10, new ItemStack(Items.APPLE, 8));
                player.getInventory().setItem(11, ItemStack.EMPTY);
                player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                player.giveExperienceLevels(41);
                player.awardStat(Stats.JUMP, 3);
                var root = server.getAdvancements().getAdvancement(new ResourceLocation("minecraft", "story/root"));
                check(root != null, "Missing vanilla advancement root");
                for (String criterion : root.getCriteria().keySet()) player.getAdvancements().award(root, criterion);
                player.inventoryMenu.broadcastChanges();
            }, server);
            return;
        }
        if ((age == 9 || (!SaoUi.safeMode() && (age == 53 || age == 117))) && !serverWork.isDone()) return;
        if (age == 117) {
            serverWork.join();
            if (client.player.getAttributeValue(Attributes.MAX_HEALTH) != 40) return;
        }
        if (SaoUi.safeMode()) safeWorld(client, ++age);
        else world(client, ++age);
    }

    private static void title(Minecraft client, int at) {
        switch (at) {
            case 3 -> {
                check(title == client.screen, "Title screen identity changed");
                Button control = button(client.screen, SaoUi.safeMode() ? "saomenu.recovery.safe" : "saomenu.recovery.disable");
                check(control.active != SaoUi.safeMode(), "Recovery control ignored forced safe mode");
                shot(client, SaoUi.safeMode() ? "recovery_title_safe.png" : "recovery_title_sao.png");
            }
            case 5 -> {
                if (SaoUi.safeMode()) {
                    boolean rejected = false;
                    try { SaoUi.setEnabled(true); } catch (IllegalStateException expected) { rejected = true; }
                    check(rejected, "Safe mode accepted an API enable request");
                }
                key(client, GLFW.GLFW_KEY_F8, GLFW.GLFW_PRESS);
                check(!SaoUi.enabled(), "F8 did not restore native UI");
                key(client, GLFW.GLFW_KEY_F8, GLFW.GLFW_REPEAT);
                check(!SaoUi.enabled(), "Held recovery key toggled twice");
                key(client, GLFW.GLFW_KEY_F8, GLFW.GLFW_RELEASE);
                check(client.screen == title, "Recovery replaced a native screen instance");
            }
            case 8 -> {
                shot(client, "recovery_title_vanilla.png");
                if (!SaoUi.safeMode()) {
                    click(client.screen, "saomenu.recovery.enable");
                    check(SaoUi.enabled(), "Visible title recovery control did not re-enable UI");
                } else {
                    check(SAOConfig.frameworkEnabled() == configuredInitially, "Forced safe mode rewrote user preference");
                }
            }
            case 12 -> {
                client.createWorldOpenFlows().createFreshLevel("saomenu-recovery-" + System.currentTimeMillis(),
                        new LevelSettings("SAOMenu native recovery", GameType.SURVIVAL, false, Difficulty.PEACEFUL,
                                true, new GameRules(), WorldDataConfiguration.DEFAULT),
                        new WorldOptions(20260921L, false, false),
                        access -> access.registryOrThrow(Registries.WORLD_PRESET)
                                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
                stage = 2;
            }
            default -> { }
        }
    }

    private static void world(Minecraft client, int at) {
        switch (at) {
            case 10 -> {
                serverWork.join();
                check(hudFrames > 0 && worldFrames > 0, "Public render callbacks were not exercised");
                shot(client, "recovery_hud_sao.png");
            }
            case 12 -> {
                press(client, GLFW.GLFW_KEY_F8);
                check(!SaoUi.enabled(), "In-world recovery input was delayed or ignored");
                pausedHudFrames = hudFrames;
                pausedWorldFrames = worldFrames;
                SaoUi.notify(Component.literal("Not displayed"), Component.empty());
                check(SAONotification.size() == 0, "Disabled UI queued a late custom notification");
            }
            case 16 -> {
                check(hudFrames == pausedHudFrames && worldFrames == pausedWorldFrames,
                        "An addon render callback bypassed the master switch");
                shot(client, "recovery_hud_vanilla.png");
                press(client, GLFW.GLFW_KEY_F8);
                check(SaoUi.enabled(), "Rapid press/release left the recovery key latched");
                press(client, GLFW.GLFW_KEY_O);
            }
            case 20 -> {
                check(client.screen instanceof SAOMenuScreen, "O did not reopen the production menu");
                shot(client, "recovery_menu.png");
                press(client, GLFW.GLFW_KEY_F8);
                check(client.screen == null && !SaoUi.enabled(), "Recovery did not close the owned menu");
                press(client, GLFW.GLFW_KEY_O);
            }
            case 22 -> {
                check(client.screen == null, "Disabled menu intercepted O");
                press(client, GLFW.GLFW_KEY_F8);
                client.setScreen(new PauseScreen(true));
                pause = client.screen;
            }
            case 24 -> {
                shot(client, "recovery_pause_controls.png");
                click(client.screen, "saomenu.settings.title");
                check(client.screen instanceof SAOSettingsScreen, "Pause settings entry did not mount settings");
                press(client, GLFW.GLFW_KEY_F8);
                check(client.screen == pause && !SaoUi.enabled(), "Settings recovery lost its native parent");
            }
            case 27 -> {
                click(client.screen, "saomenu.recovery.enable");
                press(client, GLFW.GLFW_KEY_O);
                check(client.screen instanceof SAOMenuScreen, "Pause O did not enter the production menu");
                ((SAOMenuScreen) client.screen).selectMain(0);
            }
            case 38 -> menuEntry(client, 0, 4);
            case 42 -> {
                check(client.screen != null && client.screen.getClass() == InventoryScreen.class,
                        "Inventory entry did not use the native survival screen");
                inventory = (InventoryScreen) client.screen;
                check(inventory.getMenu() == client.player.inventoryMenu, "Native inventory controller was replaced");
                shot(client, "recovery_inventory_sao.png");
                slot(client, 10);
                check(inventory.getMenu().getCarried().getCount() == 8, "Native pickup did not carry eight apples");
                press(client, GLFW.GLFW_KEY_F8);
                check(client.screen == inventory && inventory.getMenu().getCarried().getCount() == 8,
                        "Recovery replaced the container or lost its cursor stack");
            }
            case 46 -> {
                shot(client, "recovery_inventory_vanilla.png");
                slot(client, 11);
            }
            case 50 -> {
                var server = client.getSingleplayerServer();
                var id = client.player.getUUID();
                serverWork = CompletableFuture.runAsync(() -> {
                    ServerPlayer player = server.getPlayerList().getPlayer(id);
                    check(player.getInventory().getItem(10).isEmpty(), "Server retained the picked-up source stack");
                    check(player.getInventory().getItem(11).is(Items.APPLE)
                            && player.getInventory().getItem(11).getCount() == 8, "Server rejected native inventory move");
                    check(player.inventoryMenu.getCarried().isEmpty(), "Server retained a carried stack");
                }, server);
            }
            case 54 -> {
                serverWork.join();
                press(client, GLFW.GLFW_KEY_F8);
                press(client, GLFW.GLFW_KEY_ESCAPE);
                SaoUi.openMenu();
                ((SAOMenuScreen) client.screen).selectMain(2);
            }
            case 66 -> menuEntry(client, 2, 1);
            case 80 -> {
                check(client.screen != null && client.screen.getClass() == StatsScreen.class,
                        "Statistics entry is not the native statistics screen");
                check(client.player.getStats().getValue(Stats.CUSTOM, Stats.JUMP) >= 3, "Native server statistics were not received");
                shot(client, "recovery_native_statistics.png");
                click(client.screen, "gui.done");
                check(client.screen instanceof SAOMenuScreen, "Native Done action did not return to its menu parent");
                ((SAOMenuScreen) client.screen).selectMain(2);
            }
            case 92 -> menuEntry(client, 2, 0);
            case 98 -> {
                check(client.screen != null && client.screen.getClass() == AdvancementsScreen.class,
                        "Advancements entry is not the native graph");
                shot(client, "recovery_native_advancements.png");
                press(client, GLFW.GLFW_KEY_ESCAPE);
                SaoUi.openMenu();
                ((SAOMenuScreen) client.screen).selectMain(0);
            }
            case 110 -> {
                // Keep native tutorial/recipe toasts from obscuring this fixture's attribute values.
                client.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                client.getToasts().clear();
                menuEntry(client, 0, 5);
            }
            case 114 -> {
                check(client.screen instanceof SAOStatsScreen, "Character-attribute route did not mount its owned screen");
                shot(client, "recovery_character_attributes.png");
                Button done = button(client.screen, "gui.done");
                check(done.getY() >= 0 && done.getY() + done.getHeight() <= client.screen.height,
                        "Character attributes pushed Done outside the viewport");
                SaoScrollPane pane = (SaoScrollPane) client.screen.children().stream()
                        .filter(SaoScrollPane.class::isInstance).findFirst().orElseThrow();
                check(pane.maxScroll() > 0, "Long native attribute list was not bounded");
                client.screen.mouseScrolled(pane.getX() + 2, pane.getY() + 2, -100);
                check(pane.scrollAmount() == pane.maxScroll(), "Last attribute is not reachable by scrolling");
                var server = client.getSingleplayerServer();
                var id = client.player.getUUID();
                serverWork = CompletableFuture.runAsync(() -> server.getPlayerList().getPlayer(id)
                        .getAttribute(Attributes.MAX_HEALTH).setBaseValue(40), server);
            }
            case 118 -> {
                shot(client, "recovery_character_attributes_scrolled.png");
                SaoScrollPane pane = (SaoScrollPane) client.screen.children().stream()
                        .filter(SaoScrollPane.class::isInstance).findFirst().orElseThrow();
                client.screen.mouseScrolled(pane.getX() + 2, pane.getY() + 2, 100);
            }
            case 122 -> {
                check(((SAOStatsScreen) client.screen).debugRowLabels().contains(
                        Component.translatable(Attributes.MAX_HEALTH.getDescriptionId()).getString() + "=40"),
                        "Mounted attribute rows did not follow the server update");
                shot(client, "recovery_character_attributes_live.png");
                click(client.screen, "gui.done");
                check(client.screen instanceof SAOMenuScreen, "Character attributes lost their menu parent");
                client.setScreen(new PauseScreen(true) { });
                check(VanillaUiPolicy.mode(client.screen) == VanillaUiPolicy.Mode.KEEP,
                        "An unknown native-screen subclass inherited ownership");
            }
            case 126 -> {
                shot(client, "recovery_unknown_pause.png");
                finish(client);
            }
            default -> { }
        }
    }

    private static void safeWorld(Minecraft client, int at) {
        switch (at) {
            case 10 -> {
                serverWork.join();
                check(!SaoUi.enabled() && hudFrames == 0 && worldFrames == 0, "Safe mode dispatched custom render callbacks");
                shot(client, "recovery_safe_hud.png");
                press(client, GLFW.GLFW_KEY_O);
            }
            case 14 -> {
                check(client.screen == null, "Safe mode opened the SAO menu");
                press(client, GLFW.GLFW_KEY_E);
            }
            case 18 -> {
                check(client.screen != null && client.screen.getClass() == InventoryScreen.class,
                        "Native inventory key did not work in safe mode");
                Screen before = client.screen;
                press(client, GLFW.GLFW_KEY_F8);
                check(!SaoUi.enabled() && client.screen == before, "Safe-mode recovery altered the native inventory");
                shot(client, "recovery_safe_inventory.png");
            }
            case 22 -> finish(client);
            default -> { }
        }
    }

    private static void menuEntry(Minecraft client, int panelIndex, int rowIndex) {
        check(client.screen instanceof SAOMenuScreen, "Native route requires the mounted production menu");
        SAOMenuScreen menu = (SAOMenuScreen) client.screen;
        int count = SaoUi.panels().get(panelIndex).items().get().size();
        var bounds = MenuLayout.menuItemRect(menu.width, menu.height, count,
                MenuLayout.buttonCenterY(menu.height, panelIndex), rowIndex);
        float[] at = menu.screenPointOf(bounds.centerX(), bounds.centerY());
        check(menu.mouseClicked(at[0], at[1], 0), "Production menu row did not handle the native pointer");
        menu.mouseReleased(at[0], at[1], 0);
    }

    private static void slot(Minecraft client, int inventoryIndex) {
        Slot slot = inventory.getMenu().slots.stream().filter(candidate -> candidate.container == client.player.getInventory()
                && candidate.getContainerSlot() == inventoryIndex).findFirst().orElseThrow();
        double x = inventory.getGuiLeft() + slot.x + 8;
        double y = inventory.getGuiTop() + slot.y + 8;
        inventory.mouseClicked(x, y, 0);
        inventory.mouseReleased(x, y, 0);
    }

    private static Button button(Screen screen, String key) {
        String text = Component.translatable(key).getString();
        return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(value -> value.getMessage().getString().equals(text)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing native control: " + text));
    }

    private static void click(Screen screen, String key) {
        Button button = button(screen, key);
        double x = button.getX() + button.getWidth() / 2.0;
        double y = button.getY() + button.getHeight() / 2.0;
        check(screen.mouseClicked(x, y, 0), "Native control rejected pointer: " + key);
        screen.mouseReleased(x, y, 0);
    }

    private static void key(Minecraft client, int code, int action) {
        client.keyboardHandler.keyPress(client.getWindow().getWindow(), code, 0, action, 0);
    }

    private static void press(Minecraft client, int code) {
        key(client, code, GLFW.GLFW_PRESS);
        key(client, code, GLFW.GLFW_RELEASE);
    }

    private static void shot(Minecraft client, String name) {
        SAOMenuPreview.grab(client, System.getProperty("saomenu.preview"), name);
    }

    private static void finish(Minecraft client) {
        done = true;
        SAOMenu.LOGGER.info("[SAOMenu] native recovery checks passed: safeMode={}, HUD frames={}, world frames={}, native controllers retained",
                SaoUi.safeMode(), hudFrames, worldFrames);
        if (!Boolean.getBoolean("saomenu.preview.keepOpen")) client.stop();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
