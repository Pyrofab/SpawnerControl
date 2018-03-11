package ladysnake.spawnercontrol;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@SuppressWarnings("WeakerAccess")
@Config(modid=SpawnerControl.MOD_ID)
@Mod.EventBusSubscriber(modid = SpawnerControl.MOD_ID)
public class Configuration {

    @Config.RequiresMcRestart
    @Config.Comment("If set to false, this mod won't register its own spawner, making it possible to use serverside with vanilla clients.")
    public static boolean registerCustomSpawner = true;

    @Config.Comment("Regroups config options aiming to alter mob spawners spawning conditions")
    public static SpawnConditions spawnConditions = new SpawnConditions();

    @Config.RequiresWorldRestart
    @Config.Comment("If set to false, vanilla spawners won't be affected by the mod. This mod's own spawner will be the only one affected by this config")
    public static boolean alterVanillaSpawner = true;

    @Config.Comment("When a spawner has spawned this number of mobs over this lifetime, it will get broken automatically")
    public static int mobThreshold = 100;

    @Config.Comment("If set to true, spawners will count mobs when they are killed rather than when they are spawned")
    public static boolean incrementOnMobDeath = false;

    @Config.Comment("This multiplier is applied on the spawner's spawn delay each time a mob is spawned. \nan be used to make mobs spawned exponentially faster or slower.")
    public static float spawnRateModifier = 1.0f;

    @Config.Comment("If set to false, spawners will only be disabled when expired, not broken")
    public static boolean breakSpawner = true;

    @Config.Comment("The minimum amount of xp dropped by a spawner when broken")
    public static int xpDropped = 15;

    @Config.Comment("The formula used to calculate xp dropped is 'xpDropped + rand(this number) + rand(this number)'")
    public static int randXpVariation = 15;

    @Config.Comment({"A list of item ids that a mob spawner drops when broken","Format: 'modid:item(:count(:meta(:chance)))' (count, meta and chance are optional)"})
    public static String[] itemsDropped = new String[0];

    public static class SpawnConditions {

        @Config.Comment({"If set to true, spawners will not run mob-specific checks on any entity"})
        public boolean forceSpawnerAllSpawns = false;

        @Config.Comment({"If set to true, spawners will not run mob-specific checks on hostile mob entities", "This most notably prevents players from disabling a spawner with torches or other light sources"})
        public boolean forceSpawnerMobSpawns = false;

        @Config.Comment("If forceSpawnerMobSpawns is enabled, will prevent hostile mobs from spawning in daylight")
        public boolean checkSunlight = true;
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(SpawnerControl.MOD_ID))
            ConfigManager.sync(SpawnerControl.MOD_ID, Config.Type.INSTANCE);
    }

}
