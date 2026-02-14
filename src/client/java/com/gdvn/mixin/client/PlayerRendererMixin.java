package com.gdvn.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.WeakHashMap;

@Mixin(EntityRenderer.class)
public abstract class PlayerRendererMixin<T extends Entity, S extends EntityRenderState> {

    @Unique
    private static final WeakHashMap<EntityRenderState, Component> gdvn$customTags = new WeakHashMap<>();

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",
            at = @At("TAIL"))
    private void gdvn$captureCustomDisplayName(T entity, S state, float partialTick, CallbackInfo ci) {
        if (!(entity instanceof Player player)) return;
        if (Minecraft.getInstance().getConnection() == null) return;

        PlayerInfo playerInfo = Minecraft.getInstance().getConnection().getPlayerInfo(player.getUUID());
        if (playerInfo == null) return;

        Component tabDisplayName = playerInfo.getTabListDisplayName();
        if (tabDisplayName == null) return;

        if (tabDisplayName.getString().equals(player.getGameProfile().getName())) return;

        gdvn$customTags.put(state, tabDisplayName);
    }

    @Inject(method = "renderNameTag(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"))
    private void gdvn$renderCustomTag(S state, Component displayName, PoseStack poseStack,
                                       MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        Component customTag = gdvn$customTags.get(state);
        if (customTag == null) return;

        Font font = Minecraft.getInstance().font;
        Matrix4f matrix = poseStack.last().pose();
        float bgOpacity = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
        int bgColor = (int) (bgOpacity * 255.0F) << 24;

        float textWidth = (float) (-font.width(customTag) / 2);
        int nameY = "deadmau5".equals(displayName.getString()) ? -10 : 0;
        float yOffset = nameY - font.lineHeight - 1;

        boolean seeThrough = !state.isDiscrete;

        font.drawInBatch(customTag, textWidth, yOffset, seeThrough ? 0x20FFFFFF : -1, false, matrix, bufferSource,
                seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, bgColor, packedLight);
        if (seeThrough) {
            font.drawInBatch(customTag, textWidth, yOffset, -1, false, matrix, bufferSource,
                    Font.DisplayMode.NORMAL, 0, packedLight);
        }
    }
}
