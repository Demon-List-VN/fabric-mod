package com.gdvn;

import com.gdvn.mixin.SetPassengersPacketAccessor;
import com.mojang.math.Transformation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public class NameTagEntity {
    private final Display.TextDisplay textDisplay;
    private final ServerPlayer owner;

    public NameTagEntity(ServerPlayer owner, Component displayName) {
        this.owner = owner;
        ServerLevel level = owner.level();
        this.textDisplay = new Display.TextDisplay(EntityType.TEXT_DISPLAY, level);
        this.textDisplay.setPos(owner.getX(), owner.getY(), owner.getZ());
        this.textDisplay.setText(displayName);
        this.textDisplay.setBillboardConstraints(Display.BillboardConstraints.CENTER);
        this.textDisplay.setViewRange(1.0f);
        this.textDisplay.setBackgroundColor(0);
    }

    public void spawn(ServerPlayer viewer) {
        viewer.connection.send(new ClientboundAddEntityPacket(
                textDisplay, 0, textDisplay.blockPosition()
        ));
        sendMetadata(viewer);
        sendPassengerPacket(viewer);
    }

    public void despawn(ServerPlayer viewer) {
        viewer.connection.send(new ClientboundRemoveEntitiesPacket(textDisplay.getId()));
        resetPassengers(viewer);
    }

    private void sendMetadata(ServerPlayer viewer) {
        List<SynchedEntityData.DataValue<?>> dataValues = textDisplay.getEntityData().getNonDefaultValues();
        if (dataValues != null && !dataValues.isEmpty()) {
            viewer.connection.send(new ClientboundSetEntityDataPacket(textDisplay.getId(), dataValues));
        }
    }

    private void sendPassengerPacket(ServerPlayer viewer) {
        // Create packet from the owner entity, then override passengers to include our text display
        ClientboundSetPassengersPacket packet = new ClientboundSetPassengersPacket(owner);
        int[] passengerIds = getPassengerIdsWithExtra(owner, textDisplay.getId());
        ((SetPassengersPacketAccessor) packet).setPassengers(passengerIds);
        viewer.connection.send(packet);
    }

    private void resetPassengers(ServerPlayer viewer) {
        // Send the actual passengers list (without our virtual entity)
        viewer.connection.send(new ClientboundSetPassengersPacket(owner));
    }

    private static int[] getPassengerIdsWithExtra(ServerPlayer owner, int extraPassengerId) {
        var passengers = owner.getPassengers();
        int[] ids = new int[passengers.size() + 1];
        for (int i = 0; i < passengers.size(); i++) {
            ids[i] = passengers.get(i).getId();
        }
        ids[passengers.size()] = extraPassengerId;
        return ids;
    }

    public int getEntityId() {
        return textDisplay.getId();
    }

    public ServerPlayer getOwner() {
        return owner;
    }
}
