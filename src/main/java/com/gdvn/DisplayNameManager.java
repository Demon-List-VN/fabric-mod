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
    private static final Map<UUID, Component> CUSTOM_NAME_TAGS = new ConcurrentHashMap<>();
    private static final int DEFAULT_COLOR = 0xFFFFFF;

    public static Component getDisplayName(UUID uuid) {
        return DISPLAY_NAMES.get(uuid);
    }

    public static Component getCustomNameTag(UUID uuid) {
        return CUSTOM_NAME_TAGS.get(uuid);
    }

    public static void removeDisplayName(UUID uuid) {
        DISPLAY_NAMES.remove(uuid);
        CUSTOM_NAME_TAGS.remove(uuid);
    }

    public static void applyCustomNameTag(ServerPlayer player) {
        Component customNameTag = CUSTOM_NAME_TAGS.get(player.getUUID());
        if (customNameTag != null) {
            player.setCustomName(customNameTag);
            player.setCustomNameVisible(true);
        } else {
            player.setCustomName(null);
            player.setCustomNameVisible(false);
        }
    }

    public static void broadcastDisplayNameUpdate(ServerPlayer player, MinecraftServer server) {
        applyCustomNameTag(player);
        server.getPlayerList().broadcastAll(
                new ClientboundPlayerInfoUpdatePacket(
                        EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME),
                        List.of(player)
                )
        );
    }

    public static void updateDisplayName(UUID uuid, PlayerData data, String originalName) {
        MutableComponent nameTag = Component.empty();
        MutableComponent tabName = Component.empty();

        if (data.clans != null && data.clan != null) {
            boolean isBoosted = data.clans.boostedUntil != null && !isExpired(data.clans.boostedUntil);

            if (isBoosted) {
                int bracketColor = parseHexColor(data.clans.tagBgColor);
                int tagColor = parseHexColor(data.clans.tagTextColor);

                MutableComponent clanPrefix = Component.empty();
                clanPrefix.append(Component.literal("[")
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(bracketColor))));
                clanPrefix.append(Component.literal(data.clans.tag)
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(tagColor))));
                clanPrefix.append(Component.literal("] ")
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(bracketColor))));

                nameTag.append(clanPrefix.copy());
                tabName.append(clanPrefix.copy());
            } else {
                Component clanPrefix = Component.literal("[" + data.clans.tag + "] ")
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA)));

                nameTag.append(clanPrefix.copy());
                tabName.append(clanPrefix.copy());
            }
        }

        if (data.supporterUntil != null && !isExpired(data.supporterUntil)) {
            nameTag.append(Component.literal(data.name)
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFAA00))));
            tabName.append(Component.literal(data.name)
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFAA00))));
        } else {
            nameTag.append(Component.literal(data.name));
            tabName.append(Component.literal(data.name));
        }

        // Append original Minecraft name at the end of tab list name with low opacity and italic
        tabName.append(Component.literal(" " + originalName)
                .withStyle(Style.EMPTY
                        .withColor(TextColor.fromRgb(0x555555))
                        .withItalic(true)));

        CUSTOM_NAME_TAGS.put(uuid, nameTag);
        DISPLAY_NAMES.put(uuid, tabName);
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
