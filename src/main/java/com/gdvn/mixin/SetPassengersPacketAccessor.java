package com.gdvn.mixin;

import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundSetPassengersPacket.class)
public interface SetPassengersPacketAccessor {

    @Accessor("vehicle")
    void setVehicle(int vehicle);

    @Mutable
    @Accessor("passengers")
    void setPassengers(int[] passengers);
}
