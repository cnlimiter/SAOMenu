package com.sao.saomenu.api.world;

import com.sao.saomenu.api.UiContribution;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.Objects;

/**
 * Client world-space overlay drawn in the same AFTER_ENTITIES pass as builtin target shells
 * and the third-person menu board.
 *
 * <h2>When this runs</h2>
 * Forge {@code RenderLevelStageEvent.Stage.AFTER_ENTITIES}, after entity batches have been
 * flushed so depth already contains living geometry, and before weather/particles. The GUI
 * is hidden ({@code hideGui}) skips the whole pass. Name/HP text is <em>not</em> this
 * callback: that stays the existing target-label HUD bridge.
 *
 * <h2>Camera coordinates</h2>
 * {@code pose} is the level PoseStack with camera <em>rotation</em> already applied and
 * camera <em>translation</em> not applied. Place world geometry with
 * {@code pose.translate(worldX - camera.getPosition().x, ...)} then draw in that frame.
 * {@code camera} is the active level camera for this stage (same instance as
 * {@code GameRenderer.getMainCamera()}). {@code projection} is the stage projection matrix.
 * {@code partialTick} matches the stage event.
 *
 * <h2>State</h2>
 * Do not retain {@code pose}, {@code camera}, {@code projection}, or vertex consumers past
 * return. Balance every extra {@code pushPose} with {@code popPose}. Restore any
 * {@code RenderSystem} / GL state you change. The pipeline also push/pops once around
 * each overlay so a leak cannot shift later overlays' camera frame, but it cannot repair
 * leftover GL state. Overlays share the world {@code MultiBufferSource}; the hook flushes
 * it after the frozen list finishes.
 */
public interface WorldOverlay extends UiContribution {
    void render(PoseStack pose, Camera camera, Matrix4f projection, float partialTick);

    static WorldOverlay of(ResourceLocation id, int order, Renderer renderer) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(renderer, "renderer");
        return new WorldOverlay() {
            @Override
            public ResourceLocation id() {
                return id;
            }

            @Override
            public int order() {
                return order;
            }

            @Override
            public void render(PoseStack pose, Camera camera, Matrix4f projection, float partialTick) {
                renderer.render(pose, camera, projection, partialTick);
            }
        };
    }

    @FunctionalInterface
    interface Renderer {
        void render(PoseStack pose, Camera camera, Matrix4f projection, float partialTick);
    }
}
