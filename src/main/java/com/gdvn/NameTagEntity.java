package com.gdvn;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;

import java.util.List;

public class NameTagEntity {
    private static final float NAMETAG_VERTICAL_OFFSET = 0.5F;
    private static final double POSITION_EPSILON = 1.0E-4;
    private final Display.TextDisplay textDisplay;
    private final ServerPlayer owner;

    public NameTagEntity(ServerPlayer owner, Component displayName) {
        this.owner = owner;
        ServerLevel level = owner.level();
        this.textDisplay = new Display.TextDisplay(EntityType.TEXT_DISPLAY, level);
        updatePosition();
        this.textDisplay.setText(displayName);
        this.textDisplay.setBillboardConstraints(Display.BillboardConstraints.CENTER);
        this.textDisplay.setViewRange(1.0f);
        this.textDisplay.setBackgroundColor(0);
    }

    public void spawn(ServerPlayer viewer) {
        updatePosition();
        viewer.connection.send(new ClientboundAddEntityPacket(
                textDisplay, 0, textDisplay.blockPosition()
        ));
        sendMetadata(viewer);
    }

    public void despawn(ServerPlayer viewer) {
        viewer.connection.send(new ClientboundRemoveEntitiesPacket(textDisplay.getId()));
    }

    private void sendMetadata(ServerPlayer viewer) {
        List<SynchedEntityData.DataValue<?>> dataValues = textDisplay.getEntityData().getNonDefaultValues();
        if (dataValues != null && !dataValues.isEmpty()) {
            viewer.connection.send(new ClientboundSetEntityDataPacket(textDisplay.getId(), dataValues));
        }
    }

    public boolean updatePosition() {
        double x = owner.getX();
        double y = owner.getY() + owner.getBbHeight() + NAMETAG_VERTICAL_OFFSET;
        double z = owner.getZ();
        if (Math.abs(textDisplay.getX() - x) < POSITION_EPSILON
                && Math.abs(textDisplay.getY() - y) < POSITION_EPSILON
                && Math.abs(textDisplay.getZ() - z) < POSITION_EPSILON) {
            return false;
        }
        textDisplay.setPos(x, y, z);
        return true;
    }

    public int getEntityId() {
        return textDisplay.getId();
    }

    public ServerPlayer getOwner() {
        return owner;
    }
}
