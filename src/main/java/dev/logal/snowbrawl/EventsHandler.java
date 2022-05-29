/*
 * Copyright 2022 Logan Fick
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at: https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package dev.logal.snowbrawl;

import dev.logal.snowbrawl.managers.MatchManager;
import dev.logal.snowbrawl.types.Match;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Handles leftover events which do not really fit within the match or arena manager.
 */
public final class EventsHandler implements Listener {
	final MatchManager matchManager;

	/**
	 * Creates a new event handler which is owned by a given instance of Snowbrawl.
	 *
	 * @param snowbrawl The instance of Snowbrawl which owns this event handler. Used for querying the match manager for information.
	 */
	public EventsHandler(final Snowbrawl snowbrawl){
		this.matchManager = snowbrawl.getMatchManager();
	}

	/**
	 * Sets the default state of players who join the server.
	 *
	 * @param event The event.
	 */
	@EventHandler
	public void onPlayerJoin(final PlayerJoinEvent event){
		Player player = event.getPlayer();

		player.setGameMode(GameMode.SPECTATOR);
		player.teleport(player.getWorld().getSpawnLocation());

		// Reset player stats to known normal values.
		player.setHealth(20);
		player.setLevel(0);
		player.setTotalExperience(0);
		player.getInventory().clear();
		player.updateInventory();

		// Decorate the player list a little.
		player.setPlayerListHeader(ChatColor.WHITE + "" + ChatColor.BOLD + "Snow" + ChatColor.AQUA + "" + ChatColor.BOLD + "Brawl " + ChatColor.BLUE + Bukkit.getServer().getBukkitVersion().split("-")[0]);
	}

	/**
	 * Rewrites the format of chat to convey information about players in matches.
	 *
	 * @param event The event.
	 */
	@EventHandler
	public void onAsyncPlayerChat(final AsyncPlayerChatEvent event){
		final Player player = event.getPlayer();
		Match match = this.matchManager.getMatchFromPlayer(player);
		if (player.isOp()){
			if (match != null){
				event.setFormat(ChatColor.BLUE + "[" + match.getArena().getName() + "] " + ChatColor.RED + "%s" + ChatColor.WHITE + ": %s");
			} else if (this.matchManager.isInQueue(player)){
				event.setFormat(ChatColor.AQUA + "[Queued] " + ChatColor.WHITE + ChatColor.RED + "%s" + ChatColor.WHITE + ": %s");
			} else {
				event.setFormat(ChatColor.RED + "%s" + ChatColor.WHITE + ": %s");
			}
		} else {
			if (match != null){
				event.setFormat(ChatColor.BLUE + "[" + match.getArena().getName() + "] " + ChatColor.WHITE + "%s: %s");
			} else if (this.matchManager.isInQueue(player)){
				event.setFormat(ChatColor.AQUA + "[Queued] " + ChatColor.WHITE + "%s: %s");
			} else {
				event.setFormat(ChatColor.WHITE + "%s: %s");
			}
		}

	}
}
