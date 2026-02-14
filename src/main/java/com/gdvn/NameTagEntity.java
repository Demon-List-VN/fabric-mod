package com.gdvn;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.decoration.ArmorStand;

import java.util.*;

public class NameTagEntity {
    private static final double NAME_TAG_OFFSET_Y = 0.3;
    private static final double POSITION_THRESHOLD_SQ = 0.001;

    private final ArmorStand armorStand;
    private final ServerPlayer owner;
    private double lastX, lastY, lastZ;

    public NameTagEntity(ServerPlayer owner, Component displayName) {
        this.owner = owner;
        ServerLevel level = owner.serverLevel();
        this.armorStand = new ArmorStand(EntityType.ARMOR_STAND, level);
        this.armorStand.setInvisible(true);
        this.armorStand.setMarker(true);
        this.armorStand.setSmall(true);
        this.armorStand.setCustomName(displayName);
        this.armorStand.setCustomNameVisible(true);
        this.armorStand.setNoGravity(true);
        updateArmorStandPosition();
        this.lastX = armorStand.getX();
        this.lastY = armorStand.getY();
        this.lastZ = armorStand.getZ();
    }

    public void setDisplayName(Component displayName) {
        this.armorStand.setCustomName(displayName);
    }

    private void updateArmorStandPosition() {
        this.armorStand.setPos(
                owner.getX(),
                owner.getY() + owner.getBbHeight() + NAME_TAG_OFFSET_Y,
                owner.getZ()
        );
    }

    public void spawn(ServerPlayer viewer) {
        updateArmorStandPosition();
        viewer.connection.send(new ClientboundAddEntityPacket(armorStand, 0, armorStand.blockPosition()));
        sendMetadata(viewer);
    }

    public void despawn(ServerPlayer viewer) {
        viewer.connection.send(new ClientboundRemoveEntitiesPacket(armorStand.getId()));
    }

    public void updatePosition(ServerPlayer viewer) {
        viewer.connection.send(new ClientboundEntityPositionSyncPacket(
                armorStand.getId(),
                PositionMoveRotation.of(armorStand),
                false
        ));
    }

    public boolean hasPositionChanged() {
        updateArmorStandPosition();
        double dx = armorStand.getX() - lastX;
        double dy = armorStand.getY() - lastY;
        double dz = armorStand.getZ() - lastZ;
        if (dx * dx + dy * dy + dz * dz > POSITION_THRESHOLD_SQ) {
            lastX = armorStand.getX();
            lastY = armorStand.getY();
            lastZ = armorStand.getZ();
            return true;
        }
        return false;
    }

    private void sendMetadata(ServerPlayer viewer) {
        List<SynchedEntityData.DataValue<?>> dataValues = armorStand.getEntityData().getNonDefaultValues();
        if (dataValues != null && !dataValues.isEmpty()) {
            viewer.connection.send(new ClientboundSetEntityDataPacket(armorStand.getId(), dataValues));
        }
    }

    public ServerPlayer getOwner() {
        return owner;
    }
}
