/*
 * Copyright 2022 Logan Fick
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at: https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package dev.logal.snowbrawl.managers;

import dev.logal.snowbrawl.Snowbrawl;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;

/**
 * Keeps track of boss bars on a per-player basis.
 */
public class BossBarManager implements Listener {
	private final Snowbrawl snowbrawl;

	private final HashMap<Player, BossBar> bossBars = new HashMap<>();

	/**
	 * Creates a new boss bar manager which is owned by a given instance of Snowbrawl.
	 *
	 * @param snowbrawl The instance of Snowbrawl which owns this arena manager. Used to configure boss bars on the Minecraft server itself and register events.
	 */
	public BossBarManager(final Snowbrawl snowbrawl){
		this.snowbrawl = snowbrawl;
		this.snowbrawl.getServer().getPluginManager().registerEvents(this, this.snowbrawl);
	}

	/**
	 * Creates a boss at the top of a given player's GUI with a given text, color, style, and progress level.
	 * If the player already has a boss bar, the attributes are overwritten.
	 *
	 * @param player   The player whose boss bar to set.
	 * @param text     The text which appears above the boss bar.
	 * @param color    The color of the boss bar.
	 * @param style    The visual style of the boss bar.
	 * @param progress A value between 0.0 and 1.0 representing the boss bar's progress.
	 */
	public void setBossBar(final Player player, final String text, final BarColor color, final BarStyle style, final double progress){
		if (this.bossBars.containsKey(player)){
			BossBar bossBar = this.bossBars.get(player);
			bossBar.setTitle(text);
			bossBar.setColor(color);
			bossBar.setStyle(style);
			bossBar.setProgress(progress);
		} else {
			BossBar bossBar = this.snowbrawl.getServer().createBossBar(text, color, style);
			bossBar.setProgress(progress);
			bossBar.addPlayer(player);
			this.bossBars.put(player, bossBar);
		}
	}

	/**
	 * Removes the boss bar from the top of a given player's GUI, if one exists.
	 *
	 * @param player The player whose boss bar to clear.
	 */
	public void clearBossBar(final Player player){
		if (bossBars.containsKey(player)){
			this.bossBars.get(player).removeAll();
			this.bossBars.remove(player);
		}
	}

	/**
	 * Clears all boss bars owned by this boss bar manager.
	 */
	public void clearAllBossBars(){
		// TODO: There was probably a ConcurrentModificationException caused here during a server shut down at one point.
		for (Player player : this.bossBars.keySet()){
			this.clearBossBar(player);
		}
	}

	/**
	 * Cleans up any boss bars on a player's GUI when they leave.
	 * This ensures a clean slate the next time they log in.
	 *
	 * @param event The event.
	 */
	@EventHandler
	public void onPlayerLeave(final PlayerQuitEvent event){
		this.clearBossBar(event.getPlayer());
	}
}
