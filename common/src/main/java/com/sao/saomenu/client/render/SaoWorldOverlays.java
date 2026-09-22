package com.sao.saomenu.client.render;

import com.sao.saomenu.SAOMenu;
import com.sao.saomenu.api.SaoUi;
import com.sao.saomenu.api.SaoUiRegistry;
import com.sao.saomenu.api.lifecycle.SessionListener;
import com.sao.saomenu.api.world.WorldOverlay;
import com.sao.saomenu.client.render.target.SAOTargetBar3D;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Builtin world overlays and the AFTER_ENTITIES dispatch loop.
 *
 * <p>Call {@link #registerBuiltins} from client setup before freeze. The Forge world hook
 * calls {@link #render}; it does not register overlays or own level lifecycle. Target
 * name/HP labels stay on the HUD bridge ({@code SAOTargetBar3D.renderLabels}).</p>
 */
public final class SaoWorldOverlays {
    public static final ResourceLocation TARGET_ID = new ResourceLocation(SAOMenu.MOD_ID, "target");
    public static final ResourceLocation WORLD_MENU_ID = new ResourceLocation(SAOMenu.MOD_ID, "world_menu");
    public static final ResourceLocation SESSION_ID = new ResourceLocation(SAOMenu.MOD_ID, "world_overlays");

    private SaoWorldOverlays() {
    }

    public static void registerBuiltins(SaoUiRegistry registry) {
        registry.world(WorldOverlay.of(TARGET_ID, 100, SaoWorldOverlays::renderTarget));
        registry.world(WorldOverlay.of(WORLD_MENU_ID, 200, SaoWorldOverlays::renderMenuBoard));
        registry.session(SessionListener.of(SESSION_ID, 100, SaoWorldOverlays::onLevelChanged));
    }

    /**
     * Dispatch frozen overlays in registry order. Protects the camera pose around each
     * callback; does not flush buffers or hide-GUI (the Forge hook owns those).
     */
    public static void render(PoseStack pose, Camera camera, Matrix4f projection, float partialTick) {
        if (!SaoUi.enabled()) {
            return;
        }
        for (WorldOverlay overlay : SaoUi.worldOverlays()) {
            pose.pushPose();
            try {
                overlay.render(pose, camera, projection, partialTick);
            } finally {
                pose.popPose();
            }
        }
    }

    private static void renderTarget(PoseStack pose, Camera camera, Matrix4f projection, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        SAOTargetBar3D.render(mc, pose, mc.renderBuffers().bufferSource(), partialTick);
    }

    private static void renderMenuBoard(PoseStack pose, Camera camera, Matrix4f projection, float partialTick) {
        SAOMenu3DPanel.renderBoard(Minecraft.getInstance(), pose, partialTick);
    }

    private static void onLevelChanged(ClientLevel previous, ClientLevel current) {
        SAOTargetBar3D.reset();
        SAOMenu3DPanel.reset();
    }
}
