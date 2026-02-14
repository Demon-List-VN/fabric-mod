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
        String originalName = self.getGameProfile().name();
        Component tabName = DisplayNameManager.getTabListDisplayName(self.getUUID(), originalName);
        if (tabName != null) {
            cir.setReturnValue(tabName);
        }
    }
}
