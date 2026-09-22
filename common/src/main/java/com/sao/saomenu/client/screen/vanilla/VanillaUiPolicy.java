package com.sao.saomenu.client.screen.vanilla;

import com.sao.saomenu.api.SaoUi;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.controls.ControlsScreen;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.telemetry.TelemetryInfoScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;

import java.util.Map;
import java.util.HashMap;

/** Visual ownership only. Native screens, menus, widgets, input and packets remain authoritative. */
public final class VanillaUiPolicy {
    public enum Mode { KEEP, DECORATE, RESKIN }

    private static final Map<Class<? extends Screen>, Mode> SCREENS = screens();

    private VanillaUiPolicy() {
    }

    /** Subclasses are deliberately not inherited: an unregistered mod screen is untouched. */
    public static Mode mode(Screen screen) {
        return screen == null || !SaoUi.enabled()
                ? Mode.KEEP : SCREENS.getOrDefault(screen.getClass(), Mode.KEEP);
    }

    public static boolean active(Screen screen) {
        return mode(screen) != Mode.KEEP;
    }

    private static Map<Class<? extends Screen>, Mode> screens() {
        Map<Class<? extends Screen>, Mode> result = new HashMap<>();
        add(result, Mode.RESKIN,
                InventoryScreen.class, ContainerScreen.class, ShulkerBoxScreen.class,
                HopperScreen.class, DispenserScreen.class, CraftingScreen.class,
                FurnaceScreen.class, BlastFurnaceScreen.class, SmokerScreen.class,
                PauseScreen.class, OptionsScreen.class, AccessibilityOptionsScreen.class,
                ChatOptionsScreen.class, ControlsScreen.class, MouseSettingsScreen.class,
                KeyBindsScreen.class, SkinCustomizationScreen.class, SoundOptionsScreen.class,
                VideoSettingsScreen.class, LanguageSelectScreen.class, OnlineOptionsScreen.class,
                PackSelectionScreen.class, TelemetryInfoScreen.class, CreditsAndAttributionScreen.class,
                SelectWorldScreen.class, CreateWorldScreen.class, EditWorldScreen.class,
                JoinMultiplayerScreen.class, DirectJoinServerScreen.class, EditServerScreen.class,
                ConnectScreen.class, DisconnectedScreen.class, DeathScreen.class,
                ProgressScreen.class, LevelLoadingScreen.class, ReceivingLevelScreen.class,
                StatsScreen.class, AdvancementsScreen.class);
        add(result, Mode.DECORATE,
                CreativeModeInventoryScreen.class, BrewingStandScreen.class, AnvilScreen.class,
                SmithingScreen.class, EnchantmentScreen.class, GrindstoneScreen.class,
                StonecutterScreen.class, LoomScreen.class, CartographyTableScreen.class,
                BeaconScreen.class, MerchantScreen.class,
                TitleScreen.class, ChatScreen.class, InBedChatScreen.class,
                BookViewScreen.class, LecternScreen.class, BookEditScreen.class,
                SignEditScreen.class, HangingSignEditScreen.class);
        return Map.copyOf(result);
    }

    @SafeVarargs
    private static void add(Map<Class<? extends Screen>, Mode> target, Mode mode,
                            Class<? extends Screen>... screens) {
        for (Class<? extends Screen> screen : screens) {
            if (target.putIfAbsent(screen, mode) != null) {
                throw new IllegalStateException("Duplicate vanilla UI policy: " + screen.getName());
            }
        }
    }
}
