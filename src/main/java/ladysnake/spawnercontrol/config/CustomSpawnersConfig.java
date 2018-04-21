package ladysnake.spawnercontrol.config;

import ladysnake.spawnercontrol.SpawnerControl;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * This class is tasked to dynamically generate configuration options for each custom spawner added by the user <br/>
 * <p>
 * Constants used by the config systems are also stored here to avoid being considered as config options in annotated classes <br/>
 * </p>
 * @see MSCConfig#customSpawners
 */
public class CustomSpawnersConfig {
    private static final String CUSTOM_CONFIG_FOLDER = SpawnerControl.MOD_ID + "/" + "custom_spawners";
    private static final Pattern VALIDATION_PATTERN = Pattern.compile("^[\\w\\s\\d]+$");
    /**
     * The location of the Minecraft instance's configuration directory,
     * as given by {@link FMLPreInitializationEvent#getModConfigurationDirectory()}
     */
    public static File configDir;
    // we want to sync dynamically generated config to objects for convenience
    // that method does exactly that, sadly it's private
    static MethodHandle configManager$sync;
    private static Configuration mainConfiguration;
    /**Maps custom spawner names to their configuration*/
    private static final Map<ResourceLocation, SpawnerConfigHolder> customSpawnerConfigs = new HashMap<>();

    static {
        try {
            Method m = ConfigManager.class.getDeclaredMethod("sync", Configuration.class, Class.class, String.class, String.class, boolean.class, Object.class);
            m.setAccessible(true);
            configManager$sync = MethodHandles.lookup().unreflect(m);
        } catch (IllegalAccessException | NoSuchMethodException e) {
            SpawnerControl.LOGGER.error("Error while reflecting ConfigManager#sync", e);
        }
    }

    /**
     * Gets the specific configuration object used by forge's annotation system, as newly created config objects will
     * not be taken into account.
     * @return the configuration object used by forge for this mod's main config
     */
    static Configuration getMainConfiguration() {
        if (mainConfiguration == null) {
            try {
                // Get the specific configuration object used by forge to change its validation pattern
                Method getConfiguration = ConfigManager.class.getDeclaredMethod("getConfiguration", String.class, String.class);
                getConfiguration.setAccessible(true);
                mainConfiguration = (Configuration) getConfiguration.invoke(null, SpawnerControl.MOD_ID, MSCConfig.MAIN_CONFIG_FILE);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                SpawnerControl.LOGGER.error("Error while attempting to access spawner control's configuration", e);
                return new Configuration(new File(configDir, MSCConfig.MAIN_CONFIG_FILE + ".cfg"));
            }
        }
        return mainConfiguration;
    }

    public static void initCustomConfig() {
        Configuration baseConfig = getMainConfiguration();
        ConfigCategory mainCategory = baseConfig.getCategory(Configuration.CATEGORY_GENERAL);
        Property customSpawnersProp = mainCategory.get("customSpawners");
        // set the validation pattern here as there isn't an annotation for that
        customSpawnersProp.setValidationPattern(VALIDATION_PATTERN);
        for (String name : customSpawnersProp.getStringList()) {
            // as the validation pattern wasn't used when reading the values, it can contain garbage from the file
            if (VALIDATION_PATTERN.matcher(name).matches())
                generateConfig(baseConfig.getCategory(MSCConfig.VANILLA_CONFIG_CATEGORY), name);
            else
                SpawnerControl.LOGGER.warn("Invalid custom spawner name {}, skipping", name);
        }
    }

    public static Collection<SpawnerConfigHolder> getCustomSpawnerConfigs() {
        return customSpawnerConfigs.values();
    }

    private static void generateConfig(ConfigCategory baseCategory, String name) {
        Configuration config = new Configuration(new File(configDir, CUSTOM_CONFIG_FOLDER + "/" + name + ".cfg"));
        ConfigCategory category = config.getCategory(name);
        // duplicate the config from the vanilla spawner
        baseCategory.getValues().forEach((key, value) -> {
            if (!category.containsKey(key)) // don't overwrite existing values
                category.put(key, value);
        });
        SpawnerConfigHolder holder = new SpawnerConfigHolder(config, name);
        holder.sync();
        customSpawnerConfigs.put(holder.getRegistryName(), holder);
    }

}
