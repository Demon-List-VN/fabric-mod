package com.gdvn.mixin;

import com.gdvn.DisplayNameManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void gdvn$getDisplayName(CallbackInfoReturnable<Component> cir) {
        if ((Object) this instanceof ServerPlayer serverPlayer) {
            Component customName = DisplayNameManager.getDisplayName(serverPlayer.getUUID());
            if (customName != null) {
                cir.setReturnValue(customName);
            }
        }
    }
}
