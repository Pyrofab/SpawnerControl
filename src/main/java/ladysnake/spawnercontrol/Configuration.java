package ladysnake.spawnercontrol;

import net.minecraftforge.common.config.Config;

import java.util.HashMap;
import java.util.Map;

@Config(modid=SpawnerControl.MOD_ID)
public class Configuration {

    @Config.Comment("When a spawner has spawned this number of mobs over this lifetime, it will get broken automatically")
    public static int mobThreshold = 100;

    @Config.Comment("If set to false, spawners will only be disabled when expired, not broken")
    public static boolean breakSpawner = true;

    @Config.Comment("The minimum amount of xp dropped by a spawner when broken")
    public static int xpDropped = 15;

    @Config.Comment("The formula used to calculate xp dropped is 'xpDropped + rand(this number) + rand(this number)'")
    public static int randXpVariation = 15;

    @Config.Comment("A list of item ids that a mob spawner drops when broken")
    public static Map<String, Integer> itemsDropped = new HashMap<String, Integer>();

}
