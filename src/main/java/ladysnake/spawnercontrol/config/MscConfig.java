package ladysnake.spawnercontrol.config;

import ladysnake.spawnercontrol.SpawnerControl;
import me.zeroeightsix.fiber.annotation.Setting;
import net.minecraftforge.fml.common.Mod;

//@Config(modid = SpawnerControl.MOD_ID, name = MSCConfig.MAIN_CONFIG_FILE)
@Mod.EventBusSubscriber(modid = SpawnerControl.MOD_ID)
public class MscConfig {

    @Setting(ignore = true)
    public static final String MAIN_CONFIG_FILE = SpawnerControl.MOD_ID + "/" + SpawnerControl.MOD_ID;
    @Setting(ignore = true)
    public static final String VANILLA_CONFIG_CATEGORY = "general.vanillaSpawnerConfig";

    //    @Config.RequiresWorldRestart
    @Setting(comment = "If set to false, vanilla spawners won't be affected by the mod. This mod's own spawners will be the only ones affected by this config")
    public boolean alterVanillaSpawner = true;

    @Setting.Node
    @Setting(comment = "The spawner configuration for vanilla or vanilla-derived spawners")
    public SpawnerConfig vanillaSpawnerConfig = new SpawnerConfig();

    //    @Config.RequiresMcRestart
//    @Config.RequiresWorldRestart // this config can actually cause unexpected behaviour when changed in-game
    @Setting(comment = "A list of ids used to generate custom spawner objects\nEach custom spawner uses a separate configuration file, accessible in the 'custom spawners config' category")
    public String[] customSpawners = new String[0];

    //    @Config.RequiresMcRestart
    @Setting(comment = "If set to true, the mod will generate its own creative tab to put custom spawners in")
    public boolean makeCreativeTab = false;

}
