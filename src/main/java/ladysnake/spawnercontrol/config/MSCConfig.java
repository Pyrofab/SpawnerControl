package ladysnake.spawnercontrol.config;

import com.google.common.io.Files;
import ladysnake.spawnercontrol.SpawnerControl;
import net.minecraftforge.common.config.*;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
@Config(modid= SpawnerControl.MOD_ID, name = CustomSpawnersConfig.MAIN_CONFIG_FILE)
@Mod.EventBusSubscriber(modid = SpawnerControl.MOD_ID)
public class MSCConfig {

    @Config.RequiresWorldRestart
    @Config.Comment("If set to false, vanilla spawners won't be affected by the mod. This mod's own spawners will be the only ones affected by this config")
    public static boolean alterVanillaSpawner = true;

    @Config.Comment("The spawner configuration for vanilla or vanilla-derived spawners")
    public static SpawnerConfig vanillaSpawnerConfig = new SpawnerConfig();

    @Config.RequiresMcRestart
    @Config.Comment({"A list of ids used to generate custom spawner objects", "Each custom spawner uses a separate configuration file, accessible in the 'custom spawners' category"})
    public static String[] customSpawners = new String[0];

    @Config.RequiresMcRestart
    @Config.Comment("If set to true, the mod will generate its own creative tab to put custom spawners in")
    public static boolean makeCreativeTab = false;

    public static void portConfig() {
        File oldConfigFile = new File(CustomSpawnersConfig.configDir, SpawnerControl.MOD_ID + ".cfg");
        File newConfigFile = new File(CustomSpawnersConfig.configDir, CustomSpawnersConfig.MAIN_CONFIG_FILE + ".cfg");
        if (!oldConfigFile.exists()) return;
        Configuration oldConfig = new Configuration(oldConfigFile, "1.0");
        Configuration newConfig = new Configuration(newConfigFile, "1.0");
        if (oldConfig.getLoadedConfigVersion() == null) { // pre-1.4 config
            ConfigCategory general = oldConfig.getCategory(Configuration.CATEGORY_GENERAL);
            ConfigCategory vanillaSpawner = newConfig.getCategory(CustomSpawnersConfig.VANILLA_CONFIG_CATEGORY);
            // move properties from general to vanillaSpawnerConfig
            for (Map.Entry<String, Property> entry : vanillaSpawner.getValues().entrySet())
                if (general.containsKey(entry.getKey()))
                    vanillaSpawner.put(entry.getKey(), general.get(entry.getKey()));

            newConfig.save();
            try {
                Files.move(oldConfigFile, new File(CustomSpawnersConfig.configDir, SpawnerControl.MOD_ID + "_old.cfg"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(SpawnerControl.MOD_ID)) {
            ConfigManager.sync(SpawnerControl.MOD_ID, Config.Type.INSTANCE);
            CustomSpawnersConfig.getCustomSpawnerConfigs().forEach(SpawnerConfigHolder::sync);
        }
    }

}
