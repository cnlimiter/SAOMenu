package com.sao.saomenu.dev.preview;

import com.sao.saomenu.SAOMenu;
import com.sao.saomenu.api.SaoUi;
import com.sao.saomenu.client.input.SAOKeybinds;
import com.sao.saomenu.client.screen.vanilla.VanillaUiPolicy;
import com.sao.saomenu.config.SAOConfig;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.ChatOptionsScreen;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.EditServerScreen;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.MouseSettingsScreen;
import net.minecraft.client.gui.screens.OnlineOptionsScreen;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.SkinCustomizationScreen;
import net.minecraft.client.gui.screens.SoundOptionsScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.controls.ControlsScreen;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.HangingSignEditScreen;
import net.minecraft.client.gui.screens.inventory.LecternScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.telemetry.TelemetryInfoScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.client.gui.screens.LanguageSelectScreen;
import net.minecraft.client.gui.screens.VideoSettingsScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Isolated native text/information and frontend scenario. KeyboardHandler and Screen
 * input here are in-process proof, not OS IME proof.
 */
public final class FrontendNativePreview {
    private static final boolean WORLD_ONLY = "world".equals(System.getProperty("saomenu.preview.frontend"));
    private static final String LOOPBACK = "127.0.0.1:1";
    private static final String CHAT_LINE = "sao-frontend-chat";
    private static final String BOOK_INSERT = "sao-book";
    private static final String SIGN_FRONT = "sao-front";
    private static final String SIGN_BACK = "sao-back";
    private static final String SIGN_HANG = "sao-hang";

    private static int ticks;
    private static int phase;
    private static int age;
    private static int settle;
    private static String queuedShot;
    private static boolean done;
    private static boolean awaitingSafety;
    private static boolean sawDisconnected;
    private static String worldId;
    private static Screen title;
    private static Screen identity;
    private static CompletableFuture<Void> serverWork;
    private static CompletableFuture<Void> reload;
    private static CompletableFuture<Void> connectionWork;
    private static volatile boolean connectionAccepted;
    private static volatile boolean connectionCanceled;
    private static String connectionAddress;
    private static int connectStep;
    private static BlockPos signPos;
    private static BlockPos hangPos;
    private static BlockPos lecternPos;
    private static BlockPos bedPos;
    private static boolean sawLoading;
    private static boolean sawReceiving;
    private static boolean sawProgress;
    private static boolean progressContractPassed;
    private static boolean sawOverlay;
    private static boolean sawBedChat;
    private static boolean uiWasEnabled;
    private static int deathTicks;
    private static int loadingHold;
    private static int receivingHold;
    private static int progressHold;
    private static int overlayHold;
    private static int signWait;
    private static String observedSign;
    private static int backgroundEvents;
    private static int backgroundBefore;
    private static final List<String> residuals = new ArrayList<>();

    private FrontendNativePreview() {
    }

    public static boolean requested() {
        return SAOMenuPreview.requested() && (WORLD_ONLY || Boolean.getBoolean("saomenu.preview.frontend"));
    }

    public static void register() {
        ClientTickEvent.CLIENT_POST.register(FrontendNativePreview::tick);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                (net.minecraftforge.client.event.ScreenEvent.BackgroundRendered event) -> backgroundEvents++);
    }

    private static void tick(Minecraft client) {
        if (done) return;
        if (client.screen instanceof net.minecraft.client.gui.screens.multiplayer.SafetyScreen) {
            if (!awaitingSafety) {
                awaitingSafety = true;
                SAOMenu.LOGGER.info("[SAOMenu] native multiplayer safety approval required; preview paused");
            }
            return;
        }
        if (++ticks > 8000) {
            throw new IllegalStateException("Frontend native preview timed out at " + phase + ":" + age
                    + " screen=" + type(client.screen) + " overlay=" + type(client.getOverlay()));
        }
        if (serverWork != null) {
            if (!serverWork.isDone()) return;
            serverWork.join();
            serverWork = null;
        }
        noteTransient(client);
        if (queuedShot != null) {
            if (++settle < 2) return;
            shot(client, queuedShot);
            queuedShot = null;
            settle = 0;
        }
        switch (phase) {
            case 0 -> boot(client);
            case 1 -> title(client);
            case 2 -> options(client);
            case 3 -> worldUi(client);
            case 4 -> serverUi(client);
            case 5 -> createWorld(client);
            case 6 -> enterWorld(client);
            case 7 -> chat(client);
            case 8 -> books(client);
            case 9 -> lectern(client);
            case 10 -> signs(client);
            case 11 -> hanging(client);
            case 12 -> pauseInfo(client);
            case 13 -> reload(client);
            case 14 -> bed(client);
            case 15 -> death(client);
            case 16 -> disconnect(client);
            case 17 -> reopen(client);
            case 18 -> progressSurface(client);
            default -> finish(client);
        }
    }

    private static void boot(Minecraft client) {
        if (client.getOverlay() != null || SAOConfig.path() == null) return;
        client.options.pauseOnLostFocus = false;
        SaoUi.setEnabled(true);
        client.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        client.options.guiScale().set(2);
        client.resizeDisplay();
        worldId = "saomenu-frontend-" + System.currentTimeMillis();
        client.setScreen(new TitleScreen());
        title = client.screen;
        check(title.getClass() == TitleScreen.class, "Boot did not mount TitleScreen");
        check(VanillaUiPolicy.mode(title) != VanillaUiPolicy.Mode.KEEP || !SaoUi.enabled(),
                "Title policy dropped an exact TitleScreen");
        queueShot("frontend_title.png");
        phase = WORLD_ONLY ? 5 : 1;
        age = 0;
    }

    private static void title(Minecraft client) {
        switch (++age) {
            case 1 -> {
                check(client.screen == title && client.screen.getClass() == TitleScreen.class,
                        "Title screen identity changed");
                identity = client.screen;
                uiWasEnabled = SaoUi.enabled();
                press(client, GLFW.GLFW_KEY_F8);
                check(client.screen == identity, "F8 replaced the title screen instance");
                check(!SaoUi.enabled(), "Title F8 did not disable the framework");
                queueShot("frontend_title_off.png");
            }
            case 2 -> {
                press(client, GLFW.GLFW_KEY_F8);
                check(client.screen == identity && SaoUi.enabled() == uiWasEnabled,
                        "Title F8 failed to restore the same screen");
                queueShot("frontend_title_f8.png");
            }
            case 3 -> {
                client.setScreen(new PauseScreen(true) { });
                check(VanillaUiPolicy.mode(client.screen) == VanillaUiPolicy.Mode.KEEP,
                        "An unknown PauseScreen subclass inherited ownership");
                queueShot("frontend_unknown_pause.png");
            }
            case 4 -> {
                client.setScreen(new ConfirmScreen(value -> { }, Component.literal("frontend-keep"),
                        Component.literal("unlisted")));
                check(client.screen.getClass() == ConfirmScreen.class, "ConfirmScreen did not mount");
                check(VanillaUiPolicy.mode(client.screen) == VanillaUiPolicy.Mode.KEEP,
                        "Unlisted ConfirmScreen did not KEEP");
                queueShot("frontend_confirm_keep.png");
            }
            case 5 -> {
                client.setScreen(title);
                click(client.screen, "menu.options");
                phase = 2;
                age = 0;
            }
            default -> { }
        }
    }

    private static void options(Minecraft client) {
        switch (++age) {
            case 1 -> {
                check(client.screen != null && client.screen.getClass() == OptionsScreen.class,
                        "Title options button did not mount OptionsScreen");
                identity = client.screen;
                client.options.guiScale().set(1);
                client.resizeDisplay();
                check(client.getWindow().getGuiScale() == 1, "Requested GUI scale 1 did not take effect");
                backgroundBefore = backgroundEvents;
                queueShot("frontend_options_scale.png");
            }
            case 2 -> {
                check(backgroundEvents > backgroundBefore, "Reskin suppressed Forge background-render events");
                client.options.guiScale().set(2);
                client.resizeDisplay();
                check(client.getWindow().getGuiScale() == 2, "Requested GUI scale 2 did not take effect");
                check(client.screen == identity, "Resize replaced OptionsScreen");
                press(client, GLFW.GLFW_KEY_F8);
                check(client.screen == identity, "F8 replaced OptionsScreen");
                press(client, GLFW.GLFW_KEY_F8);
                click(client.screen, "options.controls");
            }
            case 3 -> {
                check(client.screen != null && client.screen.getClass() == ControlsScreen.class,
                        "Controls subpage did not mount");
                queueShot("frontend_controls.png");
            }
            case 4 -> clickFirst(client.screen, "controls.keybinds", "options.keybinds");
            case 5 -> {
                check(client.screen != null && client.screen.getClass() == KeyBindsScreen.class,
                        "Key binds subpage did not mount");
                KeyBindsScreen binds = (KeyBindsScreen) client.screen;
                identity = binds;
                boolean enabled = SaoUi.enabled();
                binds.selectedKey = SAOKeybinds.TOGGLE_UI;
                press(client, GLFW.GLFW_KEY_F8);
                check(SaoUi.enabled() == enabled, "Key binding capture triggered F8 recovery");
                check(client.screen == identity, "Keybind capture replaced KeyBindsScreen");
                check(SAOKeybinds.TOGGLE_UI.matches(GLFW.GLFW_KEY_F8, 0),
                        "Capture reassigned the recovery key away from F8");
                queueShot("frontend_keybinds.png");
            }
            case 6 -> press(client, GLFW.GLFW_KEY_ESCAPE);
            case 7 -> {
                if (client.screen instanceof KeyBindsScreen) press(client, GLFW.GLFW_KEY_ESCAPE);
            }
            case 8 -> {
                Screen parent = client.screen instanceof ControlsScreen ? client.screen
                        : new OptionsScreen(title, client.options);
                client.setScreen(new VideoSettingsScreen(parent, client.options));
            }
            case 9 -> {
                check(client.screen.getClass() == VideoSettingsScreen.class, "VideoSettingsScreen missing");
                client.screen.mouseScrolled(client.screen.width / 2.0, client.screen.height / 2.0, -2);
                queueShot("frontend_video.png");
            }
            case 10 -> {
                client.setScreen(new LanguageSelectScreen(new OptionsScreen(title, client.options),
                        client.options, client.getLanguageManager()));
            }
            case 11 -> {
                check(client.screen.getClass() == LanguageSelectScreen.class, "LanguageSelectScreen missing");
                check(child(client.screen, AbstractSelectionList.class).children().size() > 0,
                        "Language list had no native entries");
                queueShot("frontend_language.png");
            }
            case 12 -> {
                OptionsScreen parent = new OptionsScreen(title, client.options);
                client.setScreen(new SoundOptionsScreen(parent, client.options));
            }
            case 13 -> {
                check(client.screen.getClass() == SoundOptionsScreen.class, "SoundOptionsScreen missing");
                queueShot("frontend_sound.png");
            }
            case 14 -> client.setScreen(new MouseSettingsScreen(new OptionsScreen(title, client.options), client.options));
            case 15 -> {
                check(client.screen.getClass() == MouseSettingsScreen.class, "MouseSettingsScreen missing");
                queueShot("frontend_mouse.png");
            }
            case 16 -> client.setScreen(new SkinCustomizationScreen(new OptionsScreen(title, client.options), client.options));
            case 17 -> {
                check(client.screen.getClass() == SkinCustomizationScreen.class, "SkinCustomizationScreen missing");
                clickCycle(client.screen);
                queueShot("frontend_skin.png");
            }
            case 18 -> client.setScreen(new ChatOptionsScreen(new OptionsScreen(title, client.options), client.options));
            case 19 -> {
                check(client.screen.getClass() == ChatOptionsScreen.class, "ChatOptionsScreen missing");
                queueShot("frontend_chat_options.png");
            }
            case 20 -> client.setScreen(new AccessibilityOptionsScreen(new OptionsScreen(title, client.options), client.options));
            case 21 -> {
                check(client.screen.getClass() == AccessibilityOptionsScreen.class, "AccessibilityOptionsScreen missing");
                queueShot("frontend_accessibility.png");
            }
            case 22 -> client.setScreen(OnlineOptionsScreen.createOnlineOptionsScreen(client,
                    new OptionsScreen(title, client.options), client.options));
            case 23 -> {
                check(client.screen.getClass() == OnlineOptionsScreen.class, "OnlineOptionsScreen missing");
                queueShot("frontend_online.png");
            }
            case 24 -> {
                client.setScreen(new TelemetryInfoScreen(new OptionsScreen(title, client.options), client.options));
            }
            case 25 -> {
                check(client.screen.getClass() == TelemetryInfoScreen.class, "TelemetryInfoScreen missing");
                queueShot("frontend_telemetry.png");
            }
            case 26 -> {
                clickDoneOrClose(client);
                client.setScreen(new CreditsAndAttributionScreen(title));
            }
            case 27 -> {
                check(client.screen.getClass() == CreditsAndAttributionScreen.class,
                        "CreditsAndAttributionScreen missing");
                queueShot("frontend_credits.png");
            }
            case 28 -> {
                clickDoneOrClose(client);
                OptionsScreen parent = new OptionsScreen(title, client.options);
                client.setScreen(parent);
                click(parent, "options.resourcepack");
            }
            case 29 -> {
                check(client.screen.getClass() == PackSelectionScreen.class, "PackSelectionScreen missing");
                queueShot("frontend_packs.png");
            }
            case 30 -> {
                client.screen.onClose();
                client.setScreen(title);
                phase = 3;
                age = 0;
            }
            default -> { }
        }
    }

    private static void worldUi(Minecraft client) {
        switch (++age) {
            case 1 -> {
                check(client.screen != null && client.screen.getClass() == TitleScreen.class, "World UI needs TitleScreen");
                click(client.screen, "menu.singleplayer");
            }
            case 2, 3, 4, 5, 6, 7, 8, 9, 10 -> {
                // An empty native world list routes directly to creation; the saved list is exercised after disconnect.
                if (client.screen instanceof CreateWorldScreen) age = 25;
                else if (age >= 10) check(client.screen instanceof SelectWorldScreen,
                        "Singleplayer did not open world selection or first-world creation: " + type(client.screen));
            }
            case 11 -> {
                check(client.screen.getClass() == SelectWorldScreen.class, "SelectWorldScreen missing");
                EditBox search = firstEdit(client.screen);
                search.setValue("sao");
                search.moveCursorToEnd();
                queueShot("frontend_select_world.png");
            }
            case 12 -> {
                if (hasButton(client.screen, "selectWorld.create")) click(client.screen, "selectWorld.create");
                else CreateWorldScreen.openFresh(client, client.screen);
            }
            case 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25 -> {
                if (client.screen instanceof CreateWorldScreen) age = 25;
                else if (age >= 25) throw new IllegalStateException("CreateWorldScreen did not open: " + type(client.screen));
            }
            case 26 -> {
                CreateWorldScreen create = (CreateWorldScreen) client.screen;
                create.getUiState().setName("sao-frontend-cancel");
                create.getUiState().setDifficulty(Difficulty.PEACEFUL);
                queueShot("frontend_create_world.png");
            }
            case 27 -> {
                ((CreateWorldScreen) client.screen).onClose();
                phase = 4;
                age = 0;
            }
            default -> { }
        }
    }

    private static void serverUi(Minecraft client) {
        if (connectionWork != null && connectionWork.isCompletedExceptionally()) connectionWork.join();
        switch (++age) {
            case 1 -> {
                check(!client.options.skipMultiplayerWarning,
                        "Native safety prompt is disabled in this profile; use a fresh isolated run directory");
                if (!(client.screen instanceof TitleScreen)) client.setScreen(title);
                title = client.screen;
            }
            case 2 -> click(client.screen, "menu.multiplayer");
            case 3, 4, 5, 6, 7, 8, 9, 10 -> {
                if (client.screen instanceof JoinMultiplayerScreen) age = 10;
                else if (age >= 10) {
                    throw new IllegalStateException("JoinMultiplayerScreen did not open: " + type(client.screen));
                }
            }
            case 11 -> {
                check(client.screen.getClass() == JoinMultiplayerScreen.class, "JoinMultiplayerScreen missing");
                identity = client.screen;
                queueShot("frontend_server_list.png");
            }
            case 12 -> click(client.screen, "selectServer.add");
            case 13 -> {
                check(client.screen.getClass() == EditServerScreen.class, "EditServerScreen missing");
                List<EditBox> boxes = edits(client.screen);
                check(boxes.size() == 2, "EditServerScreen lost its native name/address fields");
                boxes.get(0).setValue("SAO frontend fixture");
                boxes.get(1).setValue(LOOPBACK);
                queueShot("frontend_edit_server.png");
            }
            case 14 -> {
                click(client.screen, "gui.cancel");
                check(client.screen == identity, "Cancel did not return to the original server list");
            }
            case 15 -> {
                connectionAddress = startLoopbackTransport();
                click(client.screen, "selectServer.direct");
            }
            case 16 -> {
                check(client.screen.getClass() == DirectJoinServerScreen.class, "DirectJoinServerScreen missing");
                firstEdit(client.screen).setValue(connectionAddress);
                queueShot("frontend_direct_join.png");
            }
            case 17 -> click(client.screen, "selectServer.select");
            default -> {
                check(age < 400, "Loopback connection flow timed out at step " + connectStep);
                switch (connectStep) {
                    case 0 -> {
                        if (client.screen instanceof ConnectScreen && connectionAccepted) {
                            queueShot("frontend_connect.png");
                            connectStep = 1;
                        }
                    }
                    case 1 -> {
                        check(client.screen instanceof ConnectScreen, "Native connection ended before Cancel");
                        click(client.screen, "gui.cancel");
                        connectStep = 2;
                    }
                    case 2 -> {
                        if (connectionCanceled) {
                            check(client.screen == identity, "Native Cancel did not restore its server list");
                            queueShot("frontend_connect_canceled.png");
                            connectStep = 3;
                        }
                    }
                    case 3 -> {
                        click(client.screen, "selectServer.direct");
                        check(client.screen.getClass() == DirectJoinServerScreen.class, "Second direct join missing");
                        firstEdit(client.screen).setValue(connectionAddress);
                        click(client.screen, "selectServer.select");
                        connectStep = 4;
                    }
                    case 4 -> {
                        if (client.screen instanceof DisconnectedScreen) {
                            sawDisconnected = true;
                            queueShot("frontend_disconnected.png");
                            connectStep = 5;
                        }
                    }
                    case 5 -> {
                        connectionWork.join();
                        click(client.screen, "gui.toMenu");
                        check(client.screen == identity, "Disconnected Done lost the native server-list parent");
                        click(client.screen, "gui.cancel");
                        check(client.screen == title, "Server-list Cancel did not return to the original title");
                        SAOMenu.LOGGER.info("[SAOMenu] frontend TCP fixture: Cancel closed the first stream; "
                                + "peer closure produced native DisconnectedScreen (not Minecraft login proof)");
                        phase = 5;
                        age = 0;
                    }
                    default -> throw new IllegalStateException("Unknown connection step " + connectStep);
                }
            }
        }
    }

    /** A loopback transport fixture, not a simulated Minecraft server or login success. */
    private static String startLoopbackTransport() {
        try {
            var listener = new java.net.ServerSocket(0, 2, java.net.InetAddress.getByName("127.0.0.1"));
            listener.setSoTimeout(15000);
            connectionWork = CompletableFuture.runAsync(() -> {
                try (listener) {
                    try (var first = listener.accept()) {
                        first.setSoTimeout(15000);
                        connectionAccepted = true;
                        first.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
                        connectionCanceled = true;
                    }
                    try (var second = listener.accept()) {
                        second.setSoTimeout(15000);
                        check(second.getInputStream().read() >= 0, "Second connection sent no handshake");
                    }
                } catch (java.io.IOException failure) {
                    throw new IllegalStateException("Owned loopback transport failed", failure);
                }
            });
            return "127.0.0.1:" + listener.getLocalPort();
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("Cannot bind owned loopback transport", failure);
        }
    }

    private static void createWorld(Minecraft client) {
        if (age == 0) {
            if (!(client.screen instanceof TitleScreen)) client.setScreen(title);
            client.createWorldOpenFlows().createFreshLevel(worldId,
                    new LevelSettings(worldId, GameType.SURVIVAL, false, Difficulty.PEACEFUL,
                            true, new GameRules(), WorldDataConfiguration.DEFAULT),
                    new WorldOptions(20260922L, false, false),
                    access -> access.registryOrThrow(Registries.WORLD_PRESET)
                            .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
            age = 1;
            return;
        }
        age++;
        if (client.player != null && client.level != null && client.screen == null && client.getOverlay() == null) {
            phase = 6;
            age = 0;
            return;
        }
        if (age > 1200) {
            throw new IllegalStateException("Owned world did not load: " + type(client.screen)
                    + " overlay=" + type(client.getOverlay()));
        }
    }

    private static void enterWorld(Minecraft client) {
        if (client.player == null || client.level == null || client.screen != null || client.getOverlay() != null) {
            if (++age > 200) throw new IllegalStateException("World was not idle for fixtures");
            return;
        }
        installFixtures(client);
        phase = 7;
        age = 0;
    }

    private static void chat(Minecraft client) {
        switch (++age) {
            case 1 -> client.setScreen(new ChatScreen(""));
            case 2 -> {
                check(client.screen != null && client.screen.getClass() == ChatScreen.class, "ChatScreen missing");
                identity = client.screen;
                ChatScreen chat = (ChatScreen) client.screen;
                type(chat, CHAT_LINE);
                check(firstEdit(chat).getValue().contains(CHAT_LINE), "Chat EditBox did not keep typed text");
                queueShot("frontend_chat.png");
            }
            case 3 -> {
                ChatScreen chat = (ChatScreen) client.screen;
                press(client, GLFW.GLFW_KEY_F8);
                check(client.screen == identity, "F8 replaced ChatScreen");
                press(client, GLFW.GLFW_KEY_F8);
                check(chat.handleChatInput(CHAT_LINE, true), "Native chat send was rejected");
            }
            case 4 -> {
                check(client.gui.getChat().getRecentChat().contains(CHAT_LINE),
                        "Chat history did not record the sent line");
                client.setScreen(new ChatScreen("/hel"));
            }
            case 5 -> {
                check(client.screen.getClass() == ChatScreen.class, "Suggestion chat missing");
                client.screen.keyPressed(GLFW.GLFW_KEY_TAB, 0, 0);
                queueShot("frontend_chat_suggestions.png");
            }
            case 6 -> {
                String value = firstEdit(client.screen).getValue();
                check(value.startsWith("/"), "Command field lost its slash after suggestions");
                client.setScreen(new ChatScreen(""));
            }
            case 7 -> {
                ChatScreen chat = (ChatScreen) client.screen;
                chat.moveInHistory(-1);
                check(firstEdit(chat).getValue().contains(CHAT_LINE), "Chat history recall failed");
                queueShot("frontend_chat_history.png");
            }
            case 8 -> {
                client.screen.onClose();
                phase = 8;
                age = 0;
            }
            default -> { }
        }
    }

    private static void books(Minecraft client) {
        switch (++age) {
            case 1 -> press(client, GLFW.GLFW_KEY_2);
            case 3 -> client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
            case 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 -> {
                if (client.screen instanceof BookViewScreen && !(client.screen instanceof LecternScreen)) age = 15;
                else if (age >= 15) throw new IllegalStateException("Written book did not open BookViewScreen: "
                        + type(client.screen));
            }
            case 16 -> {
                BookViewScreen view = (BookViewScreen) client.screen;
                check(view.getClass() == BookViewScreen.class, "Book view was not the exact native class");
                clickNextPage(view);
                queueShot("frontend_book_view.png");
            }
            case 17 -> {
                client.screen.onClose();
                press(client, GLFW.GLFW_KEY_1);
            }
            case 19 -> client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
            case 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30 -> {
                if (client.screen instanceof BookEditScreen) age = 30;
                else if (age >= 30) throw new IllegalStateException("Writable book did not open BookEditScreen: "
                        + type(client.screen));
            }
            case 31 -> {
                BookEditScreen edit = (BookEditScreen) client.screen;
                type(edit, BOOK_INSERT);
                queueShot("frontend_book_edit.png");
            }
            case 32 -> click(client.screen, "gui.done");
            case 33, 34, 35, 36, 37 -> {
                if (client.screen == null) age = 37;
            }
            case 38 -> {
                var server = client.getSingleplayerServer();
                var id = client.player.getUUID();
                serverWork = CompletableFuture.runAsync(() -> {
                    ServerPlayer player = server.getPlayerList().getPlayer(id);
                    ItemStack book = player.getInventory().getItem(0);
                    check(book.is(Items.WRITABLE_BOOK), "Server lost the writable book");
                    CompoundTag tag = book.getTag();
                    check(tag != null && tag.contains("pages"), "Server did not save book pages");
                    String pages = tag.getList("pages", 8).toString();
                    check(pages.contains(BOOK_INSERT), "Server book text missed native edits: " + pages);
                }, server);
                phase = 9;
                age = 0;
            }
            default -> { }
        }
    }

    private static void lectern(Minecraft client) {
        switch (++age) {
            case 1 -> {
                var server = client.getSingleplayerServer();
                var id = client.player.getUUID();
                BlockPos pos = lecternPos;
                serverWork = CompletableFuture.runAsync(() -> {
                    ServerPlayer player = server.getPlayerList().getPlayer(id);
                    ServerLevel level = player.serverLevel();
                    if (!(level.getBlockEntity(pos) instanceof LecternBlockEntity lectern) || !lectern.hasBook()) {
                        throw new IllegalStateException("Lectern fixture has no server-owned book");
                    }
                    player.openMenu(lectern);
                }, server);
            }
            case 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 -> {
                if (client.screen instanceof LecternScreen) age = 20;
                else if (age >= 20) throw new IllegalStateException("LecternScreen did not open: " + type(client.screen));
            }
            case 21 -> {
                LecternScreen lectern = (LecternScreen) client.screen;
                check(lectern.getClass() == LecternScreen.class, "Lectern was not the exact native class");
                check(lectern.getMenu() == client.player.containerMenu, "Lectern controller was replaced");
                clickNextPage(lectern);
                identity = lectern;
                queueShot("frontend_lectern.png");
            }
            case 22 -> {
                check(((LecternScreen) client.screen).getMenu().getPage() == 1,
                        "Native lectern page click was not synchronized");
                var server = client.getSingleplayerServer();
                serverWork = CompletableFuture.runAsync(() -> check(
                        ((LecternBlockEntity) server.overworld().getBlockEntity(lecternPos)).getPage() == 1,
                        "Server lectern did not advance to the second page"), server);
                press(client, GLFW.GLFW_KEY_ESCAPE);
                phase = 10;
                age = 0;
            }
            default -> { }
        }
    }

    private static void signs(Minecraft client) {
        switch (++age) {
            case 1 -> openSign(client, signPos, true);
            case 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 -> {
                if (client.screen instanceof SignEditScreen) age = 15;
                else if (age >= 15) throw new IllegalStateException("SignEditScreen did not open: " + type(client.screen));
            }
            case 16 -> {
                check(client.screen.getClass() == SignEditScreen.class, "Ordinary sign was not SignEditScreen");
                type(client.screen, SIGN_FRONT);
                queueShot("frontend_sign_front.png");
            }
            case 17 -> client.screen.onClose();
            case 18, 19, 20, 21, 22 -> {
                if (client.screen == null) age = 22;
            }
            case 23 -> {
                if (!verifySign(client, signPos, true, SIGN_FRONT)) age--;
            }
            case 24 -> openSign(client, signPos, false);
            case 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35 -> {
                if (client.screen instanceof SignEditScreen) age = 35;
                else if (age >= 35) throw new IllegalStateException("Sign back editor did not open");
            }
            case 36 -> {
                type(client.screen, SIGN_BACK);
                queueShot("frontend_sign_back.png");
            }
            case 37 -> client.screen.onClose();
            case 38 -> {
                if (client.screen == null && verifySign(client, signPos, false, SIGN_BACK)) {
                    phase = 11;
                    age = 0;
                } else age--;
            }
            default -> { }
        }
    }

    private static void hanging(Minecraft client) {
        switch (++age) {
            case 1 -> openSign(client, hangPos, true);
            case 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 -> {
                if (client.screen instanceof HangingSignEditScreen) age = 20;
                else if (age >= 20) throw new IllegalStateException("HangingSignEditScreen did not open");
            }
            case 21 -> {
                type(client.screen, SIGN_HANG);
                queueShot("frontend_hanging_sign.png");
            }
            case 22 -> client.screen.onClose();
            case 23 -> {
                if (client.screen == null && verifySign(client, hangPos, true, SIGN_HANG)) {
                    phase = 12;
                    age = 0;
                } else age--;
            }
            default -> { }
        }
    }

    private static void pauseInfo(Minecraft client) {
        switch (++age) {
            case 1 -> client.pauseGame(true);
            case 2 -> {
                check(client.screen != null && client.screen.getClass() == PauseScreen.class, "PauseScreen missing");
                check(client.screen.children().stream().noneMatch(
                                net.minecraft.client.gui.components.AbstractButton.class::isInstance),
                        "Focus-loss pause acquired interactive controls");
                queueShot("frontend_pause_hidden.png");
            }
            case 3 -> {
                press(client, GLFW.GLFW_KEY_ESCAPE);
                client.pauseGame(false);
                check(client.screen != null && client.screen.getClass() == PauseScreen.class, "Interactive pause missing");
                identity = client.screen;
            }
            case 4 -> {
                press(client, GLFW.GLFW_KEY_F8);
                check(client.screen == identity && !SaoUi.enabled(), "F8 did not restore the same native pause menu");
                queueShot("frontend_pause_off.png");
            }
            case 5 -> {
                press(client, GLFW.GLFW_KEY_F8);
                check(client.screen == identity && SaoUi.enabled(), "F8 did not re-enable the same pause menu");
                queueShot("frontend_pause.png");
            }
            case 6 -> click(client.screen, "gui.stats");
            case 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 -> {
                if (client.screen instanceof StatsScreen) age = 20;
                else if (age >= 20) throw new IllegalStateException("StatsScreen did not open: " + type(client.screen));
            }
            case 21 -> {
                StatsScreen stats = (StatsScreen) client.screen;
                check(stats.getClass() == StatsScreen.class, "Statistics screen was not native");
                check(client.player.getStats().getValue(Stats.CUSTOM, Stats.JUMP) >= 3,
                        "Native statistics counter missed server awards");
                if (hasButton(stats, "stat.generalButton")) click(stats, "stat.generalButton");
                if (stats.getActiveList() != null) {
                    stats.getActiveList().setScrollAmount(Math.min(20, stats.getActiveList().getMaxScroll()));
                    stats.mouseScrolled(stats.width / 2.0, stats.height / 2.0, -1);
                }
                queueShot("frontend_statistics.png");
            }
            case 22 -> click(client.screen, "gui.done");
            case 23 -> check(client.screen == identity, "Statistics Done did not return to its native pause parent");
            case 24 -> click(client.screen, "gui.advancements");
            case 25, 26, 27, 28, 29, 30 -> {
                if (client.screen instanceof AdvancementsScreen) age = 30;
                else if (age >= 30) {
                    throw new IllegalStateException("AdvancementsScreen did not open: " + type(client.screen));
                }
            }
            case 31 -> {
                AdvancementsScreen graph = (AdvancementsScreen) client.screen;
                check(graph.getClass() == AdvancementsScreen.class, "Advancement graph was not native");
                graph.mouseClicked(graph.width / 2.0, graph.height / 2.0, 0);
                graph.mouseDragged(graph.width / 2.0, graph.height / 2.0, 0, 12, 8);
                graph.mouseReleased(graph.width / 2.0 + 12, graph.height / 2.0 + 8, 0);
                queueShot("frontend_advancements.png");
            }
            case 32 -> {
                press(client, GLFW.GLFW_KEY_ESCAPE);
                if (client.screen instanceof PauseScreen) press(client, GLFW.GLFW_KEY_ESCAPE);
                phase = 13;
                age = 0;
            }
            default -> { }
        }
    }

    private static void reload(Minecraft client) {
        if (age == 0) {
            reload = client.reloadResourcePacks();
            age = 1;
            return;
        }
        age++;
        if (reload != null && !reload.isDone()) {
            if (age > 400) throw new IllegalStateException("Resource reload future never completed");
            return;
        }
        if (reload != null) {
            reload.join();
            reload = null;
        }
        if (client.getOverlay() != null) {
            if (age > 400) throw new IllegalStateException("LoadingOverlay never finished");
            return;
        }
        if (!sawOverlay) residuals.add("LoadingOverlay was not on screen long enough to capture");
        phase = 14;
        age = 0;
    }

    private static void bed(Minecraft client) {
        switch (++age) {
            case 1 -> {
                var server = client.getSingleplayerServer();
                var id = client.player.getUUID();
                serverWork = CompletableFuture.runAsync(() -> {
                    server.getPlayerList().getPlayer(id).serverLevel().setDayTime(18000);
                }, server);
            }
            case 4 -> {
                var server = client.getSingleplayerServer();
                var id = client.player.getUUID();
                BlockPos pos = bedPos.relative(Direction.SOUTH);
                serverWork = CompletableFuture.runAsync(() -> {
                    ServerPlayer player = server.getPlayerList().getPlayer(id);
                    ServerLevel level = player.serverLevel();
                    check(!level.isDay(), "Night-time sky state did not advance on the native server tick");
                    player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() - 1.5);
                    var result = player.startSleepInBed(pos);
                    check(result.left().isEmpty(), "Native bed interaction failed: " + result);
                }, server);
            }
            case 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25 -> {
                if (client.player != null && client.player.isSleeping()) {
                    if (client.screen instanceof InBedChatScreen) age = 25;
                } else if (age >= 25) throw new IllegalStateException("Native sleeping state was not synchronized");
            }
            case 26 -> {
                check(client.screen != null && client.screen.getClass() == InBedChatScreen.class,
                        "Sleeping player did not mount exact InBedChatScreen");
                sawBedChat = true;
                type(client.screen, "bed");
                queueShot("frontend_bed_chat.png");
            }
            case 27 -> click(client.screen, "multiplayer.stopSleeping");
            case 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40 -> {
                if (!client.player.isSleeping() && client.screen != null
                        && client.screen.getClass() == ChatScreen.class) {
                    check("bed".equals(firstEdit(client.screen).getValue()), "Waking lost the native chat draft");
                    queueShot("frontend_bed_draft_preserved.png");
                    age = 40;
                } else if (age >= 40) {
                    throw new IllegalStateException("Native wake-up did not preserve its chat screen: " + type(client.screen));
                }
            }
            case 41 -> {
                press(client, GLFW.GLFW_KEY_ESCAPE);
                check(client.screen == null, "Native chat did not close after waking");
                phase = 15;
                age = 0;
            }
            default -> { }
        }
    }

    private static void death(Minecraft client) {
        switch (++age) {
            case 1 -> {
                deathTicks = 0;
                var server = client.getSingleplayerServer();
                var id = client.player.getUUID();
                serverWork = CompletableFuture.runAsync(() -> {
                    ServerPlayer player = server.getPlayerList().getPlayer(id);
                    player.setGameMode(GameType.SURVIVAL);
                    player.kill();
                }, server);
            }
            default -> {
                if (client.screen instanceof DeathScreen) {
                    deathTicks++;
                    if (deathTicks == 2) queueShot("frontend_death.png");
                    if (deathTicks == 25) {
                        check(client.screen.getClass() == DeathScreen.class, "DeathScreen was not exact");
                        click(client.screen, "deathScreen.respawn");
                    }
                } else if (deathTicks >= 25 && client.player != null && !client.player.isDeadOrDying()
                        && client.screen == null) {
                    queueShot("frontend_respawn.png");
                    phase = 16;
                    age = 0;
                } else if (age > 80) {
                    throw new IllegalStateException("Death/respawn did not complete: " + type(client.screen)
                            + " dead=" + (client.player != null && client.player.isDeadOrDying()));
                }
            }
        }
    }

    private static void disconnect(Minecraft client) {
        switch (++age) {
            case 1 -> {
                if (queuedShot != null) return;
                if (client.screen == null) client.pauseGame(false);
            }
            case 2 -> {
                check(client.screen instanceof PauseScreen, "Disconnect needs PauseScreen, have " + type(client.screen));
                click(client.screen, "menu.returnToMenu");
            }
            default -> {
                if (client.screen instanceof TitleScreen && client.player == null) {
                    title = client.screen;
                    queueShot("frontend_title_after_disconnect.png");
                    phase = 17;
                    age = 0;
                } else if (age > 200) {
                    throw new IllegalStateException("Disconnect did not return to title: " + type(client.screen));
                }
            }
        }
    }

    private static void reopen(Minecraft client) {
        switch (++age) {
            case 1 -> {
                if (queuedShot != null) return;
                check(client.screen instanceof TitleScreen, "Reopen needs TitleScreen");
                click(client.screen, "menu.singleplayer");
            }
            case 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25 -> {
                if (client.screen instanceof SelectWorldScreen) {
                    WorldSelectionList.WorldListEntry entry = worldEntry(client.screen);
                    if (entry != null) age = 25;
                } else if (age >= 25) {
                    throw new IllegalStateException("Owned world did not appear in SelectWorldScreen");
                }
            }
            case 26 -> {
                WorldSelectionList.WorldListEntry entry = worldEntry(client.screen);
                check(entry != null, "Missing owned world list entry " + worldId);
                EditBox search = firstEdit(client.screen);
                search.setValue(worldId);
                entry = worldEntry(client.screen);
                check(entry != null, "Search hid the owned world");
                queueShot("frontend_select_owned_world.png");
            }
            case 27 -> {
                WorldSelectionList.WorldListEntry entry = worldEntry(client.screen);
                entry.editWorld();
            }
            case 28, 29, 30, 31, 32, 33, 34, 35 -> {
                if (client.screen instanceof EditWorldScreen) age = 35;
                else if (age >= 35) throw new IllegalStateException("Owned world edit control did not open EditWorldScreen");
            }
            case 36 -> {
                check(client.screen.getClass() == EditWorldScreen.class, "EditWorldScreen was not exact");
                if (!edits(client.screen).isEmpty()) edits(client.screen).get(0).moveCursorToEnd();
                queueShot("frontend_edit_world.png");
            }
            case 37 -> client.screen.onClose();
            case 38, 39, 40, 41, 42, 43, 44, 45 -> {
                if (client.screen instanceof SelectWorldScreen && worldEntry(client.screen) != null) age = 45;
                else if (age >= 45) throw new IllegalStateException("SelectWorldScreen lost the owned world after edit");
            }
            case 46 -> worldEntry(client.screen).joinWorld();
            default -> {
                if (client.player != null && client.level != null && client.screen == null && client.getOverlay() == null
                        && age > 50) {
                    phase = 18;
                    age = 0;
                } else if (age > 400) {
                    throw new IllegalStateException("Reopening the owned world failed: " + type(client.screen));
                }
            }
        }
    }

    private static void installFixtures(Minecraft client) {
        var server = client.getSingleplayerServer();
        var id = client.player.getUUID();
        serverWork = CompletableFuture.runAsync(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            check(player != null, "Missing native server player");
            player.setGameMode(GameType.SURVIVAL);
            server.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
            server.getGameRules().getRule(GameRules.RULE_DO_IMMEDIATE_RESPAWN).set(false, server);
            ServerLevel level = player.serverLevel();
            BlockPos feet = player.blockPosition();
            signPos = feet.offset(2, 0, 0);
            lecternPos = feet.offset(-2, 0, 0);
            hangPos = feet.offset(3, 2, 0);
            bedPos = feet.offset(0, 0, 3);
            level.setBlock(signPos, Blocks.OAK_SIGN.defaultBlockState(), 3);
            level.setBlock(lecternPos, Blocks.LECTERN.defaultBlockState(), 3);
            LecternBlock.tryPlaceBook(player, level, lecternPos, level.getBlockState(lecternPos),
                    writtenBook("lectern-a", "lectern-b"));
            level.setBlock(hangPos.above(), Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(hangPos, Blocks.OAK_HANGING_SIGN.defaultBlockState(), 3);
            BlockState foot = Blocks.RED_BED.defaultBlockState()
                    .setValue(BedBlock.FACING, Direction.SOUTH)
                    .setValue(BedBlock.PART, BedPart.FOOT);
            level.setBlock(bedPos, foot, 3);
            level.setBlock(bedPos.relative(Direction.SOUTH), foot.setValue(BedBlock.PART, BedPart.HEAD), 3);
            player.getInventory().setItem(0, writableBook());
            player.getInventory().setItem(1, writtenBook("view-a", "view-b"));
            player.getInventory().selected = 0;
            player.awardStat(Stats.JUMP, 3);
            var root = server.getAdvancements().getAdvancement(new ResourceLocation("minecraft", "story/root"));
            check(root != null, "Missing vanilla advancement root");
            for (String criterion : root.getCriteria().keySet()) player.getAdvancements().award(root, criterion);
            player.inventoryMenu.broadcastChanges();
        }, server);
    }

    private static void openSign(Minecraft client, BlockPos pos, boolean front) {
        var server = client.getSingleplayerServer();
        var id = client.player.getUUID();
        serverWork = CompletableFuture.runAsync(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (!(player.serverLevel().getBlockEntity(pos) instanceof SignBlockEntity sign)) {
                throw new IllegalStateException("No sign block entity at " + pos);
            }
            ((net.minecraft.world.level.block.SignBlock) sign.getBlockState().getBlock())
                    .openTextEdit(player, sign, front);
        }, server);
    }

    private static boolean verifySign(Minecraft client, BlockPos pos, boolean front, String expected) {
        if (expected.equals(observedSign)) {
            observedSign = null;
            signWait = 0;
            return true;
        }
        check(++signWait < 80, "Server did not persist sign text: expected=" + expected + " actual=" + observedSign);
        var server = client.getSingleplayerServer();
        serverWork = CompletableFuture.runAsync(() -> {
            check(server.overworld().getBlockEntity(pos) instanceof SignBlockEntity, "Sign block entity disappeared");
            SignBlockEntity sign = (SignBlockEntity) server.overworld().getBlockEntity(pos);
            observedSign = sign.getText(front).getMessage(0, false).getString();
        }, server);
        return false;
    }

    private static ItemStack writableBook() {
        ItemStack stack = new ItemStack(Items.WRITABLE_BOOK);
        CompoundTag tag = new CompoundTag();
        ListTag pages = new ListTag();
        pages.add(StringTag.valueOf("editable"));
        tag.put("pages", pages);
        stack.setTag(tag);
        return stack;
    }

    private static ItemStack writtenBook(String first, String second) {
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = new CompoundTag();
        tag.putString("title", "Frontend Book");
        tag.putString("author", "probe");
        ListTag pages = new ListTag();
        pages.add(StringTag.valueOf("{\"text\":\"" + first + "\"}"));
        pages.add(StringTag.valueOf("{\"text\":\"" + second + "\"}"));
        tag.put("pages", pages);
        stack.setTag(tag);
        return stack;
    }

    /** Native progress-listener lifecycle, explicitly separate from a disk-I/O transition. */
    private static void progressSurface(Minecraft client) {
        switch (++age) {
            case 1 -> {
                ProgressScreen progress = new ProgressScreen(true);
                progress.progressStartNoAbort(Component.literal("Native progress contract"));
                progress.progressStage(Component.literal("UI callbacks — not disk I/O"));
                progress.progressStagePercentage(35);
                client.setScreen(progress);
                identity = progress;
            }
            case 3 -> queueShot("frontend_progress_contract_35.png");
            case 4 -> {
                ((ProgressScreen) client.screen).progressStagePercentage(75);
                queueShot("frontend_progress_contract_75.png");
            }
            case 5 -> {
                press(client, GLFW.GLFW_KEY_ESCAPE);
                check(client.screen == identity, "ProgressScreen lost its non-cancellable native lifecycle");
                ((ProgressScreen) client.screen).stop();
            }
            case 7 -> {
                check(client.screen == null, "Native progress stop did not dismiss its screen");
                progressContractPassed = true;
                phase = 19;
                age = 0;
            }
            default -> { }
        }
    }

    private static void noteTransient(Minecraft client) {
        Screen screen = client.screen;
        Overlay overlay = client.getOverlay();
        if (screen instanceof LevelLoadingScreen) {
            if (++loadingHold == 2 && !sawLoading) {
                sawLoading = true;
                shot(client, "frontend_level_loading.png");
            }
        } else loadingHold = 0;
        if (screen instanceof ReceivingLevelScreen) {
            if (++receivingHold == 2 && !sawReceiving) {
                sawReceiving = true;
                shot(client, "frontend_receiving_level.png");
            }
        } else receivingHold = 0;
        if (phase != 18 && screen instanceof ProgressScreen) {
            if (++progressHold == 2 && !sawProgress) {
                sawProgress = true;
                shot(client, "frontend_progress.png");
            }
        } else progressHold = 0;
        if (phase == 13 && overlay != null && overlay.getClass() == LoadingOverlay.class) {
            if (++overlayHold == 2 && !sawOverlay) {
                sawOverlay = true;
                shot(client, "frontend_loading_overlay.png");
            }
        } else overlayHold = 0;
    }

    private static WorldSelectionList.WorldListEntry worldEntry(Screen screen) {
        if (!(screen instanceof SelectWorldScreen)) return null;
        for (var child : screen.children()) {
            if (child instanceof WorldSelectionList list) {
                for (WorldSelectionList.Entry entry : list.children()) {
                    if (entry instanceof WorldSelectionList.WorldListEntry world
                            && worldId.equals(world.getLevelName())) {
                        return world;
                    }
                }
            }
        }
        return null;
    }

    private static void clickNextPage(Screen screen) {
        PageButton button = screen.children().stream()
                .filter(PageButton.class::isInstance)
                .map(PageButton.class::cast)
                .filter(value -> value.active && value.visible)
                .max(Comparator.comparingInt(AbstractWidget::getX))
                .orElseThrow(() -> new IllegalStateException("Missing active page button"));
        clickWidget(screen, button);
    }

    private static void clickCycle(Screen screen) {
        CycleButton<?> button = screen.children().stream()
                .filter(CycleButton.class::isInstance)
                .map(CycleButton.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing native cycle button"));
        Object before = button.getValue();
        clickWidget(screen, button);
        check(!java.util.Objects.equals(before, button.getValue()), "Native cycle control did not change its value");
    }

    private static void clickDoneOrClose(Minecraft client) {
        if (client.screen == null) return;
        if (hasButton(client.screen, "gui.done")) click(client.screen, "gui.done");
        else client.screen.onClose();
    }

    private static EditBox firstEdit(Screen screen) {
        List<EditBox> boxes = edits(screen);
        check(!boxes.isEmpty(), "Missing native EditBox on " + type(screen));
        return boxes.get(0);
    }

    private static List<EditBox> edits(Screen screen) {
        return screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).toList();
    }

    private static <T> T child(Screen screen, Class<T> type) {
        return screen.children().stream().filter(type::isInstance).map(type::cast).findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing " + type.getSimpleName() + " on " + type(screen)));
    }

    private static Button button(Screen screen, String key) {
        String text = Component.translatable(key).getString();
        return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(value -> value.getMessage().getString().equals(text)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing native control '" + text + "' on "
                        + type(screen) + " have " + labels(screen)));
    }

    private static boolean hasButton(Screen screen, String key) {
        String text = Component.translatable(key).getString();
        return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .anyMatch(value -> value.getMessage().getString().equals(text));
    }

    private static String labels(Screen screen) {
        return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .map(button -> button.getMessage().getString()).toList().toString();
    }

    private static void click(Screen screen, String key) {
        clickWidget(screen, button(screen, key));
    }

    private static void clickFirst(Screen screen, String... keys) {
        for (String key : keys) {
            if (hasButton(screen, key)) {
                click(screen, key);
                return;
            }
        }
        throw new IllegalStateException("Missing native control " + String.join("/", keys) + " on "
                + type(screen) + " have " + labels(screen));
    }

    private static void clickWidget(Screen screen, AbstractWidget widget) {
        double x = widget.getX() + widget.getWidth() / 2.0;
        double y = widget.getY() + widget.getHeight() / 2.0;
        check(screen.mouseClicked(x, y, 0), "Native control rejected pointer: " + widget.getMessage().getString());
        screen.mouseReleased(x, y, 0);
    }

    private static void type(Screen screen, String text) {
        for (int i = 0; i < text.length(); i++) {
            check(screen.charTyped(text.charAt(i), 0), "Native field rejected '" + text.charAt(i) + "' on " + type(screen));
        }
    }

    private static void key(Minecraft client, int code, int action) {
        client.keyboardHandler.keyPress(client.getWindow().getWindow(), code, 0, action, 0);
    }

    private static void press(Minecraft client, int code) {
        key(client, code, GLFW.GLFW_PRESS);
        key(client, code, GLFW.GLFW_RELEASE);
    }

    private static void queueShot(String name) {
        queuedShot = name;
        settle = 0;
    }

    private static void shot(Minecraft client, String name) {
        SAOMenuPreview.grab(client, System.getProperty("saomenu.preview"), name);
    }

    private static String type(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName();
    }

    private static void finish(Minecraft client) {
        if (!sawLoading) residuals.add("LevelLoadingScreen was not observed");
        if (!sawReceiving) residuals.add("ReceivingLevelScreen was not observed");
        if (!sawProgress && !progressContractPassed) residuals.add("ProgressScreen was not exercised");
        if (!sawBedChat) residuals.add("InBedChatScreen was not observed");
        if (!WORLD_ONLY && !connectionCanceled) residuals.add("Native connect cancellation was not observed");
        if (!WORLD_ONLY && !sawDisconnected) residuals.add("Native DisconnectedScreen was not observed");
        check(residuals.isEmpty(), "Frontend verification incomplete: " + residuals);
        done = true;
        SAOMenu.LOGGER.info("[SAOMenu] frontend native {} checks passed: world={}, residuals={}",
                WORLD_ONLY ? "world" : "full", worldId, residuals);
        if (!Boolean.getBoolean("saomenu.preview.keepOpen")) client.stop();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
