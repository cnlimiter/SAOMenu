package com.sao.saomenu.forge.client.render;

import com.sao.saomenu.SAOMenu;
import com.sao.saomenu.api.SaoUi;
import com.sao.saomenu.client.render.SaoWorldOverlays;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge 世界渲染钩子:AFTER_ENTITIES 叠加冻结的 {@link SaoWorldOverlays} 列表。
 *
 * <p>选 AFTER_ENTITIES 而非 AFTER_LEVEL,是为了让血条早于天气/粒子叠加,
 * 同时此时 PoseStack 仍是纯相机旋转矩阵(未叠加额外变换)。</p>
 */
@Mod.EventBusSubscriber(modid = SAOMenu.MOD_ID, value = Dist.CLIENT)
public final class SAOMenuForgeWorldRender {

    private SAOMenuForgeWorldRender() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!SaoUi.enabled() || mc.options.hideGui) {
            return;
        }
        // AFTER_ENTITIES 触发时实体几何只是写进了缓冲、尚未上屏:
        // 必须先把实体批全部刷出,深度缓冲里才有实体。
        // 否则环带先画、实体后画,整只生物会盖在环带和血量带上(穿透/看不见)
        mc.renderBuffers().bufferSource().endBatch();
        SaoWorldOverlays.render(event.getPoseStack(), event.getCamera(),
                event.getProjectionMatrix(), event.getPartialTick());
        mc.renderBuffers().bufferSource().endBatch();
    }
}
