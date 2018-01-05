package ladysnake.spawnercontrol;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid=SpawnerControl.MOD_ID)
@Mod.EventBusSubscriber(modid = SpawnerControl.MOD_ID)
public class Configuration {

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

    @Config.Comment("A list of item ids that a mob spawner drops when broken\nFormat: 'modid:item(:count(:meta(:chance)))' (count, meta and chance are optional)")
    public static String[] itemsDropped = new String[0];

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(SpawnerControl.MOD_ID))
            ConfigManager.sync(SpawnerControl.MOD_ID, Config.Type.INSTANCE);
    }

}
