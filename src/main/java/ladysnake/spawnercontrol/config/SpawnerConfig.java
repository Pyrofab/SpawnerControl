package ladysnake.spawnercontrol.config;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import ladysnake.spawnercontrol.SpawnerControl;
import me.zeroeightsix.fiber.annotation.Setting;
import net.minecraft.util.ResourceLocation;

import java.util.Arrays;
import java.util.List;

public class SpawnerConfig {

    @Setting.Node(name = "Spawn Conditions")
    @Setting(comment = "Regroups config options aiming to alter mob spawners spawning conditions")
    public SpawnConditions spawnConditions = new SpawnConditions();

    @Setting.Node(name = "Mob Loot")
    @Setting(comment = "Groups config options related to custom drops from mobs spawned by this spawner type")
    public MobLoot mobLoot = new MobLoot();

    @Setting(comment = "When a spawner has spawned this number of mobs over this lifetime, it will get broken automatically")
    public int mobThreshold = 100;

    @Setting(comment = "If set to true, spawners will count mobs when they are killed rather than when they are spawned")
    public boolean incrementOnMobDeath = false;

    @Setting(comment = "This multiplier is applied on the spawner's spawn delay each time a mob is spawned. \nCan be used to make mobs spawned exponentially faster or slower.")
    public float spawnRateModifier = 1.0f;

    @Setting(comment = "If set to false, spawners will only be disabled when expired, not broken")
    public boolean breakSpawner = true;

    @Setting(comment = "The minimum amount of xp dropped by a spawner when broken")
    public int xpDropped = 15;

    @Setting.Constrain.Min(0)
    @Setting(comment = "The formula used to calculate xp dropped is 'xpDropped + rand(this number) + rand(this number)'")
    public int randXpVariation = 15;

    @Setting(comment = "A list of item ids that a mob spawner drops when broken\nFormat: 'modid:item(:count(:meta(:chance)))' (count, meta and chance are optional)")
    public String[] itemsDropped = new String[0];

    public static class SpawnConditions {

        @Setting(comment = "If set to true, spawners will not run mob-specific checks on any entity")
        public boolean forceSpawnerAllSpawns = false;

        @Setting(comment = "If set to true, spawners will not run mob-specific checks on hostile mob entities\nThis most notably prevents players from disabling a spawner with torches or other light sources")
        public boolean forceSpawnerMobSpawns = false;

        @Setting(comment = "If forceSpawnerMobSpawns is enabled, will prevent hostile mobs from spawning in daylight")
        public boolean checkSunlight = true;
    }

    public static class MobLoot {

        @Setting.Node
        public MobLootEntry defaultValues = new MobLootEntry(1, 0, false, new String[0], new String[0]);

        @Setting(comment = "Individual xp drop multiplier configuration for mobs spawned by this spawner type\nFormat: 'modid:entity:xpMultiplier(:flatXp)' (flatXp is optional)")
        public String[] xpMultipliers = new String[0];

        @Setting(comment = "Individual item drop removal configuration for mobs spawned by this spawner type\nFormat: 'modid:entity(,modid:item(:meta))(,modid:item(:meta))...'\nAnything in parenthesis is optional, and you can enter as many items as you want\nIf don't enter any items, all item drops are removed from the mob (itemDropAdditions are added afterwards)")
        public String[] itemDropRemovals = new String[0];

        @Setting(comment = "Individual item drop addition configuration for mobs spawned by this spawner type\nFormat: 'modid:entity,modid:item(:count(:meta(:chance)))(,modid:item(:count(:meta(:chance))))...'\nAnything in parenthesis is optional, and you can enter as many items as you want\nEg: minecraft:skeleton,minecraft:dye:100:15:0.5,minecraft:bone:1:1:1")
        public String[] itemDropAdditions = new String[0];

        @Setting(ignore = true)
        public transient LoadingCache<ResourceLocation, MobLootEntry> lootEntries = CacheBuilder.newBuilder().build(CacheLoader.from(rl -> {
            if (rl == null) return defaultValues;
            float xpMultiplier = defaultValues.xpMultiplier;
            int flatXpIncrease = defaultValues.flatXpIncrease;
            boolean removeAllItems = defaultValues.removeAllItems;
            List<String> removedItems = Arrays.asList(defaultValues.removedItems);
            List<String> addedItems = Arrays.asList(defaultValues.addedItems);
            for (String s : xpMultipliers) {
                String[] split = s.split(":");
                if (split[0].equals(rl.getNamespace()) && split[1].equals(rl.getPath())) {
                    try {
                        xpMultiplier = Float.parseFloat(split[2]);
                        flatXpIncrease = split.length > 3 ? Integer.parseInt(split[3]) : defaultValues.flatXpIncrease;
                    } catch (NumberFormatException e) {
                        SpawnerControl.LOGGER.warn("Bad mob spawner loot config option : {}", s);
                    }
                    break;
                }
            }
            for (String s : itemDropRemovals) {
                String[] split = s.split(",");
                String[] mobSplit = split[0].split(":");
                if (mobSplit[0].equals(rl.getNamespace()) && mobSplit[1].equals(rl.getPath())) {
                    try {
                        removedItems = Arrays.asList(split);
                        removedItems.remove(0);
                        removeAllItems = removedItems.size() == 0;
                    } catch (Exception e) {
                        SpawnerControl.LOGGER.warn("Bad mob spawner loot config option : {}", s);
                    }
                    break;
                }
            }
            for (String s : itemDropAdditions) {
                String[] split = s.split(":");
                if (split[0].equals(rl.getNamespace()) && split[1].equals(rl.getPath())) {
                    try {
                        addedItems = Arrays.asList(split);
                        addedItems.remove(0);
                    } catch (Exception e) {
                        SpawnerControl.LOGGER.warn("Bad mob spawner loot config option : {}", s);
                    }
                    break;
                }
            }
            return new MobLootEntry(xpMultiplier, flatXpIncrease, removeAllItems, removedItems.toArray(new String[0]), addedItems.toArray(new String[0]));
        }));

        public static class MobLootEntry {
            private MobLootEntry() {
                // used for deserialization
            }

            public MobLootEntry(float defaultXpMultiplier, int flatXpIncrease, boolean removeAllItems, String[] removedItems, String[] addedItems) {
                this.xpMultiplier = defaultXpMultiplier;
                this.flatXpIncrease = flatXpIncrease;

                this.removeAllItems = removeAllItems;
                this.removedItems = removedItems;
                this.addedItems = addedItems;
            }

            @Setting(comment = "xp drop multiplier for mobs spawned by this spawner type")
            public float xpMultiplier;

            @Setting(comment = "Flat xp modifier that will be added to mobs spawned by this spawner type")
            public int flatXpIncrease;

            @Setting(comment = "Remove all existing item drops from the mobs spawned by this spawner\n'Added Items' are added afterwards")
            public boolean removeAllItems;

            @Setting(comment = "Which items to remove from the drops of the mobs spawned\nFormat: 'modid:item(:meta)' (meta is optional)\nIf 'Remove All Items' is true, this does nothing")
            public String[] removedItems;

            @Setting(comment = "Which items to add to the drops of the mobs spawned\nFormat: 'modid:item(:count(:meta(:chance)))' (count, meta and chance are optional)")
            public String[] addedItems;
        }
    }
}
