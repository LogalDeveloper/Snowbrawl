/*
 * Copyright 2022 Logan Fick
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at: https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package dev.logal.snowbrawl.types;

import dev.logal.snowbrawl.Snowbrawl;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents the state of a match taking place in an arena.
 */
public class Match {
	private final Snowbrawl snowbrawl;

	private final Arena arena;
	private final List<MatchParticipant> participants;
	private int graceTimeTicksRemaining; // The amount of grace time remaining in ticks.
	private int matchTimeTicksRemaining; // The amount of match time remaining in ticks.

	/**
	 * Creates a new match which is owned by a given instance of Snowbrawl and uses a given arena with a given list of players.
	 *
	 * @param snowbrawl The instance of Snowbrawl which owns this match manager. Used for setting boss bars, logging, and scheduling delayed tasks.
	 * @param arena The arena this match is taking place in.
	 * @param players A List of Players who are participating in this match.
	 */
	public Match(final Snowbrawl snowbrawl, final Arena arena, final List<Player> players){
		this.snowbrawl = snowbrawl;
		this.arena = arena;

		this.participants = new ArrayList<>();
		for (final Player player : players){
			participants.add(new MatchParticipant(player, this));
		}

		this.graceTimeTicksRemaining = arena.getGraceTime();
		this.matchTimeTicksRemaining = arena.getMatchTime();
	}

	/**
	 * Gets the current leaderboard of players based on their score.
	 *
	 * @return A sorted List of match participants where the first element is the top scoring player.
	 */
	public List<MatchParticipant> getLeaderboard(){
		List<MatchParticipant> participants = new LinkedList<>(this.getParticipants());
		participants.sort(Comparator.comparingInt(MatchParticipant::getScore).reversed());
		return participants;
	}

	/**
	 * Gets the arena this match is taking place in.
	 *
	 * @return The arena.
	 */
	public Arena getArena(){
		return this.arena;
	}

	/**
	 * Gets a copy of all match participants.
	 *
	 * @return A list of match participants.
	 */
	public List<MatchParticipant> getParticipants(){
		return new ArrayList<>(this.participants); // Return a copy rather than the reference to the original list.
	}

	/**
	 * Gets the match participant which represents a given player.
	 *
	 * @param player The player to get the match participant for.
	 * @return The match participant representing the player, or null if the player is not a participant in this match.
	 */
	public MatchParticipant getParticipant(final Player player){
		for (final MatchParticipant participant : this.getParticipants()){
			if (participant.getPlayer().equals(player)){
				return participant;
			}
		}
		return null;
	}

	/**
	 * Checks whether a given player is a participant in this match.
	 *
	 * @param player The player to check.
	 * @return True of the player is a participant in this match, false otherwise.
	 */
	public boolean isParticipant(final Player player){
		for (final MatchParticipant participant : this.getParticipants()){
			if (participant.getPlayer().equals(player)){
				return true;
			}
		}
		return false;
	}

	/**
	 * Removes a given match participant from this match.
	 *
	 * @param participant The match participant to remove.
	 */
	public void removeParticipant(final MatchParticipant participant){
		final boolean success = this.participants.remove(participant);
		if (success){
			this.broadcastMessageToPlayers(ChatColor.RED + participant.getPlayer().getName() + " has been removed from the the match.");
		}
	}

	/**
	 * Updates the state of this match. Intended to be called once per tick on the server's main thread by the match manager.
	 *
	 * @return True if the match is still ongoing, false if the final tick has just executed and no further ticks are required.
	 */
	public boolean tick(){
		if (this.getParticipants().size() < 2){
			this.endMatch();
			return false;
		}

		// First tick of grace period.
		if (graceTimeTicksRemaining >= arena.getGraceTime()){
			final double bossBarProgress = (double) graceTimeTicksRemaining / (double) (arena.getGraceTime());
			for (MatchParticipant participant : this.getParticipants()){
				final Player player = participant.getPlayer();
				player.teleport(this.arena.getRandomSpawnPoint()); // Teleport them to a random spawn point.
				player.setGameMode(GameMode.ADVENTURE); // Change their gamemode to adventure.
				player.setHealth(20.0); // Reset health to full.
				player.setFoodLevel(10); // Reset food level to full.
				player.setSaturation(0.0f); // Remove any saturation to make health regeneration minimal.
				player.getInventory().clear(); // Clear their inventory.
				player.updateInventory(); // Synchronize their inventory.

				this.snowbrawl.getBossBarManager().setBossBar(player, "Grace period. The match will begin in " + (graceTimeTicksRemaining / 20 + 1) + " seconds.", BarColor.GREEN, BarStyle.SEGMENTED_10, bossBarProgress);
				player.sendMessage(ChatColor.GOLD + "Grace period has begun. You may explore the arena before the match begins in " + (graceTimeTicksRemaining / 20) + " seconds.");
				player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1.0f, 0.75f);
			}

			graceTimeTicksRemaining -= 1;
			return true;
		}

		// Not first tick of grace period, but grace time still remaining.
		if (graceTimeTicksRemaining > 0){
			final double bossBarProgress = (double) graceTimeTicksRemaining / (double) (arena.getGraceTime());
			for (MatchParticipant participant : this.getParticipants()){
				final Player player = participant.getPlayer();
				this.snowbrawl.getBossBarManager().setBossBar(player, "Grace period. The match will begin in " + (graceTimeTicksRemaining / 20 + 1) + " seconds.", BarColor.GREEN, BarStyle.SEGMENTED_10, bossBarProgress);

				if ((graceTimeTicksRemaining <= 100 && graceTimeTicksRemaining % 20 == 0) || (graceTimeTicksRemaining <= 300 && graceTimeTicksRemaining % 100 == 0) || (graceTimeTicksRemaining % 600 == 0)){
					player.sendMessage(ChatColor.YELLOW + "The match will begin in " + (graceTimeTicksRemaining / 20) + " seconds.");
					player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_DISPENSE, SoundCategory.MASTER, 1.0f, 1.0f);
				}
			}

			graceTimeTicksRemaining -= 1;
			return true;
		}

		// No more grace time remaining, first tick of match.
		if (matchTimeTicksRemaining >= arena.getMatchTime()){
			for (MatchParticipant participant : this.getParticipants()){
				final Player player = participant.getPlayer();
				player.teleport(this.arena.getRandomSpawnPoint());
				player.setGameMode(GameMode.ADVENTURE); // Change their gamemode to adventure.
				player.setHealth(20.0); // Reset health to full.
				player.setFoodLevel(10); // Reset food level to full.
				player.setSaturation(0.0f); // Remove any saturation to make health regeneration minimal.
				player.getInventory().clear(); // Clear their inventory.
				player.updateInventory(); // Synchronize their inventory.
			}

			matchTimeTicksRemaining -= 1;
			return true;
		}

		// Match time has run out. Time to end the match.
		if (matchTimeTicksRemaining <= 0){
			this.endMatch();
			return false;
		}

		// No special cases above are true. Executes when match time is remaining.
		final double bossBarProgress = (double) matchTimeTicksRemaining / (double) (arena.getMatchTime());
		for (MatchParticipant participant : this.getParticipants()){
			final Player player = participant.getPlayer();
			// Update boss bar
			this.snowbrawl.getBossBarManager().setBossBar(player, (matchTimeTicksRemaining / 20 + 1) + " seconds remaining.", BarColor.GREEN, BarStyle.SEGMENTED_10, bossBarProgress);

			// If required, provide an update to the player that the match is ending soon.
			if ((matchTimeTicksRemaining <= 100 && matchTimeTicksRemaining % 20 == 0) || (matchTimeTicksRemaining <= 300 && matchTimeTicksRemaining % 100 == 0) || (matchTimeTicksRemaining <= 600 && matchTimeTicksRemaining % 600 == 0)){
				player.sendMessage(ChatColor.YELLOW + "The match will end in " + (matchTimeTicksRemaining / 20) + " seconds.");
				player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_DISPENSE, SoundCategory.MASTER, 1.0f, 1.0f);
			}

			// Update the player's action bar with theirs tats.
			final String actionBarText = ChatColor.GREEN + "" + participant.getKills() + ChatColor.WHITE + " / " + ChatColor.RED + participant.getDeaths();
			player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(actionBarText));

			//player.setGameMode(GameMode.ADVENTURE);
			// Keep food levels up to date.
			player.setFoodLevel(10);
			player.setSaturation(0.0f);
		}

		matchTimeTicksRemaining--;
		return true;
	}

	/**
	 * Ends this match by displaying a leaderboard and teleporting all players to the world's spawn.
	 */
	public void endMatch(){
		// Ensure no time is remaining on timers, just in case this match is prematurely ending.
		this.graceTimeTicksRemaining = 0;
		this.matchTimeTicksRemaining = 0;

		// Get final leaderboard.
		final List<MatchParticipant> leaderboard = this.getLeaderboard();

		// For each player still a member of the match
		for (MatchParticipant participant : this.getParticipants()){
			final Player player = participant.getPlayer();

			player.setGameMode(GameMode.SPECTATOR);
			player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("")); // Clear action bar.
			player.teleport(player.getWorld().getSpawnLocation()); // TODO: This maybe needs to be a configurable "global spawn" so that arenas can live in dedicated worlds, if needed.
			player.getInventory().clear();
			player.updateInventory();

			// Show a title indicating the match is over and what place the player achieved on the leaderboard.
			final int place = leaderboard.indexOf(participant);
			final String title;
			final String subtitle = "You placed #" + (place + 1) + " on the leaderboard.";
			if (place == 0){
				title = "WINNER WINNER CHICKEN DINNER!";
			} else {
				title = "Game over.";
			}
			player.sendTitle(ChatColor.GOLD + title, ChatColor.YELLOW + subtitle, 10, 70, 20);

			// Start printing conclusion message
			player.sendMessage(ChatColor.GREEN + "----------== [ MATCH SUMMARY ] ==----------");
			player.sendMessage(ChatColor.YELLOW + "Top Players:");
			// For the top 3 slots of the leaderboard, if a player is present, display their stats.
			for (int leaderboardPlace = 0; leaderboardPlace <= 2; leaderboardPlace++){
				if (leaderboardPlace >= leaderboard.size()){
					break; // Stop printing the leaderboard if there is fewer players in the match than places on the leaderboard we are printing.
				}

				final MatchParticipant leaderboardPlayer = leaderboard.get(leaderboardPlace);
				player.sendMessage(ChatColor.YELLOW + " - " + (leaderboardPlace + 1) + ": " + leaderboardPlayer.getPlayer().getName() + ": " + ChatColor.GOLD + ChatColor.BOLD + leaderboardPlayer.getScore() + ChatColor.YELLOW + " (" + ChatColor.GREEN + leaderboardPlayer.getKills() + ChatColor.YELLOW + "/" + ChatColor.RED + leaderboardPlayer.getDeaths() + ChatColor.YELLOW + ")"); // 1: Player: 2 (4/2)

			}

			// Display this player's own stats at the end.
			player.sendMessage();
			player.sendMessage(ChatColor.YELLOW + "Your score: " + ChatColor.GOLD + ChatColor.BOLD + participant.getScore() + ChatColor.YELLOW + " (" + ChatColor.GREEN + participant.getKills() + ChatColor.YELLOW + "/" + ChatColor.RED + participant.getDeaths() + ChatColor.YELLOW + ")");
			// Conclusion.
			player.sendMessage(ChatColor.GREEN + "-----------------------------------------");
		}
	}

	/**
	 * Broadcasts a chat message to all match participants.
	 *
	 * @param message The message to broadcast.
	 */
	public void broadcastMessageToPlayers(final String message){
		for (MatchParticipant participant : this.getParticipants()){
			participant.getPlayer().sendMessage(message);
		}
	}

	/**
	 * Removes quitting match participants from this match.
	 *
	 * @param event The event.
	 */
	public void onPlayerQuit(final PlayerQuitEvent event){
		this.removeParticipant(this.getParticipant(event.getPlayer()));
	}

	/**
	 * Kills match participants who leave the bounds of the arena.
	 *
	 * @param event The event.
	 */
	public void onPlayerMove(final PlayerMoveEvent event){
		final Player player = event.getPlayer();

		if (!this.getArena().isWithinBoundingBox(player.getLocation())){
			player.setHealth(0.0);
		}
	}

	/**
	 * Prevents match participants from interacting with blocks inside the arena.
	 *
	 * @param event The event.
	 */
	public void onPlayerInteract(final PlayerInteractEvent event){
		// TODO: This is incomplete. Needs to block everything except snow blocks at all time.
		if (this.graceTimeTicksRemaining > 0){
			event.setCancelled(true);
		}
	}

	/**
	 * Increments kill and death stats as needed and broadcasts death messages to match participants.
	 *
	 * @param event The event.
	 */
	public void onPlayerDeath(final PlayerDeathEvent event){
		// Ensure no XP is dropped.
		event.setDroppedExp(0);

		final Player victimPlayer = event.getEntity();
		final MatchParticipant victim = this.getParticipant(victimPlayer);
		victim.awardDeath();

		MatchParticipant killer = this.getParticipant(victim.getPlayer().getKiller());
		if (killer != null && killer != victim){
			killer.awardKill();
			if (this.getArena().getRegen() > 0){
				killer.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, (int) (Math.round(this.getArena().getRegen() / 1.67 * 20)), 2)); // Award the killer with health regeneration over a period of time based on the arena's configured regen amount.
			}
		}

		// Suppress global death message and only send to match participants.
		this.broadcastMessageToPlayers(ChatColor.GRAY + "" + ChatColor.ITALIC + event.getDeathMessage());
		event.setDeathMessage(null);

		// Force the player to respawn in one second.
		Bukkit.getScheduler().scheduleSyncDelayedTask(this.snowbrawl, () -> victimPlayer.spigot().respawn(), 20);

		// Clear the player's inventory.
		victimPlayer.getInventory().clear();
		victimPlayer.updateInventory();
	}

	/**
	 * Prevents entities from damaging each other during grace time.
	 *
	 * @param event The event.
	 */
	public void onEntityDamageByEntity(final EntityDamageByEntityEvent event){
		if (this.graceTimeTicksRemaining > 0){
			event.setCancelled(true);
		}
	}

	/**
	 * Changes the respawn point of dying match participants to a random spawn point in the arena.
	 *
	 * @param event The event.
	 */
	public void onPlayerRespawn(final PlayerRespawnEvent event){
		event.setRespawnLocation(this.getArena().getRandomSpawnPoint());
	}
}
