package com.gdvn;

import com.gdvn.api.GdvnApi;
import com.gdvn.api.PlayerData;
import com.gdvn.command.GdvnCommand;
import com.gdvn.db.DatabaseManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Gdvn implements ModInitializer {
	public static final String MOD_ID = "gdvn";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static DatabaseManager database;
	private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
		Thread t = new Thread(r, "gdvn-async");
		t.setDaemon(true);
		return t;
	});

	@Override
	public void onInitialize() {
		Path dbPath = Path.of("gdvn.db");
		database = new DatabaseManager(dbPath);
		try {
			database.init();
			LOGGER.info("GDVN database initialized");
		} catch (Exception e) {
			LOGGER.error("Failed to initialize GDVN database", e);
		}

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				GdvnCommand.register(dispatcher)
		);

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.player;
			String uuid = player.getStringUUID();

			// Show existing name tags to the joining player
			DisplayNameManager.onPlayerJoin(player, server);

			runAsync(() -> {
				try {
					String token = database.getToken(uuid);
					if (token == null) return;

					PlayerData data = GdvnApi.getPlayerInfo(token);
					DisplayNameManager.updateDisplayName(player.getUUID(), data);

					server.execute(() -> {
						DisplayNameManager.applyCustomNameTag(player, server);
						DisplayNameManager.broadcastDisplayNameUpdate(player, server);
					});
				} catch (Exception e) {
					LOGGER.warn("Failed to validate token for {}: {}", player.getName().getString(), e.getMessage());
					try {
						database.removeToken(uuid);
						DisplayNameManager.removeDisplayName(player.getUUID());
					} catch (Exception ignored) {
					}
					server.execute(() -> {
						player.sendSystemMessage(Component.literal(
								"GDVN token expired. Please relink your account."));
						DisplayNameManager.removeCustomNameTag(player, server);
						DisplayNameManager.broadcastDisplayNameUpdate(player, server);
					});
				}
			});
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayer player = handler.player;
			DisplayNameManager.onPlayerDisconnect(player, server);
		});

		ServerTickEvents.END_SERVER_TICK.register(DisplayNameManager::tick);

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> EXECUTOR.shutdownNow());

		LOGGER.info("GDVN mod initialized");
	}

	public static DatabaseManager getDatabase() {
		return database;
	}

	public static void runAsync(Runnable task) {
		EXECUTOR.submit(task);
	}
}