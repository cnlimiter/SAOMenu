package com.sao.examples;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sao.saomenu.api.SaoUi;
import com.sao.saomenu.api.forge.SaoUiRegisterEvent;
import com.sao.saomenu.api.hud.HudBox;
import com.sao.saomenu.api.hud.HudElement;
import com.sao.saomenu.api.hud.HudLayoutBinding;
import com.sao.saomenu.api.hud.HudRenderContext;
import com.sao.saomenu.api.lifecycle.SessionListener;
import com.sao.saomenu.api.menu.MenuEntry;
import com.sao.saomenu.api.menu.MenuIcon;
import com.sao.saomenu.api.menu.SaoPanel;
import com.sao.saomenu.api.settings.ActionSetting;
import com.sao.saomenu.api.settings.ChoiceSetting;
import com.sao.saomenu.api.settings.NativeSetting;
import com.sao.saomenu.api.settings.SettingsGroup;
import com.sao.saomenu.api.settings.SliderSetting;
import com.sao.saomenu.api.settings.ToggleSetting;
import com.sao.saomenu.api.theme.ThemeColors;
import com.sao.saomenu.api.theme.ThemeDefinition;
import com.sao.saomenu.api.theme.ThemeTokens;
import com.sao.saomenu.api.world.WorldOverlay;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.List;

/** Compiles against the produced SAOMenu JAR, never its source tree or internal classes. */
@Mod.EventBusSubscriber(modid = ShowcaseMod.ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ShowcaseClient {
    public static final ResourceLocation CYAN = id("cyan");
    private static final ResourceLocation ICON = id("textures/gui/notebook.png");
    private static final Component NAME = Component.literal("Addon notebook");
    private static final Component CARD_TITLE = Component.literal("Independent addon");
    private static final Component CARD_DETAIL = Component.literal("Public API only");
    private static final Component BADGE_CAPTION = Component.literal("NOTEBOOK");
    private static HudBox badgeBox;
    private static BlockPos marker;

    private ShowcaseClient() {
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(ShowcaseMod.ID, path);
    }

    @SubscribeEvent
    public static void register(SaoUiRegisterEvent event) {
        var registry = event.registry();
        List<MenuEntry> entries = List.of(
                MenuEntry.action(id("edit"), Component.literal("Edit notebook"), ICON,
                        context -> context.openScreen(new ShowcaseScreen(context.screen()))),
                MenuEntry.action(id("cyan_theme"), Component.literal("Use cyan theme"), ICON,
                        context -> SaoUi.selectTheme(CYAN)),
                MenuEntry.action(id("settings"), Component.literal("Addon settings"), ICON,
                        context -> SaoUi.openSettings(context.screen())));
        registry.menu(SaoPanel.of(id("notebook"), 500, NAME, MenuIcon.of(ICON), () -> entries,
                (graphics, context, bounds, eased, alpha, mouseX, mouseY) -> {
                    var colors = SaoUi.theme().colors();
                    graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), colors.dialogSurface());
                    graphics.drawString(context.minecraft().font, CARD_TITLE, bounds.x() + 8, bounds.y() + 10,
                            colors.textOnSurface(), false);
                    graphics.drawString(context.minecraft().font, CARD_DETAIL, bounds.x() + 8, bounds.y() + 25,
                            colors.textMuted(), false);
                }));
        registry.hud(HudElement.builder(id("badge"), 1000, NAME, ShowcaseClient::renderBadge)
                .visible(context -> context.player() != null && ShowcaseConfig.BADGE.get())
                .layout(HudLayoutBinding.screenAnchor(60, ShowcaseClient::badgeBounds,
                        () -> ShowcaseConfig.X.get(), () -> ShowcaseConfig.Y.get(),
                        (x, y) -> {
                            ShowcaseConfig.X.set((double) x);
                            ShowcaseConfig.Y.set((double) y);
                        }, () -> ShowcaseConfig.SPEC.save()))
                .build());
        registry.settings(new SettingsGroup(id("notebook"), 500, NAME,
                Component.literal("Stored in the addon's own Forge config"), List.of(
                new ToggleSetting(id("badge"), Component.literal("Show notebook badge"),
                        ShowcaseConfig.BADGE::get, ShowcaseConfig.BADGE::set),
                new ToggleSetting(id("marker"), Component.literal("Show local world marker"),
                        ShowcaseConfig.MARKER::get, ShowcaseConfig.MARKER::set),
                new SliderSetting(id("opacity"), Component.literal("Badge opacity"),
                        () -> ShowcaseConfig.OPACITY.get().floatValue(),
                        value -> ShowcaseConfig.OPACITY.set((double) value), 0.2f, 1f,
                        SliderSetting.Format.PERCENT),
                new NativeSetting(id("label"), Component.literal("Notebook label"),
                        ShowcaseConfig.LABEL::get, ShowcaseConfig.LABEL::set, 64),
                new ChoiceSetting(id("anchor"), Component.literal("Badge anchor"), List.of(
                        new ChoiceSetting.Option(id("top_left"), Component.literal("Top left")),
                        new ChoiceSetting.Option(id("center"), Component.literal("Center")),
                        new ChoiceSetting.Option(id("bottom_right"), Component.literal("Bottom right"))),
                        ShowcaseClient::anchorChoice, ShowcaseClient::setAnchor),
                new ActionSetting(id("theme"), Component.literal("Cyan theme"),
                        Component.literal("Apply"), () -> SaoUi.selectTheme(CYAN))),
                () -> ShowcaseConfig.SPEC.save(), ShowcaseConfig::reset));
        ResourceLocation font = id("body");
        registry.theme(new ThemeDefinition(CYAN, 500, Component.literal("Notebook cyan"), 188f,
                new ThemeTokens(ThemeColors.sao(), font, font, 420, 220)));
        registry.world(WorldOverlay.of(id("marker"), 500, ShowcaseClient::renderMarker));
        registry.session(SessionListener.of(id("session"), 500, (previous, current) -> marker = null));
    }

    private static HudBox badgeBounds(Minecraft minecraft, int width, int height) {
        int boxWidth = Math.min(126, width);
        int boxHeight = Math.min(30, height);
        int x = (int) Math.round(ShowcaseConfig.X.get() * (width - boxWidth));
        int y = (int) Math.round(ShowcaseConfig.Y.get() * (height - boxHeight));
        if (badgeBox == null || badgeBox.x() != x || badgeBox.y() != y
                || badgeBox.width() != boxWidth || badgeBox.height() != boxHeight) {
            badgeBox = new HudBox(x, y, boxWidth, boxHeight);
        }
        return badgeBox;
    }

    private static void renderBadge(HudRenderContext context) {
        HudBox bounds = badgeBounds(context.minecraft(), context.width(), context.height());
        var graphics = context.graphics();
        var colors = SaoUi.theme().colors();
        int alpha = Math.round((float) (255 * ShowcaseConfig.OPACITY.get()) * context.fade());
        if (alpha <= 0) {
            return;
        }
        graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(),
                (alpha << 24) | (colors.dialogSurface() & 0xFFFFFF));
        graphics.fill(bounds.x(), bounds.y(), bounds.x() + 3, bounds.y() + bounds.height(),
                (alpha << 24) | (colors.accent() & 0xFFFFFF));
        graphics.enableScissor(bounds.x() + 7, bounds.y(), bounds.x() + bounds.width() - 3,
                bounds.y() + bounds.height());
        try {
            graphics.drawString(context.minecraft().font, BADGE_CAPTION, bounds.x() + 7, bounds.y() + 4,
                    (alpha << 24) | (colors.textMuted() & 0xFFFFFF), false);
            graphics.drawString(context.minecraft().font, ShowcaseConfig.LABEL.get(), bounds.x() + 7,
                    bounds.y() + 16, (alpha << 24) | (colors.textOnSurface() & 0xFFFFFF), false);
        } finally {
            graphics.disableScissor();
        }
    }

    private static ResourceLocation anchorChoice() {
        double x = ShowcaseConfig.X.get();
        double y = ShowcaseConfig.Y.get();
        if (Math.abs(x - y) > 0.0001) {
            return null;
        }
        if (Math.abs(x - 0.1) < 0.0001) return id("top_left");
        if (Math.abs(x - 0.5) < 0.0001) return id("center");
        if (Math.abs(x - 0.9) < 0.0001) return id("bottom_right");
        return null;
    }

    private static void setAnchor(ResourceLocation value) {
        double position = switch (value.getPath()) {
            case "top_left" -> 0.1;
            case "center" -> 0.5;
            case "bottom_right" -> 0.9;
            default -> throw new IllegalArgumentException("Unknown anchor: " + value);
        };
        ShowcaseConfig.X.set(position);
        ShowcaseConfig.Y.set(position);
    }

    private static void renderMarker(PoseStack pose, Camera camera, Matrix4f projection, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!ShowcaseConfig.MARKER.get() || minecraft.player == null) {
            return;
        }
        if (marker == null) {
            marker = minecraft.player.blockPosition().relative(minecraft.player.getDirection(), 3).above();
        }
        Vec3 eye = camera.getPosition();
        double x = marker.getX() - eye.x;
        double y = marker.getY() - eye.y;
        double z = marker.getZ() - eye.z;
        var vertices = minecraft.renderBuffers().bufferSource().getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(pose, vertices, x, y, z, x + 1, y + 1, z + 1,
                0.05f, 0.85f, 0.9f, 0.9f);
    }
}
