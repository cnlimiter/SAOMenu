package com.sao.saomenu.dev.preview.network;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.sao.saomenu.SAOMenu;
import com.sao.saomenu.api.SaoUi;
import com.sao.saomenu.api.SaoUiRegistry;
import com.sao.saomenu.api.hud.HudBox;
import com.sao.saomenu.api.hud.HudElement;
import com.sao.saomenu.api.hud.HudLayoutBinding;
import com.sao.saomenu.api.hud.HudPass;
import com.sao.saomenu.api.lifecycle.SessionListener;
import com.sao.saomenu.api.menu.MenuEntry;
import com.sao.saomenu.api.menu.MenuHost;
import com.sao.saomenu.client.menu.MenuLayout;
import com.sao.saomenu.client.menu.SAOMenuScreen;
import com.sao.saomenu.client.menu.SaoPanels;
import com.sao.saomenu.client.party.SAOClientPartyState;
import com.sao.saomenu.client.render.target.SAOTargetBar3D;
import com.sao.saomenu.client.screen.SAOInviteScreen;
import com.sao.saomenu.config.SAOConfig;
import com.sao.saomenu.dev.preview.SAOMenuPreview;
import com.sao.saomenu.network.c2s.DropItemC2S;
import com.sao.saomenu.network.c2s.DualWieldC2S;
import com.sao.saomenu.network.c2s.EquipItemC2S;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Independent client probe for {@code saomenu.preview.multiplayer=alpha|beta}.
 * Starts from native TitleScreen controls. Does not skip SafetyScreen.
 */
public final class MultiplayerNativePreview {
    private static final ResourceLocation HUD_ID = new ResourceLocation("saomenu_preview", "multiplayer_hud");
    private static final ResourceLocation SESSION_ID = new ResourceLocation("saomenu_preview", "multiplayer_session");
    private static final int TICK_LIMIT = 12000;
    private static final int HUD_SIZE = 24;

    private static final float[] hudPos = {MultiplayerPreviewSupport.HUD_HOME_X, MultiplayerPreviewSupport.HUD_HOME_Y};
    private static int hudSaves;
    private static HudLayoutBinding hudBinding;
    private static final List<String> transitions = new ArrayList<>();
    private static int joins;

    private static String role;
    private static String title;
    private static int ticks;
    private static int age;
    private static int lane;
    private static int settle;
    private static String queuedShot;
    private static boolean done;
    private static boolean safetyPaused;
    private static int safetyOccurrence;
    private static boolean connecting = true;
    private static boolean reconnect;
    private static List<ItemStack> snapshot;
    private static boolean dragHeld;

    private MultiplayerNativePreview() {
    }

    public static boolean requested() {
        return SAOMenuPreview.requested() && MultiplayerPreviewSupport.isClientRole();
    }

    public static void register() {
        if (!requested()) {
            return;
        }
        role = MultiplayerPreviewSupport.role();
        title = MultiplayerPreviewSupport.ROLE_ALPHA.equals(role)
                ? "SAOMenu verify Alpha" : "SAOMenu verify Beta";
        ClientTickEvent.CLIENT_POST.register(MultiplayerNativePreview::tick);
    }

    public static void registerUi(SaoUiRegistry registry) {
        if (!requested()) {
            return;
        }
        hudBinding = HudLayoutBinding.screenAnchor(500, MultiplayerNativePreview::hudBox,
                () -> hudPos[0], () -> hudPos[1],
                (x, y) -> {
                    hudPos[0] = x;
                    hudPos[1] = y;
                },
                () -> hudSaves++);
        registry.hud(HudElement.builder(HUD_ID, 2500, Component.literal("MP HUD"), context -> {
            HudBox box = hudBox(context.minecraft(), context.width(), context.height());
            context.graphics().fill(box.x(), box.y(), box.x() + box.width(), box.y() + box.height(), 0xFFDC35D5);
        }).visible(context -> context.player() != null)
                .passes(HudPass.WORLD, HudPass.MENU_OVERLAY)
                .layout(hudBinding)
                .build());
        registry.session(SessionListener.of(SESSION_ID, 2000, MultiplayerNativePreview::onLevelChanged));
    }

    private static void tick(Minecraft client) {
        if (done) {
            return;
        }
        client.getWindow().setTitle(title);
        if (client.screen instanceof SafetyScreen) {
            if (!safetyPaused) {
                safetyPaused = true;
                safetyOccurrence++;
                MultiplayerPreviewSupport.setAwaitingSafety(role, true);
                SAOMenu.LOGGER.info("[SAOMenu] multiplayer safety approval required; role={} window={} occurrence={} approvalFile={}",
                        role, title, safetyOccurrence, MultiplayerPreviewSupport.safetyApprovalFile(role, safetyOccurrence));
            }
            return;
        }
        if (safetyPaused) {
            if (!Files.isRegularFile(MultiplayerPreviewSupport.safetyApprovalFile(role, safetyOccurrence))) return;
            MultiplayerPreviewSupport.setAwaitingSafety(role, false);
            safetyPaused = false;
        }
        boolean approvalPending = MultiplayerPreviewSupport.safetyWaiting();
        if (approvalPending && !connecting) return;
        if (!approvalPending && !MultiplayerPreviewSupport.STAGE_BOOT.equals(serverStage()) && ++ticks > TICK_LIMIT) {
            throw new IllegalStateException("Multiplayer native preview timed out role=" + role
                    + " lane=" + lane + " age=" + age + " screen=" + type(client.screen)
                    + " overlay=" + type(client.getOverlay()) + " stage=" + serverStage());
        }
        if (queuedShot != null) {
            if (++settle < 2) {
                return;
            }
            shot(client, queuedShot);
            queuedShot = null;
            settle = 0;
        }
        if (!connecting && reconnect && client.player == null && lane != 80) {
            connecting = true;
            age = 0;
        }
        if (connecting) {
            connect(client);
            return;
        }
        work(client);
    }

    private static void connect(Minecraft client) {
        if (client.getOverlay() != null) {
            return;
        }
        if (age == 0) {
            if (SAOConfig.path() == null) {
                return;
            }
            String expectedName = MultiplayerPreviewSupport.ROLE_ALPHA.equals(role)
                    ? MultiplayerPreviewSupport.ALPHA : MultiplayerPreviewSupport.BETA;
            MultiplayerPreviewSupport.require(expectedName.equals(client.getUser().getName()),
                    "Client username does not match its dedicated role");
            SaoUi.setEnabled(true);
            client.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            client.options.pauseOnLostFocus = false;
            client.options.guiScale().set(2);
            client.resizeDisplay();
            if (!(client.screen instanceof TitleScreen)) {
                client.setScreen(new TitleScreen());
            }
            age = 1;
            return;
        }
        if (age == 1) {
            MultiplayerPreviewSupport.require(client.screen != null && client.screen.getClass() == TitleScreen.class,
                    "Boot did not mount TitleScreen");
            queueShot(reconnect ? "mp_" + role + "_title_rejoin.png" : "mp_" + role + "_title.png");
            age = 2;
            return;
        }
        if (age == 2) {
            if (serverStage().isEmpty()) {
                return;
            }
            if (client.options.skipMultiplayerWarning) {
                // Preserve the user's preference, but never let it bypass this run's explicit approval.
                client.setScreen(new SafetyScreen(client.screen));
            } else {
                click(client.screen, "menu.multiplayer");
            }
            age = 3;
            return;
        }
        if (age >= 3 && age < 14) {
            if (client.screen instanceof JoinMultiplayerScreen) {
                age = 14;
            } else if (age == 13) {
                throw new IllegalStateException("JoinMultiplayerScreen did not open: " + type(client.screen));
            } else {
                age++;
            }
            return;
        }
        if (age == 14) {
            MultiplayerPreviewSupport.require(client.screen.getClass() == JoinMultiplayerScreen.class,
                    "JoinMultiplayerScreen missing");
            queueShot(reconnect ? "mp_" + role + "_multiplayer_rejoin.png" : "mp_" + role + "_multiplayer.png");
            age = 15;
            return;
        }
        if (age == 15) {
            click(client.screen, "selectServer.direct");
            age = 16;
            return;
        }
        if (age >= 16 && age < 24) {
            if (client.screen instanceof DirectJoinServerScreen) {
                age = 24;
            } else if (age == 23) {
                throw new IllegalStateException("DirectJoinServerScreen did not open: " + type(client.screen));
            } else {
                age++;
            }
            return;
        }
        if (age == 24) {
            MultiplayerPreviewSupport.require(client.screen.getClass() == DirectJoinServerScreen.class,
                    "DirectJoinServerScreen missing");
            EditBox box = firstEdit(client.screen);
            box.setValue(MultiplayerPreviewSupport.address());
            queueShot(reconnect ? "mp_" + role + "_direct_rejoin.png" : "mp_" + role + "_direct.png");
            age = 25;
            return;
        }
        if (age == 25) {
            if (hasButton(client.screen, "selectServer.select")) {
                click(client.screen, "selectServer.select");
            } else if (hasButton(client.screen, "selectWorld.select")) {
                click(client.screen, "selectWorld.select");
            } else {
                throw new IllegalStateException("Direct-connect confirm control missing on "
                        + type(client.screen) + " have " + labels(client.screen));
            }
            age = 26;
            return;
        }
        if (client.screen instanceof ConnectScreen) {
            age++;
            return;
        }
        if (client.screen instanceof DisconnectedScreen) {
            if (age < 40) {
                age++;
                return;
            }
            throw new IllegalStateException("Direct connect failed: " + type(client.screen));
        }
        if (client.player != null && client.level != null && client.screen == null && client.getOverlay() == null) {
            MultiplayerPreviewSupport.require(client.getSingleplayerServer() == null,
                    "Connected client still owns an integrated server");
            connecting = false;
            age = 0;
            if (reconnect) {
                lane = 90;
            } else {
                lane = 0;
            }
            queueShot(reconnect ? "mp_" + role + "_rejoin.png" : "mp_" + role + "_world.png");
            MultiplayerPreviewSupport.pass(role, reconnect ? "rejoin" : "connect",
                    "singleplayerServer=null address=" + MultiplayerPreviewSupport.address());
            return;
        }
        if (++age > 1200) {
            throw new IllegalStateException("Did not enter the dedicated world: " + type(client.screen)
                    + " overlay=" + type(client.getOverlay()));
        }
    }

    private static void work(Minecraft client) {
        if (client.player == null || client.level == null) {
            if (lane == 80) {
                observeDisconnect(client);
            }
            return;
        }
        MultiplayerPreviewSupport.require(client.getSingleplayerServer() == null,
                "Remote client acquired an integrated server");
        if (MultiplayerPreviewSupport.ROLE_ALPHA.equals(role)) {
            alpha(client);
        } else {
            beta(client);
        }
    }

    private static boolean awaitBothClients(Minecraft client) {
        if (MultiplayerPreviewSupport.STAGE_BOOT.equals(serverStage())) {
            if (!fixturesMatch(client)) {
                if (++age > 200) throw new IllegalStateException("Owned inventory did not sync before the boot barrier");
                return false;
            }
            if (!MultiplayerPreviewSupport.acked(role, MultiplayerPreviewSupport.STAGE_BOOT)) {
                ack(MultiplayerPreviewSupport.STAGE_BOOT, extras(client));
            }
            return false;
        }
        return MultiplayerPreviewSupport.STAGE_FIXTURES.equals(serverStage());
    }

    private static void alpha(Minecraft client) {
        switch (lane) {
            case 0 -> {
                if (!awaitBothClients(client)) {
                    return;
                }
                if (!fixturesMatch(client)) {
                    if (++age > 200) {
                        throw new IllegalStateException("Alpha client inventory did not sync fixtures");
                    }
                    return;
                }
                MultiplayerPreviewSupport.pass(role, MultiplayerPreviewSupport.STAGE_FIXTURES, "client inventory synced");
                advance(10);
            }
            case 10 -> {
                snapshot = copyInv(client);
                new DualWieldC2S(MultiplayerPreviewSupport.SLOT_DIAMOND, MultiplayerPreviewSupport.SLOT_DIAMOND)
                        .sendToServer();
                advance(11);
            }
            case 11 -> waitUnchanged(client, 12, "same-slot dual wield");
            case 12 -> {
                snapshot = copyInv(client);
                new DualWieldC2S(MultiplayerPreviewSupport.SLOT_DIAMOND, MultiplayerPreviewSupport.SLOT_APPLES)
                        .sendToServer();
                advance(13);
            }
            case 13 -> waitUnchanged(client, 14, "non-sword dual wield");
            case 14 -> {
                new EquipItemC2S(MultiplayerPreviewSupport.SLOT_CHEST).sendToServer();
                advance(15);
            }
            case 15 -> {
                if (client.player.getInventory().armor.get(2).is(Items.IRON_CHESTPLATE)) {
                    MultiplayerPreviewSupport.pass(role, "equip", "iron chestplate synced");
                    advance(16);
                } else if (++age > 80) {
                    throw new IllegalStateException("Client did not sync equipped chestplate");
                }
            }
            case 16 -> {
                new DropItemC2S(MultiplayerPreviewSupport.SLOT_APPLES, false).sendToServer();
                advance(17);
            }
            case 17 -> {
                if (count(client, Items.APPLE) == MultiplayerPreviewSupport.APPLE_AFTER_DROP) {
                    MultiplayerPreviewSupport.pass(role, "drop", "apple count synced");
                    advance(18);
                } else if (++age > 80) {
                    throw new IllegalStateException("Client did not sync dropped apple");
                }
            }
            case 18 -> {
                new DualWieldC2S(MultiplayerPreviewSupport.SLOT_DIAMOND, MultiplayerPreviewSupport.SLOT_GOLD)
                        .sendToServer();
                advance(19);
            }
            case 19 -> {
                if (client.player.getMainHandItem().is(Items.DIAMOND_SWORD)
                        && client.player.getOffhandItem().is(Items.GOLDEN_SWORD)
                        && count(client, Items.DIAMOND_SWORD) == 1
                        && count(client, Items.GOLDEN_SWORD) == 1) {
                    snapshot = copyInv(client);
                    new DualWieldC2S(MultiplayerPreviewSupport.SLOT_IRON, MultiplayerPreviewSupport.SLOT_STONE)
                            .sendToServer();
                    MultiplayerPreviewSupport.pass(role, "dual_wield", "swords synced");
                    advance(20);
                } else if (++age > 80) {
                    throw new IllegalStateException("Client did not sync dual wield");
                }
            }
            case 20 -> waitUnchanged(client, 21, "cooldown dual wield");
            case 21 -> {
                if (!MultiplayerPreviewSupport.STAGE_INVENTORY.equals(serverStage())) {
                    if (++age > 200) {
                        throw new IllegalStateException("Server did not publish inventory stage");
                    }
                    return;
                }
                queueShot("mp_alpha_inventory.png");
                ack(MultiplayerPreviewSupport.STAGE_INVENTORY, extras(client));
                MultiplayerPreviewSupport.pass(role, MultiplayerPreviewSupport.STAGE_INVENTORY, "client+server inventory");
                advance(30);
            }
            case 30 -> invite(client);
            case 40 -> {
                if (!SAOClientPartyState.teamMembers().contains(MultiplayerPreviewSupport.ALPHA)
                        || !SAOClientPartyState.teamMembers().contains(MultiplayerPreviewSupport.BETA)) {
                    if (++age > 200) {
                        throw new IllegalStateException("Alpha party cache missing members "
                                + SAOClientPartyState.teamMembers());
                    }
                    return;
                }
                if (!MultiplayerPreviewSupport.STAGE_PARTY_JOINED.equals(serverStage())) {
                    return;
                }
                queueShot("mp_alpha_party.png");
                ack(MultiplayerPreviewSupport.STAGE_PARTY_JOINED, extras(client));
                MultiplayerPreviewSupport.pass(role, MultiplayerPreviewSupport.STAGE_PARTY_JOINED,
                        "members=" + SAOClientPartyState.teamMembers());
                advance(50);
            }
            case 50 -> {
                if (!MultiplayerPreviewSupport.STAGE_PARTY_LEFT.equals(serverStage())) {
                    return;
                }
                if (SAOClientPartyState.teamMembers().contains(MultiplayerPreviewSupport.BETA)) {
                    if (++age > 200) {
                        throw new IllegalStateException("Alpha still lists Beta after leave");
                    }
                    return;
                }
                advance(60);
            }
            case 60 -> startHudDrag(client, 61, "mp_alpha_hud_drag.png");
            case 61 -> {
                MultiplayerPreviewSupport.require(dragHeld && hudSaves == 0, "Alpha HUD drag was saved before teleport");
                ack(MultiplayerPreviewSupport.STAGE_PARTY_LEFT, extras(client));
                MultiplayerPreviewSupport.pass(role, "hud_pending", "addon HUD drag live across dimension");
                advance(70);
            }
            case 70 -> {
                if (!hasTransition(Level.OVERWORLD, Level.NETHER)) {
                    if (++age > 400) {
                        throw new IllegalStateException("SessionListener missed overworld→nether: " + transitions);
                    }
                    return;
                }
                assertHudRolledBack("dimension nether");
                queueShot("mp_alpha_nether.png");
                advance(71);
            }
            case 71 -> {
                if (!hasTransition(Level.NETHER, Level.OVERWORLD)) {
                    if (++age > 400) {
                        throw new IllegalStateException("SessionListener missed nether→overworld: " + transitions);
                    }
                    return;
                }
                assertHudRolledBack("dimension return");
                if (!MultiplayerPreviewSupport.STAGE_DIMENSION.equals(serverStage())) {
                    return;
                }
                if (client.screen instanceof SAOMenuScreen) {
                    client.screen.onClose();
                }
                queueShot("mp_alpha_overworld.png");
                ack(MultiplayerPreviewSupport.STAGE_DIMENSION, extras(client));
                MultiplayerPreviewSupport.pass(role, MultiplayerPreviewSupport.STAGE_DIMENSION,
                        "listener=" + transitions + " hudSaves=" + hudSaves);
                advance(100);
            }
            case 100 -> {
                if (MultiplayerPreviewSupport.STAGE_PASSED.equals(serverStage())) {
                    finish(client);
                }
            }
            default -> { }
        }
    }

    private static void beta(Minecraft client) {
        switch (lane) {
            case 0 -> {
                if (!awaitBothClients(client)) {
                    return;
                }
                ack(MultiplayerPreviewSupport.STAGE_FIXTURES, extras(client));
                MultiplayerPreviewSupport.pass(role, MultiplayerPreviewSupport.STAGE_FIXTURES, "joined");
                advance(10);
            }
            case 10 -> {
                if (!MultiplayerPreviewSupport.STAGE_INVENTORY.equals(serverStage())) {
                    return;
                }
                ack(MultiplayerPreviewSupport.STAGE_INVENTORY, extras(client));
                advance(20);
            }
            case 20 -> {
                if (client.screen instanceof SAOInviteScreen) {
                    MultiplayerPreviewSupport.require(MultiplayerPreviewSupport.ALPHA.equals(
                            SAOClientPartyState.pendingInviter()), "Invite came from an unexpected player");
                    advance(21);
                    return;
                }
                if (SAOClientPartyState.pendingInviter() != null && !(client.screen instanceof SAOInviteScreen)) {
                    throw new IllegalStateException("Invite packet arrived but SAOInviteScreen did not mount; pending="
                            + SAOClientPartyState.pendingInviter() + " screen=" + type(client.screen));
                }
                if (++age > 400) {
                    throw new IllegalStateException("SAOInviteScreen did not appear; pending="
                            + SAOClientPartyState.pendingInviter());
                }
            }
            case 21 -> {
                MultiplayerPreviewSupport.require(client.screen instanceof SAOInviteScreen,
                        "Invite disappeared before its rendered acceptance");
                if (++age < 12) return;
                queueShot("mp_beta_invite.png");
                advance(22);
            }
            case 22 -> {
                MultiplayerPreviewSupport.require(client.screen instanceof SAOInviteScreen,
                        "Invite disappeared before the native pointer action");
                clickInviteAccept(client.screen);
                MultiplayerPreviewSupport.pass(role, "invite_accept", "native pointer on rendered SAOInviteScreen");
                advance(30);
            }
            case 30 -> {
                if (!SAOClientPartyState.teamMembers().contains(MultiplayerPreviewSupport.ALPHA)
                        || !SAOClientPartyState.teamMembers().contains(MultiplayerPreviewSupport.BETA)) {
                    if (++age > 200) {
                        throw new IllegalStateException("Beta party cache missing members "
                                + SAOClientPartyState.teamMembers());
                    }
                    return;
                }
                if (!MultiplayerPreviewSupport.STAGE_PARTY_JOINED.equals(serverStage())) {
                    return;
                }
                queueShot("mp_beta_party.png");
                ack(MultiplayerPreviewSupport.STAGE_PARTY_JOINED, extras(client));
                MultiplayerPreviewSupport.pass(role, MultiplayerPreviewSupport.STAGE_PARTY_JOINED,
                        "members=" + SAOClientPartyState.teamMembers());
                advance(40);
            }
            case 40 -> {
                if (MultiplayerPreviewSupport.acked(MultiplayerPreviewSupport.ROLE_ALPHA,
                        MultiplayerPreviewSupport.STAGE_PARTY_JOINED)) leaveParty(client);
            }
            case 50 -> {
                if (SAOClientPartyState.inParty() || !SAOClientPartyState.teamMembers().isEmpty()) {
                    if (++age > 200) {
                        throw new IllegalStateException("Beta client party was not cleared: title="
                                + SAOClientPartyState.teamTitle() + " members=" + SAOClientPartyState.teamMembers());
                    }
                    return;
                }
                if (!MultiplayerPreviewSupport.STAGE_PARTY_LEFT.equals(serverStage())) {
                    return;
                }
                ack(MultiplayerPreviewSupport.STAGE_PARTY_LEFT, extras(client));
                MultiplayerPreviewSupport.pass(role, MultiplayerPreviewSupport.STAGE_PARTY_LEFT, "client party empty");
                advance(60);
            }
            case 60 -> {
                if (!MultiplayerPreviewSupport.STAGE_DIMENSION.equals(serverStage())) {
                    return;
                }
                advance(70);
            }
            case 70 -> startHudDrag(client, 71, "mp_beta_hud_drag.png");
            case 71 -> {
                MultiplayerPreviewSupport.require(hudSaves == 0, "Beta HUD drag saved before disconnect");
                if (client.screen instanceof SAOMenuScreen) {
                    press(client, GLFW.GLFW_KEY_ESCAPE);
                    if (++age > 40) {
                        throw new IllegalStateException("Escape did not close the SAO menu");
                    }
                    return;
                }
                press(client, GLFW.GLFW_KEY_ESCAPE);
                advance(80);
            }
            case 80 -> {
                if (client.screen instanceof PauseScreen) {
                    if (!hasButton(client.screen, "menu.disconnect")) {
                        if (++age > 40) {
                            throw new IllegalStateException("PauseScreen missing menu.disconnect; have "
                                    + labels(client.screen));
                        }
                        return;
                    }
                    click(client.screen, "menu.disconnect");
                    return;
                }
                if (client.player != null) {
                    if (++age > 80) {
                        throw new IllegalStateException("Beta did not disconnect: " + type(client.screen));
                    }
                }
            }
            case 90 -> {
                MultiplayerPreviewSupport.require(client.getSingleplayerServer() == null,
                        "Rejoined client owns an integrated server");
                if (joins < 2) {
                    if (++age > 100) {
                        throw new IllegalStateException("SessionListener missed reconnect: " + transitions);
                    }
                    return;
                }
                if (!MultiplayerPreviewSupport.STAGE_BETA_REJOIN.equals(serverStage())
                        && !MultiplayerPreviewSupport.STAGE_PASSED.equals(serverStage())) {
                    if (++age > 200) {
                        throw new IllegalStateException("Server did not publish beta_rejoined");
                    }
                    return;
                }
                ack(MultiplayerPreviewSupport.STAGE_BETA_REJOIN, extras(client));
                MultiplayerPreviewSupport.pass(role, MultiplayerPreviewSupport.STAGE_BETA_REJOIN,
                        "listener=" + transitions);
                advance(100);
            }
            case 100 -> {
                if (MultiplayerPreviewSupport.STAGE_PASSED.equals(serverStage())) {
                    finish(client);
                }
            }
            default -> { }
        }
    }

    private static void observeDisconnect(Minecraft client) {
        if (client.screen instanceof DisconnectedScreen) {
            if (hasButton(client.screen, "gui.toMenu")) {
                click(client.screen, "gui.toMenu");
            } else if (hasButton(client.screen, "gui.back")) {
                click(client.screen, "gui.back");
            }
            return;
        }
        if (client.screen instanceof JoinMultiplayerScreen) {
            click(client.screen, "gui.cancel");
            return;
        }
        if (!(client.screen instanceof TitleScreen) && client.getOverlay() == null) {
            if (++age > 100) {
                throw new IllegalStateException("Disconnect did not return to TitleScreen: " + type(client.screen));
            }
            return;
        }
        MultiplayerPreviewSupport.require(!SAOClientPartyState.inParty() && SAOClientPartyState.teamMembers().isEmpty(),
                "Party cache survived disconnect");
        MultiplayerPreviewSupport.require(hudSaves == 0, "HUD save ran on disconnect");
        MultiplayerPreviewSupport.require(Math.abs(hudPos[0] - MultiplayerPreviewSupport.HUD_HOME_X) < 1.0e-4f
                        && Math.abs(hudPos[1] - MultiplayerPreviewSupport.HUD_HOME_Y) < 1.0e-4f,
                "HUD anchors were not rolled back on disconnect");
        MultiplayerPreviewSupport.require(hasNullCurrent(), "SessionListener missed disconnect: " + transitions);
        if (MultiplayerPreviewSupport.STAGE_BETA_DISC.equals(serverStage())
                || MultiplayerPreviewSupport.STAGE_BETA_REJOIN.equals(serverStage())
                || MultiplayerPreviewSupport.STAGE_PASSED.equals(serverStage())) {
            queueShot("mp_beta_disconnect.png");
            ack(MultiplayerPreviewSupport.STAGE_BETA_DISC, extras(client));
            MultiplayerPreviewSupport.pass(role, MultiplayerPreviewSupport.STAGE_BETA_DISC, "title + party reset");
            reconnect = true;
            connecting = true;
            safetyPaused = false;
            age = 0;
            lane = 0;
            dragHeld = false;
        }
    }

    private static void invite(Minecraft client) {
        if (!(client.screen instanceof SAOMenuScreen)) {
            if (client.screen != null) {
                client.screen.onClose();
                return;
            }
            press(client, GLFW.GLFW_KEY_O);
            if (++age > 40) {
                throw new IllegalStateException("O did not open SAOMenuScreen");
            }
            return;
        }
        SAOMenuScreen menu = (SAOMenuScreen) client.screen;
        if (age < 16) {
            age++;
            menu.selectPanel(SaoPanels.PARTY);
            return;
        }
        if (age == 16) {
            clickMenuRow(menu, SaoPanels.PARTY, 0, false);
            age = 17;
            return;
        }
        if (age < 28) {
            if (age == 26) queueShot("mp_alpha_invite_target.png");
            age++;
            return;
        }
        List<MenuEntry> party = SaoUi.panels().stream()
                .filter(panel -> SaoPanels.PARTY.equals(panel.id()))
                .findFirst()
                .orElseThrow()
                .items()
                .get();
        MultiplayerPreviewSupport.require(!party.isEmpty() && party.get(0).hasChildren(),
                "Party invite submenu did not expand");
        List<MenuEntry> children = party.get(0).children();
        int index = -1;
        for (int i = 0; i < children.size(); i++) {
            if (MultiplayerPreviewSupport.BETA.equals(children.get(i).label().getString())) {
                index = i;
                break;
            }
        }
        MultiplayerPreviewSupport.require(index >= 0, "Invite column missing " + MultiplayerPreviewSupport.BETA
                + " have " + children.stream().map(entry -> entry.label().getString()).toList());
        clickMenuRow(menu, SaoPanels.PARTY, index, true);
        press(client, GLFW.GLFW_KEY_ESCAPE);
        SAOMenu.LOGGER.info("[SAOMenu] multiplayer Alpha clicked the native invite target {}", MultiplayerPreviewSupport.BETA);
        advance(40);
    }

    private static void leaveParty(Minecraft client) {
        if (!(client.screen instanceof SAOMenuScreen)) {
            if (client.screen != null) {
                client.screen.onClose();
                return;
            }
            press(client, GLFW.GLFW_KEY_O);
            if (++age > 40) {
                throw new IllegalStateException("O did not open SAOMenuScreen for leave");
            }
            return;
        }
        SAOMenuScreen menu = (SAOMenuScreen) client.screen;
        if (age < 16) {
            age++;
            menu.selectPanel(SaoPanels.PARTY);
            return;
        }
        clickMenuRow(menu, SaoPanels.PARTY, 1, false);
        press(client, GLFW.GLFW_KEY_ESCAPE);
        MultiplayerPreviewSupport.pass(role, "leave", "LeaveC2S via menu pointer");
        advance(50);
    }

    private static void startHudDrag(Minecraft client, int nextLane, String shotName) {
        if (!(client.screen instanceof SAOMenuScreen)) {
            if (client.screen != null) {
                client.screen.onClose();
                return;
            }
            press(client, GLFW.GLFW_KEY_O);
            if (++age > 40) {
                throw new IllegalStateException("O did not open SAOMenuScreen for HUD drag");
            }
            return;
        }
        if (age < 8) {
            age++;
            return;
        }
        HudBox box = hudBox(client, client.screen.width, client.screen.height);
        double x = box.x() + box.width() / 2.0;
        double y = box.y() + box.height() / 2.0;
        MultiplayerPreviewSupport.require(client.screen.mouseClicked(x, y, 0),
                "Addon HUD rejected the native pointer");
        client.screen.mouseMoved(x - 70, y + 40);
        MultiplayerPreviewSupport.require(hudPos[0] < MultiplayerPreviewSupport.HUD_HOME_X - 0.02f
                        || hudPos[1] > MultiplayerPreviewSupport.HUD_HOME_Y + 0.02f,
                "Addon HUD did not follow the pointer");
        MultiplayerPreviewSupport.require(hudSaves == 0, "HUD save ran on mouse move");
        dragHeld = true;
        queueShot(shotName);
        advance(nextLane);
    }

    private static void waitUnchanged(Minecraft client, int nextLane, String reason) {
        if (++age < 8) {
            return;
        }
        MultiplayerPreviewSupport.require(sameInv(snapshot, client), "Inventory changed after " + reason);
        MultiplayerPreviewSupport.pass(role, reason, "client inventory unchanged");
        advance(nextLane);
    }

    private static void assertHudRolledBack(String reason) {
        MultiplayerPreviewSupport.require(hudSaves == 0, "HUD save invoked during " + reason);
        MultiplayerPreviewSupport.require(Math.abs(hudPos[0] - MultiplayerPreviewSupport.HUD_HOME_X) < 1.0e-4f
                        && Math.abs(hudPos[1] - MultiplayerPreviewSupport.HUD_HOME_Y) < 1.0e-4f,
                "HUD anchors were not rolled back during " + reason + " x=" + hudPos[0] + " y=" + hudPos[1]);
        dragHeld = false;
    }

    private static void clickMenuRow(SAOMenuScreen menu, ResourceLocation panelId, int row, boolean child) {
        int panel = -1;
        List<MenuEntry> items = List.of();
        var panels = SaoUi.panels();
        for (int i = 0; i < panels.size(); i++) {
            if (panelId.equals(panels.get(i).id())) {
                panel = i;
                items = panels.get(i).items().get();
                break;
            }
        }
        MultiplayerPreviewSupport.require(panel >= 0, "Unknown panel " + panelId);
        if (!child) menu.selectMain(panel);
        int anchorY = MenuLayout.buttonCenterY(menu.height, panel);
        if (child) {
            MultiplayerPreviewSupport.require(!items.isEmpty() && items.get(0).hasChildren(),
                    "No child column for " + panelId);
            anchorY = MenuLayout.menuItemRect(menu.width, menu.height, items.size(), anchorY, 0).centerY();
            items = items.get(0).children();
        }
        MultiplayerPreviewSupport.require(row >= 0 && row < items.size(), "Row " + row + " missing on " + panelId);
        var bounds = child
                ? MenuLayout.childItemRect(menu.width, menu.height, items.size(), anchorY, row)
                : MenuLayout.menuItemRect(menu.width, menu.height, items.size(), anchorY, row);
        float[] at = menu.screenPointOf(bounds.centerX(), bounds.centerY());
        MultiplayerPreviewSupport.require(menu.mouseClicked(at[0], at[1], 0),
                "Menu row did not handle the native pointer");
        menu.mouseReleased(at[0], at[1], 0);
    }

    private static void clickInviteAccept(Screen screen) {
        int width = screen.width;
        int height = screen.height;
        int w = Math.min(280, width - 20);
        int h = Math.round(w * 253f / 350f);
        int x = (width - w) / 2;
        int y = (height - h) / 2 - 10;
        int d = 30;
        int bx = x + w / 4 - d / 2;
        int by = y + Math.round(h * 0.78f) - d / 2;
        double mx = bx + d / 2.0;
        double my = by + d / 2.0;
        MultiplayerPreviewSupport.require(screen.mouseClicked(mx, my, 0),
                "Invite accept control rejected the native pointer");
        screen.mouseReleased(mx, my, 0);
    }

    private static boolean fixturesMatch(Minecraft client) {
        var inv = client.player.getInventory();
        return inv.getItem(MultiplayerPreviewSupport.SLOT_APPLES).is(Items.APPLE)
                && inv.getItem(MultiplayerPreviewSupport.SLOT_APPLES).getCount() == MultiplayerPreviewSupport.APPLE_START
                && inv.getItem(MultiplayerPreviewSupport.SLOT_CHEST).is(Items.IRON_CHESTPLATE)
                && inv.getItem(MultiplayerPreviewSupport.SLOT_DIAMOND).is(Items.DIAMOND_SWORD)
                && inv.getItem(MultiplayerPreviewSupport.SLOT_GOLD).is(Items.GOLDEN_SWORD)
                && inv.getItem(MultiplayerPreviewSupport.SLOT_IRON).is(Items.IRON_SWORD)
                && inv.getItem(MultiplayerPreviewSupport.SLOT_STONE).is(Items.STONE_SWORD);
    }

    private static List<ItemStack> copyInv(Minecraft client) {
        var inv = client.player.getInventory();
        List<ItemStack> result = new ArrayList<>(inv.getContainerSize());
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            result.add(inv.getItem(slot).copy());
        }
        return result;
    }

    private static boolean sameInv(List<ItemStack> expected, Minecraft client) {
        if (expected == null) {
            return false;
        }
        var inv = client.player.getInventory();
        if (expected.size() != inv.getContainerSize()) {
            return false;
        }
        for (int slot = 0; slot < expected.size(); slot++) {
            if (!ItemStack.matches(expected.get(slot), inv.getItem(slot))) {
                return false;
            }
        }
        return true;
    }

    private static int count(Minecraft client, Item item) {
        int total = 0;
        var inv = client.player.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static HudBox hudBox(Minecraft minecraft, int width, int height) {
        int x = Math.round(hudPos[0] * Math.max(1, width - HUD_SIZE));
        int y = Math.round(hudPos[1] * Math.max(1, height - HUD_SIZE));
        return new HudBox(x, y, HUD_SIZE, HUD_SIZE);
    }

    private static void onLevelChanged(ClientLevel previous, ClientLevel current) {
        transitions.add(dim(previous) + "->" + dim(current));
        if (previous == null && current != null) joins++;
        if (previous != null) assertHudRolledBack("before SessionListener " + dim(previous) + "->" + dim(current));
    }

    private static String dim(ClientLevel level) {
        if (level == null) {
            return "null";
        }
        ResourceKey<Level> key = level.dimension();
        return key.location().toString();
    }

    private static boolean hasTransition(ResourceKey<Level> from, ResourceKey<Level> to) {
        String needle = (from == null ? "null" : from.location().toString()) + "->"
                + (to == null ? "null" : to.location().toString());
        return transitions.contains(needle);
    }


    private static boolean hasNullCurrent() {
        for (String value : transitions) {
            if (value.endsWith("->null")) {
                return true;
            }
        }
        return false;
    }

    private static JsonObject extras(Minecraft client) {
        JsonObject json = new JsonObject();
        json.addProperty("singleplayerServer", client.getSingleplayerServer() != null);
        json.addProperty("screen", type(client.screen));
        json.addProperty("partyTitle", SAOClientPartyState.teamTitle());
        json.addProperty("partyMembers", SAOClientPartyState.teamMembers().toString());
        json.addProperty("hudX", hudPos[0]);
        json.addProperty("hudY", hudPos[1]);
        json.addProperty("hudSaves", hudSaves);
        json.addProperty("transitions", transitions.toString());
        json.addProperty("visibleCount", SAOTargetBar3D.visibleCount());
        if (client.player != null) {
            json.addProperty("mainHand", String.valueOf(client.player.getMainHandItem().getItem()));
            json.addProperty("offHand", String.valueOf(client.player.getOffhandItem().getItem()));
        }
        return json;
    }

    private static void ack(String stage, JsonObject extras) {
        MultiplayerPreviewSupport.writeClientAck(role, stage, extras);
    }

    private static String serverStage() {
        return MultiplayerPreviewSupport.stageOf(MultiplayerPreviewSupport.readJson(MultiplayerPreviewSupport.stateFile()));
    }

    private static void advance(int next) {
        lane = next;
        age = 0;
    }

    private static void finish(Minecraft client) {
        done = true;
        SAOMenu.LOGGER.info("[SAOMenu] multiplayer {} checks passed: remote={}, singleplayer=null, transitions={}, hudSaves={}",
                role, MultiplayerPreviewSupport.address(), transitions, hudSaves);
        MultiplayerPreviewSupport.pass(role, MultiplayerPreviewSupport.STAGE_PASSED, "final");
        if (!MultiplayerPreviewSupport.keepOpen()) {
            client.stop();
        }
    }

    private static void queueShot(String name) {
        queuedShot = name;
        settle = 0;
    }

    private static void shot(Minecraft client, String name) {
        String out = System.getProperty(MultiplayerPreviewSupport.PROP_DIR);
        try (NativeImage image = Screenshot.takeScreenshot(client.getMainRenderTarget())) {
            Path dir = new File(out).toPath();
            Files.createDirectories(dir);
            Path file = dir.resolve(name);
            image.writeToFile(file);
            SAOMenu.LOGGER.info("[SAOMenu] preview written to {}", file.toAbsolutePath());
        } catch (IOException e) {
            SAOMenu.LOGGER.error("[SAOMenu] preview failed", e);
        }
    }

    private static EditBox firstEdit(Screen screen) {
        List<EditBox> boxes = screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).toList();
        MultiplayerPreviewSupport.require(!boxes.isEmpty(), "Missing native EditBox on " + type(screen));
        return boxes.get(0);
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
        Button control = button(screen, key);
        double x = control.getX() + control.getWidth() / 2.0;
        double y = control.getY() + control.getHeight() / 2.0;
        MultiplayerPreviewSupport.require(screen.mouseClicked(x, y, 0),
                "Native control rejected pointer: " + key);
        screen.mouseReleased(x, y, 0);
    }

    private static void press(Minecraft client, int code) {
        long window = client.getWindow().getWindow();
        client.keyboardHandler.keyPress(window, code, 0, GLFW.GLFW_PRESS, 0);
        client.keyboardHandler.keyPress(window, code, 0, GLFW.GLFW_RELEASE, 0);
    }

    private static String type(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName();
    }
}
