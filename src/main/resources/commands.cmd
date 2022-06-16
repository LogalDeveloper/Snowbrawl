command snowbrawl {
	alias sb;

	[empty] {
		run join_queue;
		type player;
		perm redstoner.snowbrawl;
	}

	arena list {
		perm redstoner.snowbrawl.arena.list;
		run get_arenas;
		type all;
		help Lists all known arenas.;
	}

	arena create [string:name] {
	   perm redstoner.snowbrawl.arena.create;
		run create_arena name;
		type player;
		help Creates a new arena.;
	}

	arena create [string:name] [string:world]{
		perm redstoner.snowbrawl.arena.create;
		run create_arena_in_specific_world name world;
		type all;
		help Creates a new arena in a specific world.;
	}

	arena [string:name] delete {
		perm redstoner.snowbrawl.arena.delete;
		run delete_arena name;
		type all;
		help Deletes an arena.;
	}

	arena [string:name] enable {
		perm redstoner.snowbrawl.arena.enable;
		run enable_arena name;
		type all;
		help Enables an arena.;
	}

	arena [string:name] disable {
		perm redstoner.snowbrawl.arena.disable;
		run disable_arena name;
		type all;
		help Disables an arena.;
	}

	arena [string:name] config {
		run get_arena_config name;
		type all;
		help Prints all configuration variable values for an arena.;

		name [string:new_name] {
			perm redstoner.snowbrawl.arena.config.name;
			run set_arena_name name new_name;
			type all;
			help Changes the name of an arena.;
		}

		subtitle {
			perm redstoner.snowbrawl.arena.config.subtitle;
			run clear_arena_subtitle name;
			type all;
			help Clears the subtitle of an arena.;
		}

		subtitle [string:new_subtitle...] {
			perm redstoner.snowbrawl.arena.config.subtitle;
			run set_arena_subtitle name new_subtitle;
			type all;
			help Changes the subtitle of an arena.;
		}

		refill [int:amount] {
			perm redstoner.snowbrawl.arena.config.refill;
			run set_arena_refill name amount;
			type all;
			help Changes the amount of snowballs given when a player refills their stock in this arena.;
		}

		players [int:amount] {
			perm redstoner.snowbrawl.arena.config.players;
			run set_arena_player_limit name amount;
			type all;
			help Changes the maximum amount of players allow to play in this arena.;
		}

		gracetime [int:seconds] {
			perm redstoner.snowbrawl.arena.config.gracetime;
			run set_arena_grace_time name seconds;
			type all;
			help Changes the amount of grace time given before each match in this arena.;
		}

		matchtime [int:seconds] {
			perm redstoner.snowbrawl.arena.config.matchtime;
			run set_arena_match_time name seconds;
			type all;
			help Changes the amount of time each match takes in this arena.;
		}

		damage [int:damage] {
			perm redstoner.snowbrawl.arena.config.damage;
			run set_arena_damage name damage;
			type all;
			help Changes the amount of damage done per snowball hit in this arena.;
		}

		regen [int:regen] {
			perm redstoner.snowbrawl.arena.config.regen;
			run set_arena_regen name regen;
			type all;
			help Changes the amount of regen awarded to players per kill.;
		}

		allowmobspawning [boolean:mobspawningallowed] {
			perm redstoner.snowbrawl.arena.config.mobspawningallowed;
			run set_arena_mobspawningallowed name mobspawningallowed;
			type all;
			help Sets whether mobs are allowed to spawn in this arena.;
		}

		bounds pos1 {
			[block_x:x] [block_y:y] [block_z:z] {
			   run set_arena_pos1 name x y z;
			   type all;
			   help Sets one corner of this arena's bounds to the given coordinates.;
		   }

		   run set_arena_pos1_from_playerpos name;
		   type player;
		   help Sets one corner of this arena's bounds to your current position.;
		}

		bounds pos2 {
			[block_x:x] [block_y:y] [block_z:z] {
				run set_arena_pos2 name x y z;
				type all;
				help Sets the other corner of this arena's bounds to the given coordinates.;
			}

			run set_arena_pos2_from_playerpos name;
			type player;
			help Sets the other corner of this arena's bounds to your current position.;
		}

		bounds [block_x:x1] [block_y:y1] [block_z:z1] [block_x:x2] [block_y:y2] [block_z:z2] {
			run set_arena_bounds name x1 y1 z1 x2 y2 z2;
			type all;
			help Sets this arena's bounds to the given pair of coordinates.;
		}

		spawnpoints list {
			run get_arena_spawnpoints name;
			type all;
			help Gets all spawn points in this arena.;
		}

		spawnpoints add {
			run add_arena_spawnpoint_from_playerpos name;
			type player;
			help Adds a new spawn point on your current position.;
		}

		spawnpoints add [entity_x:x] [entity_y:y] [entity_z:z] [entity_yaw:yaw] [entity_pitch:pitch] {
			run add_arena_spawnpoint name x y z yaw pitch;
			type player;
			help Adds the given coordinates as a spawn point.;
		}

		spawnpoints delete [int:id] {
			run delete_arena_spawnpoint name id;
			type player;
			help Deletes the spawn point of the given IDs.;
		}
	}

	arena [string:name] teleport {
		run go_to_arena name;
		type player;
		help Teleports to a spawn point in this arena.;
	}
}