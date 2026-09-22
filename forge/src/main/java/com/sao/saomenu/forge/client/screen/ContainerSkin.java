package com.sao.saomenu.forge.client.screen;

import com.sao.saomenu.SAOMenu;
import com.sao.saomenu.client.screen.vanilla.VanillaUiPolicy;
import com.sao.saomenu.ui.render.SaoDraw;
import com.sao.saomenu.ui.theme.SaoTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Visual adapter for named vanilla container screens. Native menus, slots, widgets,
 * input and packets stay authoritative. Every path gates on {@link VanillaUiPolicy}.
 */
@Mod.EventBusSubscriber(modid = SAOMenu.MOD_ID, value = Dist.CLIENT)
public final class ContainerSkin {
    /** 1×1 fully transparent substitute for a RESKIN leaf's static base blit only. */
    public static final ResourceLocation BLANK =
            new ResourceLocation(SAOMenu.MOD_ID, "textures/gui/container_blank.png");
    /** White arrow on transparent pixels; native cooking width and UVs remain authoritative. */
    public static final ResourceLocation PROGRESS =
            new ResourceLocation(SAOMenu.MOD_ID, "textures/gui/container_progress.png");

    private ContainerSkin() {
    }

    public static boolean reskin(AbstractContainerScreen<?> screen) {
        return VanillaUiPolicy.mode(screen) == VanillaUiPolicy.Mode.RESKIN;
    }

    public static boolean decorate(AbstractContainerScreen<?> screen) {
        return VanillaUiPolicy.mode(screen) == VanillaUiPolicy.Mode.DECORATE;
    }

    /**
     * {@code @ModifyArg} on a leaf {@code renderBg} base {@code blit(ResourceLocation,IIIIII)}.
     * KEEP / unknown class / disabled policy returns {@code original} unchanged.
     */
    public static ResourceLocation blankIfReskin(ResourceLocation original, AbstractContainerScreen<?> screen) {
        return reskin(screen) ? BLANK : original;
    }

    /** Mixin {@code renderBg} HEAD: themed plate and live {@link Slot} frames under native functional blits. */
    public static void paintReskinPlate(AbstractContainerScreen<?> screen, GuiGraphics graphics) {
        if (!reskin(screen)) {
            return;
        }
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        int width = screen.getXSize();
        int height = screen.getYSize();
        var colors = SaoTheme.palette();
        SaoDraw.roundedRect(graphics, left + 2, top + 3, width, height, 4, colors.shadow());
        SaoDraw.roundedRect(graphics, left, top, width, height, 4, colors.dialogSurface());
        graphics.fill(left + 4, top, left + width - 4, top + 2, colors.accent());
        // Static guides replaced with the plate; progress, entities and recipe widgets remain native.
        if (screen instanceof InventoryScreen) {
            graphics.fill(left + 26, top + 8, left + 75, top + 78, colors.surfaceSlot());
            paintRecipeArrow(graphics, left + 135, top + 35, 9, 6, colors.textMuted());
        } else if (screen instanceof CraftingScreen) {
            paintRecipeArrow(graphics, left + 90, top + 42, 14, 7, colors.textMuted());
        } else if (screen instanceof AbstractFurnaceScreen<?>) {
            paintRecipeArrow(graphics, left + 79, top + 42, 16, 7, colors.textMuted());
        }
        paintSlotFrames(screen, graphics, left, top, colors.divider(), colors.surfaceSlot());
    }

    /**
     * Mixin {@code renderBg} RETURN: accent chrome outside the native plate.
     * Does not fill the interior, so progress, maps, books, widgets and slots stay native.
     */
    public static void paintDecorateChrome(AbstractContainerScreen<?> screen, GuiGraphics graphics) {
        if (!decorate(screen)) {
            return;
        }
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        int right = left + screen.getXSize();
        int bottom = top + screen.getYSize();
        int accent = SaoTheme.accent();
        graphics.fill(left - 3, top - 3, right + 3, top - 1, accent);
        graphics.fill(left - 3, bottom + 1, right + 3, bottom + 3, accent);
        graphics.fill(left - 3, top - 1, left - 1, bottom + 1, accent);
        graphics.fill(right + 1, top - 1, right + 3, bottom + 1, accent);
    }

    /**
     * Forge {@link ContainerScreenEvent.Render.Foreground}: after labels, pose already translated
     * to {@code getGuiLeft/getGuiTop}. Corner ticks only; tooltips and the dragged stack stay above.
     */
    @SubscribeEvent
    public static void renderForeground(ContainerScreenEvent.Render.Foreground event) {
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        if (!VanillaUiPolicy.active(screen)) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int width = screen.getXSize();
        int height = screen.getYSize();
        int accent = SaoTheme.accent();
        int tick = 8;
        graphics.fill(0, 0, tick, 2, accent);
        graphics.fill(0, 0, 2, tick, accent);
        graphics.fill(width - tick, 0, width, 2, accent);
        graphics.fill(width - 2, 0, width, tick, accent);
        graphics.fill(0, height - 2, tick, height, accent);
        graphics.fill(0, height - tick, 2, height, accent);
        graphics.fill(width - tick, height - 2, width, height, accent);
        graphics.fill(width - 2, height - tick, width, height, accent);
    }

    private static void paintSlotFrames(AbstractContainerScreen<?> screen, GuiGraphics graphics,
                                        int left, int top, int frame, int fill) {
        for (Slot slot : screen.getMenu().slots) {
            if (!slot.isActive()) {
                continue;
            }
            int x = left + slot.x - 1;
            int y = top + slot.y - 1;
            graphics.fill(x, y, x + 18, y + 18, frame);
            graphics.fill(x + 1, y + 1, x + 17, y + 17, fill);
        }
    }

    private static void paintRecipeArrow(GuiGraphics graphics, int x, int centerY,
                                          int shaftWidth, int halfHeight, int color) {
        int headX = x + shaftWidth;
        graphics.fill(x, centerY - 1, headX, centerY + 2, color);
        for (int dy = -halfHeight; dy <= halfHeight; dy++) {
            int width = halfHeight + 1 - Math.abs(dy);
            graphics.fill(headX, centerY + dy, headX + width, centerY + dy + 1, color);
        }
    }

}
