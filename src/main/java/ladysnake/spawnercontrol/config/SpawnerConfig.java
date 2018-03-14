package ladysnake.spawnercontrol.config;

import net.minecraftforge.common.config.Config;

public class SpawnerConfig {

    @Config.Comment("Regroups config options aiming to alter mob spawners spawning conditions")
    public SpawnConditions spawnConditions = new SpawnConditions();

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
}
