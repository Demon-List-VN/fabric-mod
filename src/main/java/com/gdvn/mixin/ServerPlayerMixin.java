package com.gdvn.mixin;

import com.gdvn.DisplayNameManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(method = "getTabListDisplayName", at = @At("HEAD"), cancellable = true)
    private void gdvn$getTabListDisplayName(CallbackInfoReturnable<Component> cir) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        Component customName = DisplayNameManager.getDisplayName(self.getUUID());
        if (customName != null) {
            cir.setReturnValue(customName);
        }
    }

    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void gdvn$getDisplayName(CallbackInfoReturnable<Component> cir) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        Component customName = DisplayNameManager.getDisplayName(self.getUUID());
        if (customName != null) {
            cir.setReturnValue(customName);
        }
    }
}
