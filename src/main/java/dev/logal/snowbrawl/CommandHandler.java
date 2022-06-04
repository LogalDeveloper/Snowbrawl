/*
 * Copyright 2022 Logan Fick
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at: https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package dev.logal.snowbrawl;

import dev.logal.snowbrawl.managers.MatchManager;
import dev.logal.snowbrawl.types.Arena;
import net.nemez.cmdmgr.Command;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles all the different commands implemented by Snowbrawl through the Command Manager.
 */
public final class CommandHandler {
	private final Snowbrawl snowbrawl;

	/**
	 * Creates a new command handler which is owned by a given instance of Snowbrawl.
	 *
	 * @param snowbrawl The instance of Snowbrawl which owns this command handler. Used for logging and to configure events.
	 */
	public CommandHandler(final Snowbrawl snowbrawl){
		this.snowbrawl = snowbrawl;
	}

	/**
	 * Generates a chat message with the plugin name and formatting to indicate successful execution.
	 *
	 * @param message The message to format.
	 *
	 * @return The message with formatting applied.
	 */
	public static String successMessage(final String message){
		return ChatColor.DARK_GREEN + "[" + ChatColor.GREEN + "Snowbrawl" + ChatColor.DARK_GREEN + "] " + ChatColor.GREEN + message;
	}

	/**
	 * Generates a chat message with the plugin name and formatting to indicate failed execution.
	 *
	 * @param message The message to format.
	 *
	 * @return The message with formatting applied.
	 */
	public static String failMessage(final String message){
		return ChatColor.DARK_RED + "[" + ChatColor.RED + "Snowbrawl" + ChatColor.DARK_RED + "] " + ChatColor.RED + message;
	}

	/**
	 * Joins or removes the player from the queue.
	 *
	 * @param sender The user who executed the command.
	 */
	@Command(hook = "join_queue")
	public void commandJoinQueue(CommandSender sender){
		MatchManager matchManager = this.snowbrawl.getMatchManager();
		Player player = (Player) sender;

		if (matchManager.isInQueue(player)){
			matchManager.removeFromQueue(player);
			sender.sendMessage(successMessage("You have been removed from the queue."));
		} else {
			matchManager.addToQueue(player);
			sender.sendMessage(successMessage("You have been added to the queue."));
		}
	}

	/**
	 * Lists all known arenas.
	 *
	 * @param sender The user who executed the command.
	 */
	@Command(hook = "get_arenas")
	public void commandArenaList(CommandSender sender){
		List<Arena> arenas = this.snowbrawl.getArenaManager().getArenas();
		if (arenas.size() == 0){
			sender.sendMessage(successMessage("There are no known arenas."));
		} else {
			sender.sendMessage(successMessage("The following arenas are known:"));
			for (Arena arena : arenas){
				sender.sendMessage(successMessage(" - " + arena.getName()));
			}
		}
	}

	/**
	 * Creates a new arena with a given name, inferring the world name from the player's position.
	 *
	 * @param sender The user who executed the command.
	 * @param name   The name for the new arena.
	 */
	@Command(hook = "create_arena")
	public void commandArenaCreate(CommandSender sender, String name){
		Player player = (Player) sender; // CommandManager blocks console from executing this command, so this should never fail.
		Arena arena;
		try{
			arena = new Arena(name, player.getWorld());
		} catch (final IllegalArgumentException exception){
			sender.sendMessage(failMessage(exception.getMessage()));
			return;
		}

		try{
			this.snowbrawl.getArenaManager().registerArena(arena);
		} catch (final IllegalArgumentException exception){
			sender.sendMessage(failMessage(exception.getMessage()));
			return;
		}

		sender.sendMessage(successMessage("Successfully created new arena " + arena.getName() + "."));
	}

	/**
	 * Creates a new arena with a given name, inferring the world name from the player's position.
	 *
	 * @param sender The user who executed the command.
	 * @param name   The name for the new arena.
	 */
	@Command(hook = "delete_arena")
	public void commandArenaDelete(CommandSender sender, String name){
		Arena arena = this.snowbrawl.getArenaManager().getArenaByName(name);
		if (arena == null){
			sender.sendMessage(failMessage("The specified arena does not exist."));
			return;
		}

		this.snowbrawl.getArenaManager().unregisterArena(arena);
		sender.sendMessage(successMessage("Successfully deleted arena " + arena.getName() + "."));
	}

	/**
	 * Enable an arena.
	 *
	 * @param sender The user who executed the command.
	 * @param name The name of the arena.
	 */
	@Command(hook = "enable_arena")
	public void commandArenaEnable(CommandSender sender, String name){
		Arena arena = this.snowbrawl.getArenaManager().getArenaByName(name);
		if (arena == null){
			sender.sendMessage(failMessage("The specified arena does not exist."));
			return;
		}

		arena.setEnabled(true);
		sender.sendMessage(successMessage("Arena " + arena.getName() + " has been enabled."));
		this.snowbrawl.saveConfig();
	}

	/**
	 * Disables an arena.
	 *
	 * @param sender The user who executed the command.
	 * @param name The name of the arena.
	 */
	@Command(hook = "disable_arena")
	public void commandArenaDisable(CommandSender sender, String name){
		Arena arena = this.snowbrawl.getArenaManager().getArenaByName(name);
		if (arena == null){
			sender.sendMessage(failMessage("The specified arena does not exist."));
			return;
		}

		arena.setEnabled(false);
		sender.sendMessage(successMessage("Arena " + arena.getName() + " has been disabled."));
		this.snowbrawl.saveConfig();
	}

	/**
	 * Creates a new arena with a given name in a specific world.
	 *
	 * @param sender    The user who executed the command.
	 * @param name      The name for the new arena.
	 * @param worldName The name of the world to create the arena in.
	 */
	@Command(hook = "create_arena_in_specific_world")
	public void commandArenaCreate(CommandSender sender, String name, String worldName){
		World world = Bukkit.getWorld(worldName);
		if (world == null){
			sender.sendMessage(failMessage("The specified world name does not exist."));
			return;
		}

		Arena arena;
		try{
			arena = new Arena(name, world);
		} catch (final IllegalArgumentException exception){
			sender.sendMessage(failMessage(exception.getMessage()));
			return;
		}

		try{
			this.snowbrawl.getArenaManager().registerArena(arena);
		} catch (final IllegalArgumentException exception){
			sender.sendMessage(failMessage(exception.getMessage()));
			return;
		}

		sender.sendMessage(successMessage("A new arena named " + arena.getName() + " has been created."));
	}

	/**
	 * Prints all configuration options and their values for an arena.
	 *
	 * @param sender The user who executed the command.
	 * @param name   The name of the arena.
	 */
	@Command(hook = "get_arena_config")
	public void commandArenaConfig(CommandSender sender, String name){
		Arena arena = this.snowbrawl.getArenaManager().getArenaByName(name);
		if (arena == null){
			sender.sendMessage(failMessage("The specified arena does not exist."));
			return;
		}

		sender.sendMessage(successMessage("Configuration variables for arena " + arena.getName()) + ":");
		sender.sendMessage(successMessage(" - Refill: " + arena.getRefill() + " snowballs"));
		sender.sendMessage(successMessage(" - Player Limit: " + arena.getPlayerLimit() + " players"));
		sender.sendMessage(successMessage(" - Grace Time: " + (arena.getGraceTime() / 20) + "s"));
		sender.sendMessage(successMessage(" - Match Time: " + (arena.getMatchTime() / 20) + "s"));
		sender.sendMessage(successMessage(" - Damage: " + arena.getDamage() + " HP"));
		sender.sendMessage(successMessage(" - World: " + arena.getWorld().getName()));

		Location pos1 = arena.getPos1(), pos2 = arena.getPos2();
		sender.sendMessage(successMessage(" - Pos1: " + pos1.getX() + ", " + pos1.getY() + ", " + pos1.getZ()));
		sender.sendMessage(successMessage(" - Pos2: " + pos2.getX() + ", " + pos2.getY() + ", " + pos2.getZ()));
	}

	/**
	 * Changes the name of an arena.
	 *
	 * @param sender  The user who executed the command.
	 * @param name    The name of the arena.
	 * @param newName The new name for the arena.
	 */
	@Command(hook = "set_arena_name")
	public void commandArenaConfigName(CommandSender sender, String name, String newName){
		Arena arena = this.snowbrawl.getArenaManager().getArenaByName(name);
		if (arena == null){
			sender.sendMessage(failMessage("The specified arena does not exist."));
			return;
		}

		if (this.snowbrawl.getArenaManager().getArenaByName(newName) != null){
			sender.sendMessage(failMessage("An arena with that name already exists."));
			return;
		}

		String oldName = arena.getName();
		try{
			arena.setName(newName);
		} catch (final IllegalArgumentException exception){
			sender.sendMessage(failMessage(exception.getMessage()));
			return;
		}

		sender.sendMessage(successMessage("The name for arena " + oldName + " has been changed to " + arena.getName()));
		this.snowbrawl.saveConfig();
	}

	/**
	 * Changes the amount of snowballs given when a player refills their stock in an arena
	 *
	 * @param sender The user who executed the command.
	 * @param name   The name of the arena.
	 * @param amount The new refill amount for the arena.
	 */
	@Command(hook = "set_arena_refill")
	public void commandArenaConfigRefill(CommandSender sender, String name, int amount){
		Arena arena = this.snowbrawl.getArenaManager().getArenaByName(name);
		if (arena == null){
			sender.sendMessage(failMessage("The specified arena does not exist."));
			return;
		}

		int oldRefill = arena.getRefill();
		try{
			arena.setRefill(amount);
		} catch (final IllegalArgumentException exception){
			sender.sendMessage(failMessage(exception.getMessage()));
			return;
		}

		sender.sendMessage(successMessage("The refill for arena " + arena.getName() + " has been changed from " + oldRefill + " snowballs to " + arena.getRefill() + " snowballs."));
		this.snowbrawl.saveConfig();
	}

	/**
	 * Changes the maximum amount of players allowed to play in an arena.
	 *
	 * @param sender  The user who executed the command.
	 * @param name    The name of the arena.
	 * @param players The new player limit for the arena.
	 */
	@Command(hook = "set_arena_player_limit")
	public void commandArenaConfigPlayers(CommandSender sender, String name, int players){
		Arena arena = this.snowbrawl.getArenaManager().getArenaByName(name);
		if (arena == null){
			sender.sendMessage(failMessage("The specified arena does not exist."));
			return;
		}

		int oldPlayerLimit = arena.getPlayerLimit();
		try{
			arena.setPlayerLimit(players);
		} catch (final IllegalArgumentException exception){
			sender.sendMessage(failMessage(exception.getMessage()));
			return;
		}

		sender.sendMessage(successMessage("The player limit for arena " + arena.getName() + " has been changed from " + oldPlayerLimit + " players to " + arena.getPlayerLimit() + " players."));
		this.snowbrawl.saveConfig();
	}

	/**
	 * Changes the amount of grace time allowed before each match in an arena.
	 *
	 * @param sender  The user who executed the command.
	 * @param name    The name of the arena.
	 * @param seconds The amount of grace time in seconds for the arena.
	 */
	@Command(hook = "set_arena_grace_time")
	public void commandArenaConfigGraceTime(CommandSender sender, String name, int seconds){
		Arena arena = this.snowbrawl.getArenaManager().getArenaByName(name);
		if (arena == null){
			sender.sendMessage(failMessage("The specified arena does not exist."));
			return;
		}

		int oldGraceTime = arena.getGraceTime();
		try{
			arena.setGraceTime(seconds * 20);
		} catch (final IllegalArgumentException exception){
			sender.sendMessage(failMessage(exception.getMessage()));
			return;
		}

		sender.sendMessage(successMessage("The grace time for arena " + arena.getName() + " has been changed from " + (oldGraceTime / 20) + " seconds to " + (arena.getGraceTime() / 20) + " seconds."));
		this.snowbrawl.saveConfig();
	}

	/**
	 * Changes the amount of match time for each match in an arena.
	 *
	 * @param sender  The user who executed the command.
	 * @param name    The name of the arena.
	 * @param seconds The amount of match time in seconds for the arena.
	 */
	@Command(hook = "set_arena_match_time")
	public void commandArenaConfigMatchTime(CommandSender sender, String name, int seconds){
		Arena arena = this.snowbrawl.getArenaManager().getArenaByName(name);
		if (arena == null){
			sender.sendMessage(failMessage("The specified arena does not exist."));
			return;
		}

		int oldMatchTime = arena.getMatchTime();
		try{
			arena.setMatchTime(seconds * 20);
		} catch (final IllegalArgumentException exception){
			sender.sendMessage(failMessage(exception.getMessage()));
			return;
		}

		sender.sendMessage(successMessage("The match time for arena " + arena.getName() + " has been changed from " + (oldMatchTime / 20) + " seconds to " + (arena.getMatchTime() / 20) + " seconds."));
		this.snowbrawl.saveConfig();
	}

	/**
	 * Changes the amount of damage done per snowball hit in this arena.
	 *
	 * @param sender The user who executed the command.
	 * @param name   The name of the arena.
	 * @param damage The amount of damage for this arena.
	 */
	@Command(hook = "set_arena_damage")
	public void commandArenasDamage(CommandSender sender, String name, int damage){
		Arena arena = this.snowbrawl.getArenaManager().getArenaByName(name);
		if (arena == null){
			sender.sendMessage(failMessage("The specified arena does not exist."));
			return;
		}

		int oldDamage = arena.getDamage();
		try{
			arena.setDamage(damage);
		} catch (final IllegalArgumentException exception){
			sender.sendMessage(failMessage(exception.getMessage()));
			return;
		}

		sender.sendMessage(successMessage("The damage for arena " + arena.getName() + " has been changed from " + oldDamage + " HP to " + arena.getDamage() + " HP."));
		this.snowbrawl.saveConfig();
	}

	/**
	 * Sets one corner of this arena's bounds to the given coordinates.
	 *
	 * @param sender The user who executed the command
	 * @param name   The name of the arena.
	 * @param x      The X coordinate for the corner.
	 * @param y      The Y coordinate for the corner.
	 * @param z      The Z coordinate for the corner.
	 */
	@Command(hook = "set_arena_pos1")
	public void commandArenaConfigBoundsPos1(CommandSender sender, String name, int x, int y, int z){
		Arena arena = this.snowbrawl.getArenaManager().getArenaByName(name);
		if (arena == null){
			sender.sendMessage(failMessage("The specified arena does not exist."));
			return;
		}

		Location oldPos1 = arena.getPos1();
		try{
			arena.setPos1(new Location(null, x, y, z));
		} catch (final IllegalStateException exception){
			sender.sendMessage(failMessage(exception.getMessage()));
			return;
		}

		Location newPos1 = arena.getPos1();
		sender.sendMessage(successMessage("Pos1 for arena " + arena.getName() + " has been changed from " + oldPos1.getX() + "/" + oldPos1.getY() + "/" + oldPos1.getZ() + " to " + newPos1.getX() + "/" + newPos1.getY() + "/" + newPos1.getZ() + "."));
		this.snowbrawl.saveConfig();
	}

	/**
	 * Sets one corner of this arena's bounds to the player's current position.
	 *
	 * @param sender The user who executed the command
	 * @param name   The name of the arena.
	 */
	@Command(hook = "set_arena_pos1_from_playerpos")
	public void commandArenaConfigBoundsPos1FromPlayerPos(CommandSender sender, String name){
		Arena arena = this.snowbrawl.getArenaManager().getArenaByName(name);
		if (arena == null){
			sender.sendMessage(failMessage("The specified arena does not exist."));
			return;
		}

		Location playerPos = ((Player) sender).getLocation(); // CommandManager blocks console from executing this command, so this should never fail.
		this.commandArenaConfigBoundsPos1(sender, name, (int) playerPos.getX(), (int) playerPos.getY(), (int) playerPos.getZ());
	}

	/**
	 * Sets the other corner of this arena's bounds to the given coordinates.
	 *
	 * @param sender The user who executed the command
	 * @param name   The name of the arena.
	 * @param x      The X coordinate for the corner.
	 * @param y      The Y coordinate for the corner.
	 * @param z      The Z coordinate for the corner.
	 */
	@Command(hook = "set_arena_pos2")
	public void commandArenaConfigBoundsPos2(CommandSender sender, String name, int x, int y, int z){
		Arena arena = this.snowbrawl.getArenaManager().getArenaByName(name);
		if (arena == null){
			sender.sendMessage(failMessage("The specified arena does not exist."));
			return;
		}

		Location oldPos2 = arena.getPos2();
		try{
			arena.setPos2(new Location(null, x, y, z));
		} catch (final IllegalStateException exception){
			sender.sendMessage(failMessage(exception.getMessage()));
			return;
		}

		Location newPos2 = arena.getPos2();
		sender.sendMessage(successMessage("Pos2 for arena " + arena.getName() + " has been changed from " + oldPos2.getX() + "/" + oldPos2.getY() + "/" + oldPos2.getZ() + " to " + newPos2.getX() + "/" + newPos2.getY() + "/" + newPos2.getZ() + "."));
		this.snowbrawl.saveConfig();
	}

	/**
	 * Sets the other corner of this arena's bounds to the player's current position.
	 *
	 * @param sender The user who executed the command
	 * @param name   The name of the arena.
	 */
	@Command(hook = "set_arena_pos2_from_playerpos")
	public void commandArenaConfigBoundsPos2FromPlayerPos(CommandSender sender, String name){
		Arena arena = this.snowbrawl.getArenaManager().getArenaByName(name);
		if (arena == null){
			sender.sendMessage(failMessage("The specified arena does not exist."));
			return;
		}

		Location playerPos = ((Player) sender).getLocation(); // CommandManager blocks console from executing this command, so this should never fail.
		this.commandArenaConfigBoundsPos2(sender, name, (int) playerPos.getX(), (int) playerPos.getY(), (int) playerPos.getZ());
	}

	/**
	 * Sets this arena's bounds to the given pair of coordinates.
	 *
	 * @param sender The user who executed the command.
	 * @param name   The name of the arena.
	 * @param x1     The X coordinate for Pos1.
	 * @param y1     The Y coordinate for Pos1.
	 * @param z1     The Z coordinate for Pos1.
	 * @param x2     The X coordinate for Pos2.
	 * @param y2     The Y coordinate for Pos2.
	 * @param z2     The Z coordinate for Pos2.
	 */
	@Command(hook = "set_arena_bounds")
	public void commandArenaConfigBounds(CommandSender sender, String name, int x1, int y1, int z1, int x2, int y2, int z2){
		Arena arena = this.snowbrawl.getArenaManager().getArenaByName(name);
		if (arena == null){
			sender.sendMessage(failMessage("The specified arena does not exist."));
			return;
		}

		this.commandArenaConfigBoundsPos1(sender, name, x1, y1, z1);
		this.commandArenaConfigBoundsPos2(sender, name, x2, y2, z2);
	}

	/**
	 * Lists an arena's spawn points.
	 *
	 * @param sender The user who executed the command.
	 * @param name   The name of the arena.
	 */
	@Command(hook = "get_arena_spawnpoints")
	public void commandArenaConfigSpawnpointsList(CommandSender sender, String name){
		Arena arena = this.snowbrawl.getArenaManager().getArenaByName(name);
		if (arena == null){
			sender.sendMessage(failMessage("The specified arena does not exist."));
			return;
		}

		if (arena.getSpawnPoints().size() == 0){
			sender.sendMessage(successMessage("Arena " + arena.getName() + " has no spawn points."));
		} else {
			sender.sendMessage(successMessage("Arena " + arena.getName() + " has the following spawn points:"));
			List<Location> spawnPoints = arena.getSpawnPoints();
			for (int i = 0; i < spawnPoints.size(); i++){
				Location spawnPoint = spawnPoints.get(i);
				sender.sendMessage(successMessage(" - #" + i + ": " + spawnPoint.getX() + "/" + spawnPoint.getY() + "/" + spawnPoint.getZ() + "/" + spawnPoint.getYaw() + "/" + spawnPoint.getPitch()));
			}
		}
	}

	/**
	 * Adds a player's current location as a spawn point.
	 *
	 * @param sender The user who executed the command.
	 * @param name   The name of the arena.
	 */
	@Command(hook = "add_arena_spawnpoint_from_playerpos")
	public void commandArenaConfigSpawnpointsAddFromPlayerPos(CommandSender sender, String name){
		Arena arena = this.snowbrawl.getArenaManager().getArenaByName(name);
		if (arena == null){
			sender.sendMessage(failMessage("The specified arena does not exist."));
			return;
		}

		Location playerPos = ((Player) sender).getLocation(); // CommandManager blocks console from executing this command, so this should never fail.
		try{
			arena.addSpawnPoint(playerPos);
		} catch (final IllegalArgumentException exception){
			sender.sendMessage(failMessage(exception.getMessage()));
			return;
		}

		sender.sendMessage(successMessage("A spawn point at " + playerPos.getX() + "/" + playerPos.getY() + "/" + playerPos.getZ() + "/" + playerPos.getYaw() + "/" + playerPos.getPitch() + " has been added."));
		this.snowbrawl.saveConfig();
	}

	/**
	 * Adds a new spawn point with a given set of coordinates.
	 *
	 * @param sender The user who executed the command.
	 * @param name The name of the arena.
	 * @param x The X coordinate for the spawn point.
	 * @param y The Y coordiante for the spawn point.
	 * @param z The Z coordinate for the spawn point.
	 * @param yaw The yaw for the spawn point.
	 * @param pitch The pitch for the spawn point.
	 */
	@Command(hook = "add_arena_spawnpoint")
	public void commandArenaConfigSpawnpointsAdd(CommandSender sender, String name, double x, double y, double z, double yaw, double pitch){
		Arena arena = this.snowbrawl.getArenaManager().getArenaByName(name);
		if (arena == null){
			sender.sendMessage(failMessage("The specified arena does not exist."));
			return;
		}

		Location coordinates = new Location(arena.getWorld(), x, y, z, (float) yaw, (float) pitch); // TODO: Is an upstream fix to CommandManager required?
		try{
			arena.addSpawnPoint(coordinates);
		} catch (final IllegalArgumentException exception){
			sender.sendMessage(failMessage(exception.getMessage()));
			return;
		}

		sender.sendMessage(successMessage("A spawn point at " + coordinates.getX() + "/" + coordinates.getY() + "/" + coordinates.getZ() + "/" + coordinates.getYaw() + "/" + coordinates.getPitch() + " has been added."));
		this.snowbrawl.saveConfig();
	}

	/**
	 * Deletes a spawn point from an arena.
	 *
	 * @param sender The user who executed the command.
	 * @param name The name of the arena.
	 * @param id The ID of the spawn point. Obtainable
	 */
	@Command(hook = "delete_arena_spawnpoint")
	public void commandArenaConfigSpawnpointsDelete(CommandSender sender, String name, int id){
		Arena arena = this.snowbrawl.getArenaManager().getArenaByName(name);
		if (arena == null){
			sender.sendMessage(failMessage("The specified arena does not exist."));
			return;
		}

		Location spawnpoint;
		try{
			spawnpoint = arena.getSpawnPoints().get(id);
		} catch (final IndexOutOfBoundsException exception){
			sender.sendMessage(failMessage("The specified spawn point ID does not exist."));
			return;
		}

		arena.removeSpawnPoint(spawnpoint);
		sender.sendMessage(successMessage("The spawn point at " + spawnpoint.getX() + "/" + spawnpoint.getY() + "/" + spawnpoint.getZ() + "/" + spawnpoint.getYaw() + "/" + spawnpoint.getPitch() + " has been deleted."));
		this.snowbrawl.saveConfig();
	}

	/**
	 * Teleports to a random spawn point within an arena.
	 *
	 * @param sender The user who executed the command.
	 * @param name The name of the arena.
	 */
	@Command(hook = "go_to_arena")
	public void commandArenaTeleport(CommandSender sender, String name){
		Arena arena = this.snowbrawl.getArenaManager().getArenaByName(name);
		if (arena == null){
			sender.sendMessage(failMessage("The specified arena does not exist."));
			return;
		}

		((Player) sender).teleport(arena.getRandomSpawnPoint()); // CommandManager blocks console from executing this command, so this should never fail.
	}
}
