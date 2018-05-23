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
@Config(modid = SpawnerControl.MOD_ID, name = MSCConfig.MAIN_CONFIG_FILE)
@Mod.EventBusSubscriber(modid = SpawnerControl.MOD_ID)
public class MSCConfig {

    @Config.Ignore
    public static final String MAIN_CONFIG_FILE = SpawnerControl.MOD_ID + "/" + SpawnerControl.MOD_ID;
    @Config.Ignore
    public static final String VANILLA_CONFIG_CATEGORY = Configuration.CATEGORY_GENERAL + Configuration.CATEGORY_SPLITTER + "vanillaSpawnerConfig";

    @Config.RequiresWorldRestart
    @Config.Comment("If set to false, vanilla spawners won't be affected by the mod. This mod's own spawners will be the only ones affected by this config")
    public static boolean alterVanillaSpawner = true;

    @Config.Comment("The spawner configuration for vanilla or vanilla-derived spawners")
    public static SpawnerConfig vanillaSpawnerConfig = new SpawnerConfig();

    @Config.RequiresMcRestart
    @Config.RequiresWorldRestart // this config can actually cause unexpected behaviour when changed in-game
    @Config.Comment({"A list of ids used to generate custom spawner objects", "Each custom spawner uses a separate configuration file, accessible in the 'custom spawners config' category"})
    public static String[] customSpawners = new String[0];

    @Config.RequiresMcRestart
    @Config.Comment("If set to true, the mod will generate its own creative tab to put custom spawners in")
    public static boolean makeCreativeTab = false;

    public static void portConfig() {
        File oldConfigFile = new File(CustomSpawnersConfig.configDir, SpawnerControl.MOD_ID + ".cfg");
        if (!oldConfigFile.exists()) return;
        Configuration oldConfig = new Configuration(oldConfigFile, "1.0");
        Configuration newConfig = CustomSpawnersConfig.getMainConfiguration();
        if (oldConfig.getLoadedConfigVersion() == null) { // pre-1.4 config
            ConfigCategory general = oldConfig.getCategory(Configuration.CATEGORY_GENERAL);
            ConfigCategory vanillaSpawner = newConfig.getCategory(VANILLA_CONFIG_CATEGORY);
            // move properties from general to vanillaSpawnerConfig
            copyConfigCategory(general, vanillaSpawner);

            // hardcoded because old configs only have 1 subcategory
            if (general.getChildren().size() > 0) {
                ConfigCategory oldSpawnConditions = general.getChildren().iterator().next();
                vanillaSpawner.getChildren().stream()
                        .filter(child -> child.getName().equals(oldSpawnConditions.getName()))
                        .findAny().ifPresent(spawnConditions -> copyConfigCategory(oldSpawnConditions, spawnConditions));
            }
            newConfig.save();
            try {
                Files.move(oldConfigFile, new File(CustomSpawnersConfig.configDir, SpawnerControl.MOD_ID + "_old.cfg"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void copyConfigCategory(ConfigCategory oldConf, ConfigCategory newConf) {
        for (Map.Entry<String, Property> entry : newConf.getValues().entrySet())
            if (oldConf.containsKey(entry.getKey()))
                newConf.put(entry.getKey(), oldConf.get(entry.getKey()));
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(SpawnerControl.MOD_ID)) {
            ConfigManager.sync(SpawnerControl.MOD_ID, Config.Type.INSTANCE);
            CustomSpawnersConfig.getCustomSpawnerConfigs().forEach(SpawnerConfigHolder::sync);
        }
    }

}
