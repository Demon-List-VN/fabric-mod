package com.gdvn;

import com.gdvn.api.PlayerData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DisplayNameManager {
    private static final Map<UUID, Component> DISPLAY_NAMES = new ConcurrentHashMap<>();

    public static Component getDisplayName(UUID uuid) {
        return DISPLAY_NAMES.get(uuid);
    }

    public static void removeDisplayName(UUID uuid) {
        DISPLAY_NAMES.remove(uuid);
    }

    public static void updateDisplayName(UUID uuid, PlayerData data) {
        MutableComponent component = Component.empty();

        if (data.supporterUntil != null && !isExpired(data.supporterUntil)) {
            component.append(Component.literal("SPT ")
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFAA00))));
        }

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

        component.append(Component.literal(data.name));

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
            return 0xFFFFFF;
        }
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }
}
