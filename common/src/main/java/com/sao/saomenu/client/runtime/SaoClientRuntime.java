package com.sao.saomenu.client.runtime;

import com.sao.saomenu.api.SaoUi;
import com.sao.saomenu.api.lifecycle.SessionListener;
import com.sao.saomenu.client.effect.SAODeathEffect;
import com.sao.saomenu.client.effect.SAOWelcome;
import com.sao.saomenu.client.hud.SAOBossBanner;
import com.sao.saomenu.client.hud.SAOHud;
import com.sao.saomenu.client.hud.SAOMapPanel;
import com.sao.saomenu.client.input.SAOFreeLook;
import com.sao.saomenu.client.input.SAOKeybinds;
import com.sao.saomenu.client.render.SaoWorldOverlays;
import com.sao.saomenu.client.input.SAOMenuMovement;
import com.sao.saomenu.client.menu.SaoPanels;
import com.sao.saomenu.client.party.SAOClientPartyState;
import com.sao.saomenu.client.skill.SAODualWield;
import com.sao.saomenu.client.skill.SaoSkillClientState;
import com.sao.saomenu.client.skill.SaoSkillRegistry;
import com.sao.saomenu.client.screen.settings.SettingsCatalog;
import com.sao.saomenu.config.SAOConfig;
import com.sao.saomenu.network.ClientboundPartyMessages;
import com.sao.saomenu.ui.theme.SaoThemeLibrary;
import com.sao.saomenu.ui.theme.SaoTheme;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import java.nio.file.Path;
import java.util.List;

/** Owns client startup and session transitions; renderers do not orchestrate other subsystems. */
public final class SaoClientRuntime {
    private static boolean initialized;
    private static boolean inWorld;
    private static ClientLevel activeLevel;

    private SaoClientRuntime() {
    }

    /** Called on the client thread after platform registration. */
    public static void initialize() {
        if (initialized) {
            return;
        }
        Path configDirectory = Minecraft.getInstance().gameDirectory.toPath().resolve("config");
        SAOConfig.load(configDirectory.resolve("saomenu.json"));
        UiRegistries.begin();
        UiRegistries registry = UiRegistries.instance();
        SaoPanels.registerBuiltins(registry);
        SAOHud.registerBuiltins(registry);
        SettingsCatalog.registerBuiltins(registry);
        SaoTheme.registerBuiltins(registry);
        SaoWorldOverlays.registerBuiltins(registry);
        SAOClientPlatform.registerUi(registry);
        UiRegistries.freeze();
        SaoTheme.installRegistered(registry.themes());
        SaoThemeLibrary.load(configDirectory.resolve("saomenu"));
        SaoSkillRegistry.registerBuiltins();
        ClientboundPartyMessages.install(new ClientboundPartyMessages.Receiver() {
            @Override
            public void invite(String inviter) {
                SAOClientPartyState.onInviteReceived(inviter);
            }

            @Override
            public void team(String title, List<String> members) {
                SAOClientPartyState.onTeamSync(title, members);
            }
        });
        ClientTickEvent.CLIENT_POST.register(SaoClientRuntime::tick);
        initialized = true;
    }

    private static void tick(Minecraft client) {
        boolean available = client.player != null && client.level != null;
        if (client.level != activeLevel) {
            for (SessionListener listener : UiRegistries.instance().sessionListeners()) {
                listener.levelChanged(activeLevel, client.level);
            }
        }
        if (!available && inWorld) {
            leaveWorld(client);
        } else if (available && !inWorld) {
            resetWorldVisuals();
            if (SaoUi.enabled()) {
                SAOWelcome.scheduleStart();
            }
        } else if (available && client.level != activeLevel) {
            // Entity IDs and render resources belong to one level, not an entire connection.
            resetWorldVisuals();
        }
        inWorld = available;
        activeLevel = client.level;

        if (SaoUi.enabled()) {
            SAOWelcome.clientTick(client);
            SAOFreeLook.tick(client);
            SAODeathEffect.clientTick(client);
        }
        SAODualWield.tick();
        SAOHud.clientTick(client);
        SAOKeybinds.tick(client);
    }

    private static void resetWorldVisuals() {
        SAODeathEffect.reset();
        SAOBossBanner.reset();
        SAOMapPanel.reset();
        SaoPanels.resetSession();
    }

    /** Cancels only transient UI state; party, skill and server-owned gameplay state survive. */
    public static void resetUi(Minecraft client) {
        resetWorldVisuals();
        SAOHud.resetSession();
        SAOWelcome.reset();
        SAOFreeLook.reset(client);
        SAOMenuMovement.reset(client);
    }

    private static void leaveWorld(Minecraft client) {
        resetWorldVisuals();
        SAOHud.resetSession();
        SAOClientPartyState.reset();
        SaoSkillClientState.reset();
        SAODualWield.reset();
        SAOWelcome.reset();
        SAOFreeLook.reset(client);
        SAOKeybinds.reset();
        SAOMenuMovement.reset(client);
    }
}
