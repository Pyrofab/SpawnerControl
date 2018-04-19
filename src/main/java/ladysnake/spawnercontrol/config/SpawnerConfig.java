package ladysnake.spawnercontrol.config;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import ladysnake.spawnercontrol.SpawnerControl;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Config;

public class SpawnerConfig {

    @Config.Comment("Regroups config options aiming to alter mob spawners spawning conditions")
    public SpawnConditions spawnConditions = new SpawnConditions();

    @Config.Comment("Groups config options related to custom drops from mobs spawned by this spawner type")
    public MobLoot mobLoot = new MobLoot();

    @Config.Comment("When a spawner has spawned this number of mobs over this lifetime, it will get broken automatically")
    public int mobThreshold = 100;

    @Config.Comment("If set to true, spawners will count mobs when they are killed rather than when they are spawned")
    public boolean incrementOnMobDeath = false;

    @Config.Comment("This multiplier is applied on the spawner's spawn delay each time a mob is spawned. \nCan be used to make mobs spawned exponentially faster or slower.")
    public float spawnRateModifier = 1.0f;

    @Config.Comment("If set to false, spawners will only be disabled when expired, not broken")
    public boolean breakSpawner = true;

    @Config.Comment("The minimum amount of xp dropped by a spawner when broken")
    public int xpDropped = 15;

    @Config.RangeInt(min = 0)
    @Config.Comment("The formula used to calculate xp dropped is 'xpDropped + rand(this number) + rand(this number)'")
    public int randXpVariation = 15;

    @Config.Comment({"A list of item ids that a mob spawner drops when broken", "Format: 'modid:item(:count(:meta(:chance)))' (count, meta and chance are optional)"})
    public String[] itemsDropped = new String[0];

    public static class SpawnConditions {

        @Config.Comment({"If set to true, spawners will not run mob-specific checks on any entity"})
        public boolean forceSpawnerAllSpawns = false;

        @Config.Comment({"If set to true, spawners will not run mob-specific checks on hostile mob entities", "This most notably prevents players from disabling a spawner with torches or other light sources"})
        public boolean forceSpawnerMobSpawns = false;

        @Config.Comment("If forceSpawnerMobSpawns is enabled, will prevent hostile mobs from spawning in daylight")
        public boolean checkSunlight = true;
    }

    public static class MobLoot {

        public MobLootEntry defaultValues = new MobLootEntry(1, 0);

        @Config.Comment({"Individual xp drop multiplier configuration for mobs spawned by this spawner type", "Format: 'modid:entity:xpMultiplier(:flatXp)' (flatXp is optional)"})
        public String[] xpMultipliers = new String[0];

        @Config.Ignore
        public LoadingCache<ResourceLocation, MobLootEntry> lootEntries = CacheBuilder.newBuilder().build(CacheLoader.from(rl -> {
            if (rl == null) return defaultValues;
            for (String s : xpMultipliers) {
                String[] split = s.split(":");
                if (split[0].equals(rl.getResourcePath()) && split[1].equals(rl.getResourceDomain())) {
                    try {
                        float xpMultiplier = Float.parseFloat(split[2]);
                        int flatXpIncrease = split.length > 3 ? Integer.parseInt(split[3]) : defaultValues.flatXpIncrease;
                        return new MobLootEntry(xpMultiplier, flatXpIncrease);
                    } catch (NumberFormatException e) {
                        SpawnerControl.LOGGER.warn("Bad mob spawner loot config option : {}", s);
                    }
                }
            }
            return defaultValues;
        }));

        public static class MobLootEntry {

            public MobLootEntry(float defaultXpMultiplier, int flatXpIncrease) {
                this.xpMultiplier = defaultXpMultiplier;
                this.flatXpIncrease = flatXpIncrease;
            }

            @Config.Comment("xp drop multiplier for mobs spawned by this spawner type")
            public float xpMultiplier;

            @Config.Comment("Flat xp modifier that will be added to mobs spawned by this spawner type")
            public int flatXpIncrease;

        }
    }
}
