package com.gdvn;

import com.gdvn.api.PlayerData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DisplayNameManager {
    private static final Map<UUID, Component> DISPLAY_NAMES = new ConcurrentHashMap<>();
    private static final Map<UUID, NameTagEntity> NAME_TAGS = new ConcurrentHashMap<>();
    private static final int DEFAULT_COLOR = 0xFFFFFF;

    public static Component getDisplayName(UUID uuid) {
        return DISPLAY_NAMES.get(uuid);
    }

    public static void removeDisplayName(UUID uuid) {
        DISPLAY_NAMES.remove(uuid);
    }

    public static void broadcastDisplayNameUpdate(ServerPlayer player, MinecraftServer server) {
        server.getPlayerList().broadcastAll(
                new ClientboundPlayerInfoUpdatePacket(
                        EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME),
                        List.of(player)
                )
        );
    }

    public static Component getTabListDisplayName(UUID uuid, String originalName) {
        Component customName = DISPLAY_NAMES.get(uuid);
        if (customName == null) return null;

        MutableComponent tabName = Component.empty();
        tabName.append(customName);
        tabName.append(Component.literal(" " + originalName)
                .withStyle(Style.EMPTY
                        .withColor(TextColor.fromRgb(0x555555))
                        .withItalic(true)));
        return tabName;
    }

    public static void onPlayerJoin(ServerPlayer player, MinecraftServer server) {
        for (Map.Entry<UUID, NameTagEntity> entry : NAME_TAGS.entrySet()) {
            if (!entry.getKey().equals(player.getUUID())) {
                entry.getValue().spawn(player);
            }
        }
    }

    public static void onPlayerDisconnect(ServerPlayer player, MinecraftServer server) {
        NameTagEntity nameTag = NAME_TAGS.remove(player.getUUID());
        if (nameTag != null) {
            for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
                if (!viewer.getUUID().equals(player.getUUID())) {
                    nameTag.despawn(viewer);
                }
            }
        }
    }

    public static void updateDisplayName(UUID uuid, PlayerData data) {
        MutableComponent component = Component.empty();

        if (data.clans != null && data.clan != null) {
            boolean isBoosted = data.clans.boostedUntil != null && !isExpired(data.clans.boostedUntil);

            if (isBoosted) {
                int bracketColor = parseHexColor(data.clans.tagBgColor);
                int tagColor = parseHexColor(data.clans.tagTextColor);

                component.append(Component.literal("[")
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(bracketColor))));
                component.append(Component.literal(data.clans.tag)
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(tagColor))));
                component.append(Component.literal("] ")
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(bracketColor))));
            } else {
                component.append(Component.literal("[" + data.clans.tag + "] ")
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA))));
            }
        }

        if (data.supporterUntil != null && !isExpired(data.supporterUntil)) {
            component.append(Component.literal(data.name))
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFAA00)));
        } else {
            component.append(Component.literal(data.name));
        }

        DISPLAY_NAMES.put(uuid, component);
    }

    private static boolean isExpired(String dateStr) {
        try {
            OffsetDateTime dateTime = OffsetDateTime.parse(dateStr);
            return dateTime.toInstant().isBefore(Instant.now());
        } catch (Exception e) {
            return true;
        }
    }

    private static int parseHexColor(String hex) {
        if (hex == null || hex.isEmpty()) {
            return DEFAULT_COLOR;
        }
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return DEFAULT_COLOR;
        }
    }
}
