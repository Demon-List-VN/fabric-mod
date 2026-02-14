package com.gdvn.command;

import com.gdvn.DisplayNameManager;
import com.gdvn.Gdvn;
import com.gdvn.api.GdvnApi;
import com.gdvn.api.PlayerData;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GdvnCommand {
    private static final Map<UUID, String> PENDING_OTP = new ConcurrentHashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("gdvn")
                .then(Commands.literal("link").executes(ctx -> link(ctx.getSource())))
                .then(Commands.literal("confirm").executes(ctx -> confirm(ctx.getSource())))
                .then(Commands.literal("unlink").executes(ctx -> unlink(ctx.getSource())))
                .then(Commands.literal("account").executes(ctx -> account(ctx.getSource())))
                .then(Commands.literal("help").executes(ctx -> help(ctx.getSource())))
                .executes(ctx -> help(ctx.getSource()))
        );
    }

    private static int link(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }

        String uuid = player.getStringUUID();

        try {
            String existingToken = Gdvn.getDatabase().getToken(uuid);
            if (existingToken != null) {
                source.sendFailure(Component.literal("You are already linked! Use /gdvn unlink first."));
                return 0;
            }
        } catch (Exception e) {
            source.sendFailure(Component.literal("Database error: " + e.getMessage()));
            return 0;
        }

        source.sendSystemMessage(Component.literal("Creating OTP code..."));

        MinecraftServer server = source.getServer();
        Gdvn.runAsync(() -> {
            try {
                JsonObject response = GdvnApi.createOtp();
                String code = response.get("code").getAsString();
                PENDING_OTP.put(player.getUUID(), code);

                server.execute(() -> {
                    MutableComponent message = Component.empty();

                    message.append(Component.literal("Click here to login!")
                            .withStyle(Style.EMPTY
                                    .withColor(TextColor.fromRgb(0x55FFFF))
                                    .withUnderlined(true)
                                    .withClickEvent(new ClickEvent.OpenUrl(
                                            URI.create("https://www.gdvn.net/auth/otp/" + code)))));

                    message.append(Component.literal(" Your OTP code is "));
                    message.append(Component.literal(code)
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF55))));
                    message.append(Component.literal(". Use "));
                    message.append(Component.literal("/gdvn confirm")
                            .withStyle(Style.EMPTY
                                    .withColor(TextColor.fromRgb(0x55FF55))
                                    .withClickEvent(new ClickEvent.SuggestCommand(
                                            "/gdvn confirm"))));
                    message.append(Component.literal(" to confirm linking."));

                    source.sendSystemMessage(message);
                });
            } catch (Exception e) {
                server.execute(() ->
                        source.sendFailure(Component.literal("Failed to create OTP: " + e.getMessage()))
                );
            }
        });

        return 1;
    }

    private static int confirm(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }

        String code = PENDING_OTP.get(player.getUUID());
        if (code == null) {
            source.sendFailure(Component.literal("No pending OTP code. Use /gdvn link first."));
            return 0;
        }

        source.sendSystemMessage(Component.literal("Verifying OTP code..."));

        MinecraftServer server = source.getServer();
        Gdvn.runAsync(() -> {
            try {
                JsonObject response = GdvnApi.verifyOtp(code);
                boolean granted = response.get("granted").getAsBoolean();

                if (!granted) {
                    server.execute(() ->
                            source.sendFailure(Component.literal(
                                    "OTP not yet granted. Please login via the link first."))
                    );
                    return;
                }

                String key = response.get("key").getAsString();
                String playerName = response.get("player").getAsString();
                PENDING_OTP.remove(player.getUUID());

                Gdvn.getDatabase().saveToken(player.getStringUUID(), key);

                PlayerData data = GdvnApi.getPlayerInfo(key);
                DisplayNameManager.updateDisplayName(player.getUUID(), data);

                server.execute(() -> {
                    source.sendSystemMessage(Component.literal("Successfully linked to account: ")
                            .append(Component.literal(playerName)
                                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FF55)))));
                    DisplayNameManager.broadcastDisplayNameUpdate(player, server);
                });
            } catch (Exception e) {
                server.execute(() ->
                        source.sendFailure(Component.literal("Failed to verify OTP: " + e.getMessage()))
                );
            }
        });

        return 1;
    }

    private static int unlink(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }

        String uuid = player.getStringUUID();

        MinecraftServer server = source.getServer();
        Gdvn.runAsync(() -> {
            try {
                String token = Gdvn.getDatabase().getToken(uuid);
                if (token == null) {
                    server.execute(() ->
                            source.sendFailure(Component.literal("You are not linked to any account."))
                    );
                    return;
                }

                try {
                    GdvnApi.deleteApiKey(token);
                } catch (Exception ignored) {
                }

                Gdvn.getDatabase().removeToken(uuid);
                DisplayNameManager.removeDisplayName(player.getUUID());

                server.execute(() -> {
                    source.sendSystemMessage(Component.literal("Successfully unlinked your account."));
                    DisplayNameManager.broadcastDisplayNameUpdate(player, server);
                });
            } catch (Exception e) {
                server.execute(() ->
                        source.sendFailure(Component.literal("Failed to unlink: " + e.getMessage()))
                );
            }
        });

        return 1;
    }

    private static int account(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }

        String uuid = player.getStringUUID();

        MinecraftServer server = source.getServer();
        Gdvn.runAsync(() -> {
            try {
                String token = Gdvn.getDatabase().getToken(uuid);
                if (token == null) {
                    server.execute(() ->
                            source.sendFailure(Component.literal("You are not linked to any account."))
                    );
                    return;
                }

                PlayerData data = GdvnApi.getPlayerInfo(token);

                server.execute(() -> {
                    source.sendSystemMessage(Component.literal("=== GDVN Account Info ===")
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF))));
                    source.sendSystemMessage(Component.literal("Name: ")
                            .append(Component.literal(data.name)
                                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF55)))));

                    if (data.clans != null && data.clan != null) {
                        source.sendSystemMessage(Component.literal("Clan: ")
                                .append(Component.literal("[" + data.clans.tag + "]")
                                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FF55)))));
                    }

                    if (data.supporterUntil != null) {
                        source.sendSystemMessage(Component.literal("Supporter until: ")
                                .append(Component.literal(data.supporterUntil)
                                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFAA00)))));
                    }

                    source.sendSystemMessage(Component.literal("========================")
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF))));
                });
            } catch (Exception e) {
                server.execute(() -> {
                    try {
                        Gdvn.getDatabase().removeToken(uuid);
                        DisplayNameManager.removeDisplayName(player.getUUID());
                        DisplayNameManager.broadcastDisplayNameUpdate(player, server);
                    } catch (Exception ignored) {
                    }
                    source.sendFailure(Component.literal(
                            "GDVN token expired. Please relink your account."));
                });
            }
        });

        return 1;
    }

    private static int help(CommandSourceStack source) {
        source.sendSystemMessage(Component.literal("=== GDVN Commands ===")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF))));
        source.sendSystemMessage(Component.literal("/gdvn link")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FF55)))
                .append(Component.literal(" - Link your GDVN account")
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF)))));
        source.sendSystemMessage(Component.literal("/gdvn confirm")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FF55)))
                .append(Component.literal(" - Confirm account linking")
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF)))));
        source.sendSystemMessage(Component.literal("/gdvn unlink")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FF55)))
                .append(Component.literal(" - Unlink your GDVN account")
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF)))));
        source.sendSystemMessage(Component.literal("/gdvn account")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FF55)))
                .append(Component.literal(" - Show linked account info")
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF)))));
        source.sendSystemMessage(Component.literal("/gdvn help")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FF55)))
                .append(Component.literal(" - Show this help message")
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF)))));
        source.sendSystemMessage(Component.literal("====================")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF))));
        return 1;
    }
}
