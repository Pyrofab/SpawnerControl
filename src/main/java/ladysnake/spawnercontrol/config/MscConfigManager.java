package ladysnake.spawnercontrol.config;

import com.google.common.collect.ImmutableList;
import ladysnake.spawnercontrol.SpawnerControl;
import me.zeroeightsix.fiber.NodeOperations;
import me.zeroeightsix.fiber.exception.FiberException;
import me.zeroeightsix.fiber.tree.ConfigNode;
import me.zeroeightsix.fiber.tree.Node;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This class is tasked with dynamically generating configuration options for each custom spawner added by the user <br/>
 * <p>
 * Constants used by the config systems are also stored here to avoid being considered as config options in annotated classes <br/>
 * </p>
 *
 * @see MscConfig#customSpawners
 */
public class MscConfigManager {
    /**
     * The location of Spawner Control's configuration directory
     */
    public static final Path CONFIG_ROOT = Paths.get("config", SpawnerControl.MOD_ID);
    public static final Pattern VALIDATION_PATTERN = Pattern.compile("^[\\w\\s\\d]+$");

    private final ConfigHolder<MscConfig> mainConfig = new ConfigHolder<>(
            new ConfigNode("spawner-control", "Root configuration"),
            new MscConfig()
    );
    /**
     * Maps custom spawner names to their configuration
     */
    private final List<SpawnerConfigHolder> customSpawnerConfigs = new ArrayList<>();

    public void init() throws IOException, FiberException {
        this.mainConfig.load();
        Node vanillaSpawnerConfig = (Node) this.mainConfig.config.lookup("vanillaSpawnerConfig");
        assert vanillaSpawnerConfig != null;

        for (String name : this.mainConfig.getConfigObject().customSpawners) {
            // as the validation pattern wasn't used when reading the values, it can contain garbage from the file
            if (VALIDATION_PATTERN.matcher(name).matches()) {
                Node cfg = this.mainConfig.config.fork(name, true);
                // duplicate the config from the vanilla spawner
                NodeOperations.mergeTo(vanillaSpawnerConfig, cfg);
                SpawnerConfigHolder customSpawnerConfig = new SpawnerConfigHolder(cfg, new SpawnerConfig());
                customSpawnerConfig.load();
                this.customSpawnerConfigs.add(customSpawnerConfig);
            } else {
                SpawnerControl.LOGGER.warn("Invalid custom spawner name {}, skipping", name);
            }
        }
    }

    public ImmutableList<SpawnerConfigHolder> getCustomSpawnerConfigs() {
        return ImmutableList.copyOf(customSpawnerConfigs);
    }

    public MscConfig getMainConfig() {
        return this.mainConfig.getConfigObject();
    }
}
