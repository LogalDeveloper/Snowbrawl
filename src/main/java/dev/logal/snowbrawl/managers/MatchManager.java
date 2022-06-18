/*
 * Copyright 2022 Logan Fick
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at: https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package dev.logal.snowbrawl.managers;

import dev.logal.snowbrawl.Snowbrawl;
import dev.logal.snowbrawl.types.Arena;
import dev.logal.snowbrawl.types.Match;
import dev.logal.snowbrawl.types.MatchParticipant;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Handles queueing of players for matches, creating matches, and keeping track of those matches until conclusion.
 */
public class MatchManager implements Listener, Runnable {
	public static final int MAXIMUM_QUEUE_WAIT_TIME_TICKS = (20 * 120); // The maximum amount of ticks the queue will wait to start a match when all criteria for a match is met.

	private final Snowbrawl snowbrawl;

	private final LinkedList<Player> queue = new LinkedList<>(); // Holds the current queue of players waiting for a match.
	private final ArrayList<Match> matches = new ArrayList<>(); // Holds current ongoing matches.
	private final Random rng = new Random();
	private int queueWaitTimeTicksRemaining = MAXIMUM_QUEUE_WAIT_TIME_TICKS; // Tracks how many ticks are remaining in the queue wait time.


	/**
	 * Creates a new match manager which is owned by a given instance of Snowbrawl.
	 *
	 * @param snowbrawl The instance of Snowbrawl which owns this match manager. Used for logging and to configure events.
	 */
	public MatchManager(final Snowbrawl snowbrawl){
		this.snowbrawl = snowbrawl;
		this.snowbrawl.getServer().getPluginManager().registerEvents(this, this.snowbrawl);
	}

	/**
	 * Adds a player to the queue of players waiting to enter a match.
	 *
	 * @param player The player to add to the queue.
	 */
	public void addToQueue(final Player player){
		if (this.getMatchFromPlayer(player) != null){
			throw new IllegalStateException("A player cannot be in a match and in the queue at the same time.");
		}

		this.snowbrawl.getLogger().info(player.getName() + " has been added to the queue.");
		this.queue.add(player);
	}

	/**
	 * Removes a player from the queue of players waiting to enter a match.
	 *
	 * @param player The player to remove from the queue.
	 */
	public void removeFromQueue(final Player player){
		this.snowbrawl.getLogger().info(player.getName() + " has been removed from the queue.");
		this.queue.remove(player);
	}

	/**
	 * Gets whether a given player is in the queue of players waiting to enter a match.
	 *
	 * @param player The player to test for.
	 *
	 * @return True if the given player is in the queue, false otherwise.
	 */
	public boolean isInQueue(final Player player){
		return this.queue.contains(player);
	}

	/**
	 * Gets a copy of the current queue of players waiting to enter a match.
	 *
	 * @return A List containing players in the queue.
	 */
	public List<Player> getQueuedPlayers(){
		return new LinkedList<>(this.queue); // Return a copy rather than the reference to the original list.
	}

	/**
	 * Gets a list of all players currently participating in matches.
	 *
	 * @return A list containing all known match participants.
	 */
	public List<MatchParticipant> getPlayersInMatches(){
		final List<MatchParticipant> matchParticipants = new ArrayList<>();
		for (Match match : this.matches){
			matchParticipants.addAll(match.getParticipants());
		}
		return matchParticipants;
	}

	/**
	 * Gets the match this player is currently in.
	 *
	 * @param player The player to look up.
	 *
	 * @return The match the player is in, null if they are not in a match.
	 */
	public Match getMatchFromPlayer(final Player player){
		for (Match match : this.matches){
			if (match.isParticipant(player)){
				return match;
			}
		}
		return null;
	}

	/**
	 * Gets all arenas which are not currently in use by a match.
	 *
	 * @return A list of available arenas.
	 */
	public List<Arena> getAvailableArenas(){
		final List<Arena> availableArenas = new ArrayList<>();

		for (Arena arena : this.snowbrawl.getArenaManager().getArenas()){
			if (arena.isEnabled() && this.getMatchFromArena(arena) == null){
				availableArenas.add(arena);
			}
		}

		return availableArenas;
	}

	/**
	 * Gets the match a given arena is in use by.
	 *
	 * @param arena The arena to look up.
	 *
	 * @return The match the arena is in use by, null if the arena is not in use.
	 */
	public Match getMatchFromArena(final Arena arena){
		for (Match match : this.matches){
			if (match.getArena().equals(arena)){
				return match;
			}
		}

		return null;
	}

	/**
	 * Updates the state of the match manager and all matches it tracks. Intended to be called once per tick on the server's main thread.
	 */
	@Override
	public void run(){
		this.tick();
	}

	/**
	 * Updates the state of the match manager and all matches it tracks. Intended to be called once per tick on the server's main thread.
	 */
	public void tick(){
		this.updateQueue();

		// Tick all matches. The newest match will be included as well.
		// Clone the list of matches to prevent concurrent modification.
		List<Match> matchesToTick = new ArrayList<>(this.matches);
		for (Match match : matchesToTick){
			if (!match.tick()){
				this.matches.remove(match);
			}
		}
	}

	private void updateQueue(){
		// Display a default boss bar instruction to players who are not queued and not in a match.
		for (Player player : this.snowbrawl.getServer().getOnlinePlayers()){
			if (!this.isInQueue(player) && this.snowbrawl.getMatchManager().getMatchFromPlayer(player) == null){
				this.snowbrawl.getBossBarManager().setBossBar(player, "You are not in the queue. Run /sb to queue for a match.", BarColor.YELLOW, BarStyle.SOLID, 1.0);
			}
		}

		// There are either 0 or 1 players in the queue, which is not enough for a match.
		if (this.queue.size() < 2){
			// Reset the queue timer.
			this.queueWaitTimeTicksRemaining = MAXIMUM_QUEUE_WAIT_TIME_TICKS;

			// Tell players in the queue via boss bar update.
			for (Player player : this.queue){
				this.snowbrawl.getBossBarManager().setBossBar(player, "There are not enough players in the queue.", BarColor.RED, BarStyle.SOLID, 1.0);
			}

			// Stop any further update to the queue.
			return;
		}

		// There are at least 2 people in the queue. Now get a list of available arenas.
		final List<Arena> availableArenas = this.getAvailableArenas();
		if (availableArenas.size() == 0){
			// No arenas available. Reset queue timer and stop handling the queue.
			this.queueWaitTimeTicksRemaining = MAXIMUM_QUEUE_WAIT_TIME_TICKS;

			// Tell players in the queue about the problem via boss bar update.
			for (Player player : this.queue){
				this.snowbrawl.getBossBarManager().setBossBar(player, "There are no arenas available.", BarColor.RED, BarStyle.SOLID, 1.0);
			}

			// Stop any further update to the queue.
			return;
		}

		// There are available arenas. Pick a random one.
		final Arena candidateArena = availableArenas.get(rng.nextInt(0, availableArenas.size()));

		// Has the queue timer run out?
		if (queueWaitTimeTicksRemaining <= 0){
			// We have a candidate arena and the queue timer has expired. Time to build a match.
			// First, let's pull some players from the head of the queue.
			final ArrayList<Player> players = new ArrayList<>();

			// TODO: This seems over complicated. There's most likely a better way to do this.
			// Iterate x amount of times, where x is the maximum amount of players allowed in the arena.
			for (int i = 0; i < candidateArena.getPlayerLimit(); i++){
				// Are there still players remaining in the queue?
				if (this.queue.size() > 0){
					// Yes. Add them to the players list.
					players.add(this.queue.poll());
				} else {
					// No. stop adding players.
					break;
				}
			}

			// Finally, create the match.
			this.matches.add(new Match(this.snowbrawl, candidateArena, players));
			return;
		}

		queueWaitTimeTicksRemaining -= this.getQueueDecayRate();

		final double bossBarProgress = (double) queueWaitTimeTicksRemaining / (double) MAXIMUM_QUEUE_WAIT_TIME_TICKS;
		final int secondsRemaining = this.getQueueSecondsRemaining();
		for (Player player : this.queue){
			this.snowbrawl.getBossBarManager().setBossBar(player, "The next match will start in " + secondsRemaining + " seconds.", BarColor.GREEN, BarStyle.SEGMENTED_10, bossBarProgress);
		}

		// The above queue system works, but has some problems.
		// TODO: Pick bigger maps (larger player limit) when bigger amounts of players are in the queue. The current pure RNG makes some odd choices at times.
	}

	/**
	 * Gets how many ticks per second the queue wait time will decay given how many players online are queued and not in a match.
	 *
	 * @return The amount of ticks per second the queue wait time will decay, up to 10.
	 */
	public int getQueueDecayRate(){
		final double availablePlayers = this.snowbrawl.getServer().getOnlinePlayers().size() - this.getPlayersInMatches().size(); // The amount of players online but not in a match.
		final double queuedPlayers = this.getQueuedPlayers().size(); // The amount of queued players.

		final int decayRate = (int) ((queuedPlayers / availablePlayers) * 10);

		// Clamp decay rate between 1 and 10 ticks per second.
		if (decayRate < 1){
			return 1;
		} else if (decayRate > 10){
			return 10;
		} else {
			return decayRate;
		}
	}

	/**
	 * Gets the amount of seconds remaining on the queue wait timer.
	 *
	 * @return The amount of seconds remaining.
	 */
	private int getQueueSecondsRemaining(){
		return (this.queueWaitTimeTicksRemaining / (20 * this.getQueueDecayRate())) + 1;
	}

	/*
	 * These event handlers build upon the default event handlers in the arena manager.
	 *
	 * Most of the event handlers remove the operator exemptions for operators who are in matches. The intent is to prevent operators from accidentally tampering with the arena during normal gameplay.
	 * Some event handlers also forward events down to specific matches if the match itself needs to perform an action in response to an event.
	 */

	/**
	 * Prevents match participants from building inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockCanBuild(final BlockCanBuildEvent event){
		final Match match = this.getMatchFromPlayer(event.getPlayer());

		if (match != null){
			event.setBuildable(false);
		}
	}

	/**
	 * Prevents match participants from damaging blocks inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockDamage(final BlockDamageEvent event){
		final Match match = this.getMatchFromPlayer(event.getPlayer());

		if (match != null){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents match participants from fertilizing blocks inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockFertilize(final BlockFertilizeEvent event){
		final Match match = this.getMatchFromPlayer(event.getPlayer());

		if (match != null){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents match participants from igniting blocks inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockIgnite(final BlockIgniteEvent event){
		final Player player = event.getPlayer();
		if (player != null){
			final Match match = this.getMatchFromPlayer(event.getPlayer());
			if (match != null){
				event.setCancelled(true);
			}
		}
	}

	/**
	 * Prevents match participants from placing blocks inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockPlace(final BlockPlaceEvent event){
		final Match match = this.getMatchFromPlayer(event.getPlayer());

		if (match != null){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents match participants from changing cauldron levels.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onCauldronLevelChange(final CauldronLevelChangeEvent event){
		final Entity entity = event.getEntity();
		if (entity instanceof final Player player){
			final Match match = this.getMatchFromPlayer(player);

			if (match != null){
				event.setCancelled(true);
			}
		}
	}

	/**
	 * Prevents match participants from changing signs inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onSignChange(final SignChangeEvent event){
		final Match match = this.getMatchFromPlayer(event.getPlayer());

		if (match != null){
			event.setCancelled(true);
		}
	}

	/**
	 * Notifies appropriate matches of player deaths.
	 *
	 * @param event The event.
	 */
	@EventHandler
	public void onPlayerDeath(final PlayerDeathEvent event){
		final Player player = event.getEntity();

		Match match = this.getMatchFromPlayer(player);
		if (match != null){
			match.onPlayerDeath(event);
		}
	}

	/**
	 * Notifies appropriate matches of entity damages by players.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onEntityDamageByEntity(final EntityDamageByEntityEvent event){
		final Arena arena = this.snowbrawl.getArenaManager().getArenaByLocation(event.getEntity().getLocation());
		if (arena != null){
			final Match match = this.getMatchFromArena(arena);
			if (match != null){
				match.onEntityDamageByEntity(event);
			}
		}
	}

	/**
	 * Prevents match participants from picking up items except snowballs.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onEntityPickupItem(final EntityPickupItemEvent event){
		final Entity entity = event.getEntity();
		if (entity instanceof final Player player){
			final Match match = this.getMatchFromPlayer(player);

			if (match != null){
				if (!event.getItem().getItemStack().getType().equals(Material.SNOWBALL)){ // Is the item not a snowball?
					event.setCancelled(true);
				}
			}
		}
	}

	/**
	 * Prevents match participants from placing entities from items.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onEntityPlace(final EntityPlaceEvent event){
		final Match match = this.getMatchFromPlayer(event.getPlayer());

		if (match != null){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents match participants from gaining hunger.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onFoodLevelChange(final FoodLevelChangeEvent event){
		if (event.getEntity() instanceof final Player player){
			final Match match = this.getMatchFromPlayer(player);

			if (match != null){
				event.setCancelled(true);
			}
		}
	}

	/**
	 * Prevents match participants from entering beds.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onPlayerBedEnter(final PlayerBedEnterEvent event){
		final Match match = this.getMatchFromPlayer(event.getPlayer());

		if (match != null){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents match participants from capturing entities inside of buckets.
	 *
	 * @param event The event.
	 */
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onPlayerBucketEntity(final PlayerBucketEntityEvent event){
		final Match match = this.getMatchFromPlayer(event.getPlayer());

		if (match != null){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents match participants from dropping items.
	 *
	 * @param event The event.
	 */
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onPlayerDropItem(final PlayerDropItemEvent event){
		final Match match = this.getMatchFromPlayer(event.getPlayer());

		if (match != null){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents match participants from editing books inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onPlayerEditBook(final PlayerEditBookEvent event){
		final Match match = this.getMatchFromPlayer(event.getPlayer());

		if (match != null){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents match participants from fishing inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onPlayerFish(final PlayerFishEvent event){
		final Match match = this.getMatchFromPlayer(event.getPlayer());

		if (match != null){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents non-operators from harvesting blocks inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onPlayerHarvestBlock(final PlayerHarvestBlockEvent event){
		final Match match = this.getMatchFromPlayer(event.getPlayer());

		if (match != null){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents match participants from interacting with entities inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onPlayerInteractEntity(final PlayerInteractEntityEvent event){
		final Match match = this.getMatchFromPlayer(event.getPlayer());

		if (match != null){
			event.setCancelled(true);
		}
	}

	/**
	 * Notifies appropriate matches of player interactions.
	 *
	 * @param event The event.
	 */
	@EventHandler
	public void onPlayerInteract(final PlayerInteractEvent event){
		final Match match = this.getMatchFromPlayer(event.getPlayer());

		if (match != null){
			match.onPlayerInteract(event);
		}
	}

	/**
	 * Prevents match participants from consuming items inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onPlayerItemConsume(final PlayerItemConsumeEvent event){
		final Match match = this.getMatchFromPlayer(event.getPlayer());

		if (match != null){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents match participants' items taking durability damage inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onPlayerItemDamage(final PlayerItemDamageEvent event){
		final Match match = this.getMatchFromPlayer(event.getPlayer());

		if (match != null){
			event.setCancelled(true);
		}
	}

	/**
	 * Notifies appropriate matches of player movements.
	 *
	 * @param event The event.
	 */
	@EventHandler
	public void onPlayerMove(final PlayerMoveEvent event){
		final Player player = event.getPlayer();

		Match match = this.getMatchFromPlayer(player);
		if (match != null){
			match.onPlayerMove(event);
		}
	}

	/**
	 * Removes quitting players from the queue and notifies appropriate matches of the quits.
	 *
	 * @param event The event.
	 */
	@EventHandler
	public void onPlayerQuit(final PlayerQuitEvent event){
		final Player player = event.getPlayer();

		if (this.isInQueue(player)){
			this.removeFromQueue(player);
		}

		Match match = this.getMatchFromPlayer(player);
		if (match != null){
			match.onPlayerQuit(event);
		}
	}

	/**
	 * Prevents match participants from discovering recipes inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onRecipeDiscover(final PlayerRecipeDiscoverEvent event){
		final Match match = this.getMatchFromPlayer(event.getPlayer());

		if (match != null){
			event.setCancelled(true);
		}
	}

	/**
	 * Notifies appropriate matches of player respawns.
	 *
	 * @param event The event.
	 */
	@EventHandler
	public void onPlayerRespawn(final PlayerRespawnEvent event){
		final Match match = this.getMatchFromPlayer(event.getPlayer());

		if (match != null){
			match.onPlayerRespawn(event);
		}
	}

	/**
	 * Prevents match participants from shearing entities inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onPlayerShearEntity(final PlayerShearEntityEvent event){
		final Match match = this.getMatchFromPlayer(event.getPlayer());

		if (match != null){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents match participants from taking books off a lectern inside of arenas.
	 *
	 * @param event The events.
	 */
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onPlayerTakeLecternBook(final PlayerTakeLecternBookEvent event){
		final Match match = this.getMatchFromPlayer(event.getPlayer());

		if (match != null){
			event.setCancelled(true);
		}
	}
}
