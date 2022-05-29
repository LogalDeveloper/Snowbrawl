/*
 * Copyright 2022 Logan Fick
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at: https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package dev.logal.snowbrawl.types;

import org.bukkit.entity.Player;

/**
 * Represents a player participating in a match.
 */
public class MatchParticipant {
	private final Player player;
	private final Match match;
	private int kills = 0, deaths = 0;

	/**
	 * Creates a new match participant with a given player participating in a given match.
	 *
	 * @param player The player.
	 * @param match The match they are participating in.
	 */
	public MatchParticipant(final Player player, final Match match){
		this.player = player;
		this.match = match;
	}

	/**
	 * Increments this match participant's kill count.
	 */
	public void awardKill(){
		this.kills++;
	}

	/**
	 * Increments this match participants death count.
	 */
	public void awardDeath(){
		this.deaths++;
	}

	/**
	 * Gets the current amount of kills this match participant has.
	 *
	 * @return The number of kills.
	 */
	public int getKills(){
		return this.kills;
	}

	/**
	 * Gets the current amount of deaths this match participant has.
	 *
	 * @return The number of deaths.
	 */
	public int getDeaths(){
		return this.deaths;
	}

	/**
	 * Gets the current score for this player. Calculated by subtracing deaths from kills.
	 *
	 * @return The score.
	 */
	public int getScore(){
		return this.getKills() - this.getDeaths();
	}

	/**
	 * Gets the player represented by this match participant.
	 *
	 * @return The player.
	 */
	public Player getPlayer(){
		return this.player;
	}

	/**
	 * Gets the match this match participant is participating in.
	 *
	 * @return The match.
	 */
	public Match getMatch(){
		return this.match;
	}
}
