/*
 * Copyright 2022 Logan Fick
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at: https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package dev.logal.snowbrawl;

import dev.logal.snowbrawl.managers.ArenaManager;
import dev.logal.snowbrawl.managers.BossBarManager;
import dev.logal.snowbrawl.managers.MatchManager;
import dev.logal.snowbrawl.types.Arena;
import net.nemez.cmdmgr.CommandManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class Snowbrawl extends JavaPlugin {
	private static final String COMMAND_FILE_PATH = "/commands.cmd";

	private ArenaManager arenaManager;
	private MatchManager matchManager;
	private BossBarManager bossBarManager;

	private int matchManagerTickTaskID = -1;

	@Override
	public void onEnable(){
		this.loadConfig();

		this.getLogger().info("Starting match manager...");
		this.bossBarManager = new BossBarManager(this);
		this.matchManager = new MatchManager(this);
		this.matchManagerTickTaskID = this.getServer().getScheduler().scheduleSyncRepeatingTask(this, this.matchManager, 1, 1);

		this.getServer().getPluginManager().registerEvents(new EventsHandler(this), this);

		this.getLogger().info("Registering commands...");
		CommandManager.enable(this, CommandManager.VERSION);
		CommandManager.registerCommands(getClass().getResourceAsStream(COMMAND_FILE_PATH), new CommandHandler(this), this);
	}

	@Override
	public void onDisable(){
		this.getLogger().info("Unregistering commands...");
		CommandManager.disable(this);

		this.getLogger().info("Stopping match manager...");
		this.getServer().getScheduler().cancelTask(this.matchManagerTickTaskID);
		this.matchManager = null;

		this.getLogger().info("Cleaning up...");
		this.getBossBarManager().clearAllBossBars();
		this.bossBarManager = null;

		this.saveConfig();
	}

	/**
	 * Saves all arenas and their attributes to the plugin's configuration file.
	 */
	@Override
	public void saveConfig(){
		this.getLogger().info("Saving configuration file...");

		// Most likely does nothing since the configuration file should exist by this point.
		this.saveDefaultConfig();

		// Create a new configuration file to ensure _everything_ is overridden.
		YamlConfiguration newConfig = new YamlConfiguration();

		// Dump arena configuration variables.
		for (int id = 0; id < this.arenaManager.getArenas().size(); id++){
			Arena arena = this.arenaManager.getArenas().get(id);

			newConfig.set("arenas." + id + ".enabled", arena.isEnabled());
			newConfig.set("arenas." + id + ".name", arena.getName());
			newConfig.set("arenas." + id + ".subtitle", arena.getSubtitle());
			newConfig.set("arenas." + id + ".refill", arena.getRefill());
			newConfig.set("arenas." + id + ".playerLimit", arena.getPlayerLimit());
			newConfig.set("arenas." + id + ".graceTime", arena.getGraceTime());
			newConfig.set("arenas." + id + ".matchTime", arena.getMatchTime());
			newConfig.set("arenas." + id + ".damage", arena.getDamage());
			newConfig.set("arenas." + id + ".regen", arena.getRegen());
			newConfig.set("arenas." + id + ".mobspawningallowed", arena.isMobSpawningAllowed());
			newConfig.set("arenas." + id + ".world", arena.getWorld().getName());

			newConfig.set("arenas." + id + ".pos1.x", arena.getPos1().getX());
			newConfig.set("arenas." + id + ".pos1.y", arena.getPos1().getY());
			newConfig.set("arenas." + id + ".pos1.z", arena.getPos1().getZ());
			newConfig.set("arenas." + id + ".pos2.x", arena.getPos2().getX());
			newConfig.set("arenas." + id + ".pos2.y", arena.getPos2().getY());
			newConfig.set("arenas." + id + ".pos2.z", arena.getPos2().getZ());

			for (int spawnPointId = 0; spawnPointId < arena.getSpawnPoints().size(); spawnPointId++){
				Location spawnPoint = arena.getSpawnPoints().get(spawnPointId);
				newConfig.set("arenas." + id + ".spawnpoints." + spawnPointId + ".x", spawnPoint.getX());
				newConfig.set("arenas." + id + ".spawnpoints." + spawnPointId + ".y", spawnPoint.getY());
				newConfig.set("arenas." + id + ".spawnpoints." + spawnPointId + ".z", spawnPoint.getZ());
				newConfig.set("arenas." + id + ".spawnpoints." + spawnPointId + ".yaw", spawnPoint.getYaw());
				newConfig.set("arenas." + id + ".spawnpoints." + spawnPointId + ".pitch", spawnPoint.getPitch());
			}
		}

		// Replace the JavaPlugin's config file with our new one, then save.
		try{
			this.getConfig().loadFromString(newConfig.saveToString());
		} catch (InvalidConfigurationException exception){
			// This shouldn't be possible as we are going directly from an export to an import, but maybe a cosmic ray hit the RAM.
			this.getLogger().severe("An impossible internal error occurred while saving the configuration file.");
			exception.printStackTrace();
		}

		super.saveConfig();
	}

	/**
	 * Loads all arenas from the configuration file and initializes the arena manager with the loaded arenas.
	 */
	public void loadConfig(){
		Logger logger = this.getLogger();
		logger.info("Loading configuration file...");

		this.saveDefaultConfig();
		FileConfiguration config = this.getConfig();

		// Load arena configuration variables.
		List<Arena> loadedArenas = new ArrayList<>();
		for (int id = 0; id < Integer.MAX_VALUE; id++){
			// An arena is considered to exist if the "name" attribute is defined. If we run into null, we probably reached the end of the ""list"".
			String name = config.getString("arenas." + id + ".name");
			if (name == null){
				break;
			}

			// We now know we have a name, the only other required variable is the world. Try to use the world in the config.
			String worldName = config.getString("arenas." + id + ".world");
			World world = Bukkit.getWorld(worldName);
			if (world == null){
				// Hmm, the world doesn't exist. Let's default to the first world Bukkit gives us.
				// TODO: This is blindly assuming at least one world exists. Is this guaranteed?
				world = Bukkit.getWorlds().get(0);
				logger.warning("The arena with name \"" + name + "\" and ID " + id + " is supposedly located in a world named \"" + worldName + "\", but that world couldn't be found. Defaulting to world named \"" + world.getName() + "\".");
			}

			// We can now safely create a world object.
			Arena arena = new Arena(name, world);

			// Finally, we populate all the configuration variables.
			// If any of these config options don't exist in the config file, then default to the defaults defined in Arena.
			arena.setEnabled(config.getBoolean("arenas." + id + ".enabled", Arena.DEFAULT_ENABLED));
			arena.setSubtitle(config.getString("arenas." + id + ".subtitle", Arena.DEFAULT_SUBTITLE));
			arena.setRefill(config.getInt("arenas." + id + ".refill", Arena.DEFAULT_REFILL));
			arena.setPlayerLimit(config.getInt("arenas." + id + ".playerLimit", Arena.DEFAULT_PLAYER_LIMIT));
			arena.setGraceTime(config.getInt("arenas." + id + ".graceTime", Arena.DEFAULT_GRACE_TIME));
			arena.setMatchTime(config.getInt("arenas." + id + ".matchTime", Arena.DEFAULT_MATCH_TIME));
			arena.setDamage(config.getInt("arenas." + id + ".damage", Arena.DEFAULT_DAMAGE));
			arena.setRegen(config.getInt("arenas." + id + ".regen", Arena.DEFAULT_REGEN));
			arena.setMobSpawningAllowed(config.getBoolean("arenas." + id + ".mobspawningallowed", Arena.DEFAULT_MOB_SPAWNING_ALLOWED));

			// The world can be null because Arena overrides it with the actual world. Only the coordinates matter.
			arena.setPos1(new Location(null, config.getDouble("arenas." + id + ".pos1.x", Arena.DEFAULT_POS1_X), config.getDouble("arenas." + id + ".pos1.y", Arena.DEFAULT_POS1_Y), config.getDouble("arenas." + id + ".pos1.z", Arena.DEFAULT_POS1_Z)));
			arena.setPos2(new Location(null, config.getDouble("arenas." + id + ".pos2.x", Arena.DEFAULT_POS2_X), config.getDouble("arenas." + id + ".pos2.y", Arena.DEFAULT_POS2_Y), config.getDouble("arenas." + id + ".pos2.z", Arena.DEFAULT_POS2_Z)));

			for (int spawnPointId = 0; spawnPointId < Integer.MAX_VALUE; spawnPointId++){
				// config.getDouble returns 0 if the option doesn't exist, which could be a valid coordinate.
				// To test if this spawn exists, first load it as a String and test for null.
				String xTest = config.getString("arenas." + id + ".spawnpoints." + spawnPointId + ".x");

				if (xTest == null){
					break;
				}

				arena.addSpawnPoint(new Location(arena.getWorld(), config.getDouble("arenas." + id + ".spawnpoints." + spawnPointId + ".x"), config.getDouble("arenas." + id + ".spawnpoints." + spawnPointId + ".y"), config.getDouble("arenas." + id + ".spawnpoints." + spawnPointId + ".z"), ((float) config.getDouble("arenas." + id + ".spawnpoints." + spawnPointId + ".yaw")), ((float) config.getDouble("arenas." + id + ".spawnpoints." + spawnPointId + ".pitch"))));
			}

			loadedArenas.add(arena);
		}

		this.arenaManager = new ArenaManager(this, loadedArenas);
	}

	/**
	 * Gets the arena manager used by this instance of Snowbrawl.
	 *
	 * @return The ArenaManager.
	 */
	public ArenaManager getArenaManager(){
		return this.arenaManager;
	}

	/**
	 * Gets the match manager used by this instance of Snowbrawl.
	 *
	 * @return The MatchManager.
	 */
	public MatchManager getMatchManager(){
		return this.matchManager;
	}

	/**
	 * Gets the boss bar manager used by this instance of Snowbrawl.
	 *
	 * @return The MatchManager.
	 */
	public BossBarManager getBossBarManager(){
		return this.bossBarManager;
	}
}
