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
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.spigotmc.event.entity.EntityMountEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Keeps track of Arenas and establishes default mechanics for activities inside of arenas.
 */
public class ArenaManager implements Listener {
	private final Snowbrawl snowbrawl;

	private final List<Arena> arenas = new ArrayList<>(); // Holds registered arenas.
	private final HashMap<Player, Arena> playersInArenas = new HashMap<>(); // Keeps track of which arena a given player is currently in.
	private final Random rng = new Random();

	/**
	 * Creates a new Arena Manager which is owned by a given instance of Snowbrawl and pre-registers a given array of given Arenas.
	 *
	 * @param snowbrawl     The instance of Snowbrawl which owns this arena manager. Used for logging, save the configuration file, and registering events.
	 * @param initialArenas A list of Arenas to pre-register.
	 */
	public ArenaManager(final Snowbrawl snowbrawl, final List<Arena> initialArenas){
		this.snowbrawl = snowbrawl;
		for (Arena arena : initialArenas){
			snowbrawl.getLogger().info("Arena loaded.");
			logArenaInfo(arena);
			this.arenas.add(arena);
		}
		this.snowbrawl.getServer().getPluginManager().registerEvents(this, this.snowbrawl);
	}

	/**
	 * Adds an arena to the pool of arenas.
	 *
	 * @param arena The arena to register.
	 */
	public void registerArena(final Arena arena){
		if (this.getArenaByName(arena.getName()) != null){
			throw new IllegalArgumentException("The specified arena name is already in use.");
		}

		this.snowbrawl.getLogger().info("Arena registered.");
		this.logArenaInfo(arena);
		this.arenas.add(arena);

		this.snowbrawl.saveConfig();
	}

	/**
	 * Removes an arena from the pool of arenas.
	 *
	 * @param arena The arena to unregister.
	 */
	public void unregisterArena(final Arena arena){
		if (arenas.remove(arena)){
			this.snowbrawl.getLogger().info("Arena unregistered.");
			this.logArenaInfo(arena);
			this.snowbrawl.saveConfig();
		}
	}

	/**
	 * Gets a copy of all known arenas.
	 *
	 * @return A List of all known Arenas.
	 */
	public List<Arena> getArenas(){
		return new ArrayList<>(this.arenas); // Return a copy rather than the reference to the original list.
	}

	/**
	 * Gets an arena by its name.
	 *
	 * @param name The name of the arena to search for. Case-insensitive.
	 *
	 * @return The Arena with the given name if found, null otherwise.
	 */
	public Arena getArenaByName(final String name){
		if (name != null){
			for (Arena arena : this.getArenas()){
				if (arena.getName().equalsIgnoreCase(name)){
					return arena;
				}
			}
		}

		return null;
	}

	/**
	 * Gets the first arena whose bounding box contains the given location.
	 *
	 * @param location The location to test for.
	 *
	 * @return The arena which contains the given location, null if none.
	 */
	public Arena getArenaByLocation(final Location location){
		if (location != null){
			for (Arena arena : this.getArenas()){
				if (arena.isWithinBoundingBox(location)){
					return arena;
				}
			}
		}

		return null;
	}

	/*
	 * These event handlers establish the default gameplay mechanics of activity which occurs inside the bounding box of arenas.
	 *
	 * These defaults are very strict and prevent most activities from occurring in order to preserve the integrity of the arena.
	 * These defaults also implement most of the core mechanics of Snowbrawl, like making snowballs explode.
	 *
	 * Most restrictions here can be bypassed by simply being an operator, so they can build and modify arenas.
	 * It is intended for the match manager to implement the same restrictions on operators who are in matches.
	 */

	/**
	 * Prevents blocks from burning insides of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockBurn(final BlockBurnEvent event){
		final Arena arena = this.getArenaByLocation(event.getBlock().getLocation());

		if (arena != null){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents non-operators from building inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockCanBuild(final BlockCanBuildEvent event){
		final Arena arena = this.getArenaByLocation(event.getBlock().getLocation());
		final Player player = event.getPlayer();

		if (arena != null && (player == null || !player.isOp())){
			event.setBuildable(false);
		}
	}

	/**
	 * Prevents non-operators from damaging blocks inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockDamage(final BlockDamageEvent event){
		final Arena arena = this.getArenaByLocation(event.getBlock().getLocation());
		final Player player = event.getPlayer();

		if (arena != null && !player.isOp()){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents blocks from creating item drops inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockDropItem(final BlockDropItemEvent event){
		final Arena arena = this.getArenaByLocation(event.getBlock().getLocation());

		if (arena != null){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents blocks from creating experience orbs inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockExp(final BlockExpEvent event){
		final Arena arena = this.getArenaByLocation(event.getBlock().getLocation());

		if (arena != null){
			event.setExpToDrop(0);
		}
	}

	/**
	 * Prevents blocks from exploding inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockExplode(final BlockExplodeEvent event){
		final Arena arena = this.getArenaByLocation(event.getBlock().getLocation());

		if (arena != null){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents blocks from fading inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockFade(final BlockFadeEvent event){
		final Arena arena = this.getArenaByLocation(event.getBlock().getLocation());

		if (arena != null){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents non-operators from fertilizing blocks inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockFertilize(final BlockFertilizeEvent event){
		final Arena arena = this.getArenaByLocation(event.getBlock().getLocation());
		final Player player = event.getPlayer();

		if (arena != null && (player == null || !player.isOp())){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents crops from naturally growing inside of arenas.
	 * Operators can manually grow crops and the amount of growth will be frozen.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockGrow(final BlockGrowEvent event){
		final Arena arena = this.getArenaByLocation(event.getBlock().getLocation());

		if (arena != null){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents non-operators from igniting blocks inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockIgnite(final BlockIgniteEvent event){
		final Arena arena = this.getArenaByLocation(event.getBlock().getLocation());
		final Player player = event.getPlayer();

		if (arena != null && (player == null || !player.isOp())){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents non-operators from placing blocks inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockPlace(final BlockPlaceEvent event){
		final Arena arena = this.getArenaByLocation(event.getBlock().getLocation());
		final Player player = event.getPlayer();

		if (arena != null && !player.isOp()){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents blocks from receiving game events inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockReceiveGame(final BlockReceiveGameEvent event){
		final Arena arena = this.getArenaByLocation(event.getBlock().getLocation());

		if (arena != null){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents blocks from shearing entities inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockShearEntity(final BlockShearEntityEvent event){
		final Arena arena = this.getArenaByLocation(event.getBlock().getLocation());

		if (arena != null){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents anything except operators from changing cauldron levels.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onCauldronLevelChange(final CauldronLevelChangeEvent event){
		final Arena arena = this.getArenaByLocation(event.getBlock().getLocation());

		if (arena == null){
			return;
		}

		final Entity entity = event.getEntity();
		if (entity instanceof final Player player){
			if (!player.isOp()){
				event.setCancelled(true);
			}
		} else {
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents leaves from naturally decaying inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onLeavesDecay(final LeavesDecayEvent event){
		final Arena arena = this.getArenaByLocation(event.getBlock().getLocation());

		if (arena != null){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents non-operators from changing signs inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onSignChange(final SignChangeEvent event){
		final Arena arena = this.getArenaByLocation(event.getBlock().getLocation());
		final Player player = event.getPlayer();

		if (arena != null && !player.isOp()){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents entities from changing blocks inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onEntityChangeBlock(final EntityChangeBlockEvent event){
		final Arena arena = this.getArenaByLocation(event.getBlock().getLocation());

		if (arena != null){
			event.setCancelled(true);
		}
	}

	/**
	 * Turns snowballs which hit entities inside of arenas into damage dealing explosions.
	 * All other entity damage by entity is prevented.
	 *
	 * @param event The event.
	 */
	@EventHandler
	public void onEntityDamageByEntity(final EntityDamageByEntityEvent event){
		final Arena arena = this.getArenaByLocation(event.getEntity().getLocation());

		if (arena != null){
			final Entity attacker = event.getDamager();

			// Was the entity hit directly by a snowball?
			if (attacker instanceof Snowball && event.getCause().equals(EntityDamageEvent.DamageCause.PROJECTILE)){
				// After hours of testing, it seems Minecraft is extremely inconsistent about dealing damage to an entity from a projectile spawning an explosion.
				// To try and make direct hits deal damage consistently, let's set the event damage to whatever is configured in the arena, then spawn a 0 power decorative explosion.
				// Even this doesn't seem perfectly consistent, but it's the best solution so far.
				event.setDamage(arena.getDamage());
				attacker.getWorld().createExplosion(attacker.getLocation(), 0f, false, false, attacker);
			}
		}
	}

	/**
	 * Changes drops from entities inside of arenas into snowballs.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onEntityDeath(final EntityDeathEvent event){
		final Arena arena = this.getArenaByLocation(event.getEntity().getLocation());

		if (arena != null){
			if (event.getDrops().size() > 0){ // Was this entity about to drop anything?
				event.getDrops().clear(); // Reset the drops to nothing.
				event.getDrops().add(new ItemStack(Material.SNOWBALL, this.rng.nextInt(1, 4))); // Add some snowballs.
			}
			event.setDroppedExp(0); // Ensure no experience orbs get spawned.
		}
	}

	/**
	 * Prevents entities from dropping items inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onEntityDropItem(final EntityDropItemEvent event){
		final Arena arena = this.getArenaByLocation(event.getEntity().getLocation());

		if (arena != null){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents entities from entering blocks inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onEntityEnterBlock(final EntityEnterBlockEvent event){
		final Arena blockArena = this.getArenaByLocation(event.getBlock().getLocation());
		if (blockArena != null){
			event.setCancelled(true);
			return;
		}

		final Arena entityArena = this.getArenaByLocation(event.getEntity().getLocation());
		if (entityArena != null){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents entities from interacting with blocks inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onEntityInteract(final EntityInteractEvent event){
		final Arena blockArena = this.getArenaByLocation(event.getBlock().getLocation());
		if (blockArena != null){
			event.setCancelled(true);
			return;
		}

		final Arena entityArena = this.getArenaByLocation(event.getEntity().getLocation());
		if (entityArena != null){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents entities from mounting entities inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onEntityMount(final EntityMountEvent event){
		final Arena entityArena = this.getArenaByLocation(event.getEntity().getLocation());
		if (entityArena != null){
			event.setCancelled(true);
			return;
		}

		final Arena mountArena = this.getArenaByLocation(event.getMount().getLocation());
		if (mountArena != null){
			event.setCancelled(true);
		}
	}

	/**
	 * Except for operators, prevents entities from picking up items other than snowballs on the ground.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onEntityPickupItem(final EntityPickupItemEvent event){
		final Entity entity = event.getEntity();
		final Arena arena = this.getArenaByLocation(entity.getLocation());

		if (arena != null){ // Is the entity picking up the item in an arena?
			if (entity instanceof final Player player){ // Is the entity a player?
				if (!player.isOp()){ // Is that player not an operator?
					if (!event.getItem().getItemStack().getType().equals(Material.SNOWBALL)){ // Is the item not a snowball?
						event.setCancelled(true);
					}
				}
			} else {
				event.setCancelled(true);
			}
		}
	}

	/**
	 * Prevents non-operators from placing entities from items.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onEntityPlace(final EntityPlaceEvent event){
		final Player player = event.getPlayer();
		final Arena entityArena = this.getArenaByLocation(player.getLocation()); // getLocation may be null, but getArenaByLocation will simply pass it through.
		final Arena blockArena = this.getArenaByLocation(event.getBlock().getLocation());

		if (entityArena != null || blockArena != null){ // Is the player placing the entity from or in an arena?
			if (!player.isOp()){ // Is that player also not an operator?
				event.setCancelled(true);
			}
		}
	}

	/**
	 * Converts any arrows shot inside of arenas into snowballs.
	 *
	 * @param event The event.
	 */
	@EventHandler
	public void onEntityShootBow(final EntityShootBowEvent event){
		final Entity entity = event.getEntity();
		final Arena arena = this.getArenaByLocation(entity.getLocation());

		if (arena != null){ // Is the entity shooting from inside an arena?
			final Entity arrow = event.getProjectile(); // Get the arrow which was about to be shot.
			final Entity snowball = entity.getWorld().spawnEntity(arrow.getLocation(), EntityType.SNOWBALL); // Spawn a new snowball at the arrow's location.

			// TODO: This is not enough info to transfer information about the shooter. Dying entities only know they were killed by Snowball.
			snowball.setVelocity(arrow.getVelocity()); // Copy the arrow's velocity to the new snowball.
			snowball.setLastDamageCause(arrow.getLastDamageCause()); // Carry over damage cause from the arrow to the snowball.

			event.setProjectile(snowball); // Change the projectile to the snowball.
		}
	}

	/**
	 * Prevents entities from spawning inside of arenas with mob spawning set to disabled.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onEntitySpawn(final EntitySpawnEvent event){
		final Arena arena = this.getArenaByLocation(event.getEntity().getLocation());
		if (arena != null && !arena.isMobSpawningAllowed()){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents entities inside of arenas from casting spells.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onEntitySpellCast(final EntitySpellCastEvent event){
		final Arena arena = this.getArenaByLocation(event.getEntity().getLocation());
		if (arena != null){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents entities inside of arenas from being tamed.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onEntityTame(final EntityTameEvent event){
		final Arena arena = this.getArenaByLocation(event.getEntity().getLocation());
		if (arena != null){
			event.setCancelled(true);
		}
	}

	/**
	 * Creates an explosion when a snowball hits a block.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onProjectileHit(final ProjectileHitEvent event){
		Projectile projectile = event.getEntity();

		if (projectile instanceof Snowball){ // Is the projectile a snowball?
			Block hitBlock = event.getHitBlock();
			if (hitBlock != null && this.getArenaByLocation(hitBlock.getLocation()) != null){ // Did the snowball hit a block inside an arena?
				projectile.getWorld().createExplosion(projectile.getLocation(), 1f, false, false, projectile); // Spawn an explosion at the impact location.
			}
		}
	}

	/**
	 * Prevents non-operators from entering beds.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onPlayerBedEnter(final PlayerBedEnterEvent event){
		final Arena arena = this.getArenaByLocation(event.getBed().getLocation());
		final Player player = event.getPlayer();

		if (arena != null && !player.isOp()){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents non-operators from capturing entities inside of buckets.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onPlayerBucketEntity(final PlayerBucketEntityEvent event){
		final Arena arena = this.getArenaByLocation(event.getEntity().getLocation());
		final Player player = event.getPlayer();

		if (arena != null && !player.isOp()){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents non-operators from dropping items.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onPlayerDropItem(final PlayerDropItemEvent event){
		final Player player = event.getPlayer();
		final Arena arena = this.getArenaByLocation(player.getLocation());

		if (arena != null && !player.isOp()){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents non-operators from editing books inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onPlayerEditBook(final PlayerEditBookEvent event){
		final Player player = event.getPlayer();
		final Arena arena = this.getArenaByLocation(player.getLocation());

		if (arena != null && !player.isOp()){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents non-operators from fishing inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onPlayerFish(final PlayerFishEvent event){
		final Player player = event.getPlayer();
		final Arena arena = this.getArenaByLocation(player.getLocation());

		if (arena != null && !player.isOp()){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents non-operators from harvesting blocks inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onPlayerHarvestBlock(final PlayerHarvestBlockEvent event){
		final Arena arena = this.getArenaByLocation(event.getHarvestedBlock().getLocation());
		final Player player = event.getPlayer();

		if (arena != null && !player.isOp()){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents non-operators from interacting with entities inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteractEntity(final PlayerInteractEntityEvent event){
		final Arena arena = this.getArenaByLocation(event.getRightClicked().getLocation());
		final Player player = event.getPlayer();

		if (arena != null && !player.isOp()){
			event.setCancelled(true);
		}
	}

	/**
	 * Makes right-clicking snow blocks inside of arenas grant snowballs in the first hotbar slot.
	 *
	 * @param event The event.
	 */
	@EventHandler
	public void onPlayerInteract(final PlayerInteractEvent event){
		final Player player = event.getPlayer();
		final Arena arena = this.getArenaByLocation(player.getLocation());

		if (arena != null){
			// TODO: getClickedBlock doesn't only mean it was right clicked. It could also have been destroyed. This causes a bug where operators cannot delete snow blocks.
			final Block rightClickedBlock = event.getClickedBlock();
			if (rightClickedBlock != null && rightClickedBlock.getType().equals(Material.SNOW_BLOCK)){
				event.setCancelled(true);
				PlayerInventory playerInventory = event.getPlayer().getInventory();
				if (!playerInventory.containsAtLeast(new ItemStack(Material.SNOWBALL, arena.getRefill()), arena.getRefill())){ // Does this player already have at least the refill amount of snowballs?
					playerInventory.clear();
					playerInventory.setItem(4, new ItemStack(Material.SNOWBALL, arena.getRefill())); // Set the middle hotbar slot to a new snowball stack.
					playerInventory.setHeldItemSlot(4); // For convenience, set the selected hotbar slot to the same one.
					player.updateInventory(); // Synchronize the player's inventory.
					player.playSound(player, Sound.ITEM_ARMOR_EQUIP_GENERIC, SoundCategory.MASTER, 1f, 1f); // Provide audible confirmation of the snowballs added to the inventory.

				}
			}
		}
	}

	/**
	 * Prevents non-operators from consuming items inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onPlayerItemConsume(final PlayerItemConsumeEvent event){
		final Player player = event.getPlayer();
		final Arena arena = this.getArenaByLocation(player.getLocation());

		if (arena != null && !player.isOp()){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents non-operators' items taking durability damage inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onPlayerItemDamage(final PlayerItemDamageEvent event){
		final Player player = event.getPlayer();
		final Arena arena = this.getArenaByLocation(player.getLocation());

		if (arena != null && !player.isOp()){
			event.setCancelled(true);
		}
	}

	/**
	 * Monitors player movement and displays a title screen with an arena's name if they cross into its bounding box.
	 * Also monitor's player movement and applies a buff depending on if they walk over a certain block:
	 * - Sponge: Jump Boost for 3 seconds.
	 * - Glass: Speed for 3 seconds.
	 * - Obsidian: Damage Resistance for 15 seconds.
	 *
	 * @param event The event.
	 */
	@EventHandler
	public void onPlayerMove(final PlayerMoveEvent event){
		// TODO: After a teleport, the title screen only appears after the player moves. It might be worth handling teleport events to also show title screens.
		final Player player = event.getPlayer();
		final Arena arena = this.getArenaByLocation(event.getTo());

		if (arena != null){ // Did this player move into an arena's bounding box?
			if (this.playersInArenas.get(player) != arena){ // Is the arena they entered different than the one they were last in? (Including not in an arena)
				// TODO: Add configurable arena subtitle (such as for giving credit to the arena's builders)
				player.sendTitle(ChatColor.AQUA + "" + arena.getName(), "", 10, 70, 20); // Display a title screen with the arena's name.
				this.playersInArenas.put(player, arena); // Store which arena the player is in for future checks.
			}

			Location playerPos = player.getLocation();
			Material material = player.getWorld().getBlockAt(playerPos.getBlockX(), playerPos.getBlockY() - 1, playerPos.getBlockZ()).getType();

			if (material.equals(Material.SPONGE)){
				player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 60, 0));
			} else if (material.equals(Material.GLASS)){
				// TODO: Support stained glass.
				player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0));
			} else if (material.equals(Material.OBSIDIAN)){
				player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 300, 1));
			}
		} else {
			this.playersInArenas.remove(player);
		}
	}

	/**
	 * Cleans up tracking of player movement for arena name title screens.
	 *
	 * @param event The event.
	 */
	@EventHandler
	public void onPlayerQuit(final PlayerQuitEvent event){
		this.playersInArenas.remove(event.getPlayer());
	}

	/**
	 * Prevents non-operators from discovering recipes inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onRecipeDiscover(final PlayerRecipeDiscoverEvent event){
		final Player player = event.getPlayer();
		final Arena arena = this.getArenaByLocation(player.getLocation());

		if (arena != null && !player.isOp()){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents non-operators from shearing entities inside of arenas.
	 *
	 * @param event The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onPlayerShearEntity(final PlayerShearEntityEvent event){
		final Player player = event.getPlayer();
		final Arena arena = this.getArenaByLocation(event.getEntity().getLocation());

		if (arena != null && !player.isOp()){
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents non-operators from taking books off a lectern inside of arenas.
	 *
	 * @param event The events.
	 */
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onPlayerTakeLecternBook(final PlayerTakeLecternBookEvent event){
		final Player player = event.getPlayer();
		final Arena arena = this.getArenaByLocation(event.getLectern().getLocation());

		if (arena != null && !player.isOp()){
			event.setCancelled(true);
		}
	}

	/*
	 * Private helper methods
	 */

	/**
	 * Logs all configuration variables about a given arena to console.
	 *
	 * @param arena The arena to log about.
	 */
	private void logArenaInfo(Arena arena){
		Logger logger = this.snowbrawl.getLogger();
		logger.info(" - Name: " + arena.getName());
		logger.info(" - Enabled: " + arena.isEnabled());
		logger.info(" - Refill: " + arena.getRefill() + " snowballs");
		logger.info(" - Player Limit: " + arena.getPlayerLimit() + " players");
		logger.info(" - Grace Time: " + arena.getGraceTime() + "s");
		logger.info(" - Match Time: " + arena.getMatchTime() + "s");
		logger.info(" - Damage: " + arena.getDamage() + " HP");
		logger.info(" - Mob Spawning Allowed: " + arena.isMobSpawningAllowed());
		logger.info(" - World: " + arena.getWorld().getName());

		Location pos1 = arena.getPos1(), pos2 = arena.getPos2();
		logger.info(" - Bounding Box: " + pos1.getX() + ", " + pos1.getY() + ", " + pos1.getZ() + " / " + pos2.getX() + ", " + pos2.getY() + ", " + pos2.getZ());
		logger.info(" - Spawn Points: " + arena.getSpawnPoints().size());
	}
}
