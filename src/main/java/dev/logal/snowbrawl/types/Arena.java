/*
 * Copyright 2022 Logan Fick
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at: https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package dev.logal.snowbrawl.types;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Represents an arena in a minecraft world and holds all of its attributes.
 */
public class Arena {
	// Default config variables
	public static final int DEFAULT_REFILL = 16;
	public static final int DEFAULT_PLAYER_LIMIT = 8;
	public static final int DEFAULT_GRACE_TIME = 15 * 20;
	public static final int DEFAULT_MATCH_TIME = 300 * 20;
	public static final int DEFAULT_DAMAGE = 4;
	public static final double DEFAULT_POS1_X = 0, DEFAULT_POS1_Y = 0, DEFAULT_POS1_Z = 0;
	public static final double DEFAULT_POS2_X = 0, DEFAULT_POS2_Y = 0, DEFAULT_POS2_Z = 0;

	private final ArrayList<Location> spawnPoints = new ArrayList<>(); // A list of locations identifying spawn locations.
	// Runtime variables
	private final Random rng = new Random();
	// Config variables
	private String name; // A friendly name for this arena
	private int refill; // Amount of snowballs to give players per snow block right click.
	private int playerLimit; // Maximum amount of players allowed in this arena in a match.
	private int graceTime, matchTime;
	private int damage; // Damage dealt to players per snowball hit.
	private World world; // Which world the coordinates apply to.
	private Location pos1, pos2; // Coordinates identifying the 2 corners of the arena.


	/**
	 * Creates a new arena with a given name in a given world and sets all other attributes to the below defaults:
	 * <ul>
	 * <li>refill = Arena.DEFAULT_REFILL</li>
	 * <li>playerLimit = Arena.DEFAULT_PLAYER_LIMIT</li>
	 * <li>graceTime = Arena.DEFAULT_GRACE_TIME</li>
	 * <li>matchTime = Arena.DEFAULT_MATCH_TIME</li>
	 * <li>damage = Arena.DEFAULT_DAMAGE</li>
	 * <li>pos1 = new Location(world, 0, 0, 0)</li>
	 * <li>pos2 = new Location(world, 0, 0, 0)</li>
	 * <li>spawnPoints = new ArrayList<>();</li>
	 * </ul>
	 */
	public Arena(final String name, final World world){
		this.setName(name);

		this.setRefill(DEFAULT_REFILL);
		this.setPlayerLimit(DEFAULT_PLAYER_LIMIT);
		this.setGraceTime(DEFAULT_GRACE_TIME);
		this.setMatchTime(DEFAULT_MATCH_TIME);
		this.setDamage(DEFAULT_DAMAGE);

		this.setWorld(world);
		this.setPos1(new Location(this.getWorld(), 0, 0, 0));
		this.setPos2(new Location(this.getWorld(), 0, 0, 0));
	}

	/**
	 * Gets the friendly name for this arena.
	 *
	 * @return The friendly name.
	 */
	public String getName(){
		return this.name;
	}

	/**
	 * Changes the friendly name for this arena.
	 * All spaces are converted to underscores to ensure the name can be used in commands.
	 *
	 * @param name The new friendly name.
	 */
	public void setName(final String name){
		if (name == null || name.isEmpty()){
			throw new IllegalArgumentException("The name must be a non-null and non-empty String.");
		}

		this.name = name.replaceAll(" ", "_");
	}

	/**
	 * Gets the amount of snowballs given to players each time they click a snow block to refill their snowballs.
	 *
	 * @return The amount of snowballs given to players.
	 */
	public int getRefill(){
		return this.refill;
	}

	/**
	 * Sets the amount of snowballs given to players each time they click a snow block to refill their snowballs.
	 *
	 * @param refill The amount of snowballs to give to players.
	 */
	public void setRefill(final int refill){
		if (refill < 1){
			throw new IllegalArgumentException("The refill amount must be at least 1.");
		}

		this.refill = refill;
	}

	/**
	 * Gets the maximum amount of players allowed to play on this arena.
	 *
	 * @return The maximum amount of players.
	 */
	public int getPlayerLimit(){
		return this.playerLimit;
	}

	/**
	 * Sets the maximum amount of players allowed to play on this arena.
	 *
	 * @param playerLimit The maximum amount of players.
	 */
	public void setPlayerLimit(final int playerLimit){
		if (playerLimit < 2){
			throw new IllegalArgumentException("The player limit must be at least 2.");
		}

		this.playerLimit = playerLimit;
	}

	/**
	 * Gets the amount of time players are allowed to safely explore the map before the match begins in this arena.
	 *
	 * @return The grace time, in ticks.
	 */
	public int getGraceTime(){
		return this.graceTime;
	}

	/**
	 * Sets the amount of time players are allowed to safely explore the map before the match begins in this arena.
	 *
	 * @param graceTime The grace time, in ticks.
	 */
	public void setGraceTime(final int graceTime){
		if (graceTime < (5 * 20)){
			throw new IllegalArgumentException("The grace time must be at least 5 seconds.");
		}

		this.graceTime = graceTime;
	}

	/**
	 * Gets the amount of time a match lasts in this arena.
	 *
	 * @return The match time, in ticks.
	 */
	public int getMatchTime(){
		return this.matchTime;
	}

	/**
	 * Sets the amount of time a match lasts in this arena.
	 *
	 * @param matchTime The match time, in ticks.
	 */
	public void setMatchTime(final int matchTime){
		if (matchTime < (5 * 20)){
			throw new IllegalArgumentException("The match time must be at least 5 seconds.");
		}

		this.matchTime = matchTime;
	}

	/**
	 * Gets the amount of damage each snowball hit does to a player in this arena.
	 *
	 * @return The amount of damage done by each snowball.
	 */
	public int getDamage(){
		return this.damage;
	}

	/**
	 * Sets the amount of damage each snowball hit does to a player in this arena.
	 *
	 * @param damage The amount of damage done by each snowball.
	 */
	public void setDamage(final int damage){
		if (damage < 1){
			throw new IllegalArgumentException("The damage must be at least 1.");
		}

		this.damage = damage;
	}

	/**
	 * Gets the position of one corner of the bounding box of this arena.
	 *
	 * @return A Location representing one corner.
	 */
	public Location getPos1(){
		return this.pos1.clone();
	}

	/**
	 * Sets the position of one corner of the bounding box of this arena.
	 *
	 * @param pos1 A Location representing one corner.
	 */
	public void setPos1(final Location pos1){
		if (this.spawnPoints.size() > 0){
			throw new IllegalStateException("The arena bounding box cannot be changed with spawn points set.");
		}

		// Get a rid of any information about the world and store only the coordinates.
		// The world is tracked separately and yaw/pitch doesn't matter.
		this.pos1 = new Location(null, pos1.getX(), pos1.getY(), pos1.getZ());
	}

	/**
	 * Gets the position of the opposite corner of the bounding box of this arena.
	 *
	 * @return A Location representing one corner.
	 */
	public Location getPos2(){
		return this.pos2.clone();
	}

	/**
	 * Sets the position of the opposite corner of the bounding box of this arena.
	 *
	 * @param pos1 A Location representing one corner.
	 */
	public void setPos2(final Location pos1){
		if (this.spawnPoints.size() > 0){
			throw new IllegalStateException("The arena bounding box cannot be changed with spawn points set.");
		}

		// Get a rid of any information about the world and store only the coordinates.
		// The world is tracked separately and yaw/pitch doesn't matter.
		this.pos2 = new Location(null, pos1.getX(), pos1.getY(), pos1.getZ());
	}

	/**
	 * Gets the world this arena is located in.
	 *
	 * @return The world.
	 */
	public World getWorld(){
		return this.world;
	}

	/**
	 * Sets the world this arena is located in.
	 *
	 * @param world The world.
	 */
	public void setWorld(final World world){
		this.world = world;
	}

	/**
	 * Adds a new spawn point to this arena.
	 *
	 * @param spawnPoint A Location representing the new spawn point.
	 */
	public void addSpawnPoint(final Location spawnPoint){
		if (!this.isWithinBoundingBox(spawnPoint)){
			throw new IllegalArgumentException("The specified spawn point is outside the bounds of this arena.");
		}

		this.spawnPoints.add(new Location(this.getWorld(), spawnPoint.getX(), spawnPoint.getY(), spawnPoint.getZ(), spawnPoint.getYaw(), spawnPoint.getPitch()));
	}

	/**
	 * Removes a spawn point from this arena.
	 *
	 * @param spawnPoint The spawn point to remove from this arena.
	 */
	public void removeSpawnPoint(final Location spawnPoint){
		this.spawnPoints.remove(spawnPoint);
	}

	/**
	 * Gets a copy of all spawn points for this arena.
	 *
	 * @return A list of Locations representing the spawn points for this arena.
	 */
	public List<Location> getSpawnPoints(){
		return new ArrayList<>(this.spawnPoints); // Return a copy rather than the reference to the original list.
	}

	/**
	 * Checks if a given location is within the bounding box of this arena.
	 *
	 * @param location The location to check.
	 *
	 * @return True if the location is within the bounding box, false otherwise.
	 */
	public boolean isWithinBoundingBox(Location location){
		if (location.getWorld() != this.getWorld()){
			return false;
		}

		double highX = Math.max(pos1.getX(), pos2.getX()); // Highest X coordinate.
		double lowX = Math.min(pos1.getX(), pos2.getX()); // Lowest X coordinate.
		double highY = Math.max(pos1.getY(), pos2.getY()); // Highest Y coordinate.
		double lowY = Math.min(pos1.getY(), pos2.getY()); // Lowest Y coordinate.
		double highZ = Math.max(pos1.getZ(), pos2.getZ()); // Highest Y coordinate.
		double lowZ = Math.min(pos1.getZ(), pos2.getZ()); // Lowest Y coordinate.

		if (location.getX() > highX){
			return false;
		} else if (location.getX() < lowX){
			return false;
		} else if (location.getY() > highY){
			return false;
		} else if (location.getY() < lowY){
			return false;
		} else if (location.getZ() > highZ){
			return false;
		} else {
			return !(location.getZ() < lowZ);
		}
	}

	/**
	 * Gets a random spawn point for this arena. If no spawn points are configured, then the exact center of the arena is returned instead.
	 *
	 * @return A Location representing the randomly selected spawnpoint.
	 */
	public Location getRandomSpawnPoint(){
		Location destination;
		if (this.spawnPoints.size() > 0){
			// If spawn points are configured, teleport the player to a random spawn point.
			destination = this.spawnPoints.get(this.rng.nextInt(0, spawnPoints.size()));
		} else {
			// If no spawn points are configured, teleport the player to the center of the bounding box.
			double x = (pos1.getX() + pos2.getX()) / 2;
			double y = (pos1.getY() + pos2.getY()) / 2;
			double z = (pos1.getZ() + pos2.getZ()) / 2;

			destination = new Location(this.getWorld(), x, y, z);
		}

		return destination;
	}
}
