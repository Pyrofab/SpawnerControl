package ladysnake.spawnercontrol.config;

import ladysnake.spawnercontrol.SpawnerControl;
import me.zeroeightsix.fiber.JanksonSettings;
import me.zeroeightsix.fiber.annotation.AnnotatedSettings;
import me.zeroeightsix.fiber.exception.FiberException;
import me.zeroeightsix.fiber.tree.Node;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigHolder<C> {
    protected final Node config;
    protected final C configPojo;
    private final JanksonSettings janksonSettings = new JanksonSettings();

    public ConfigHolder(Node config, C configObject) {
        this.config = config;
        this.configPojo = configObject;
    }

    public void load() throws IOException, FiberException {
        AnnotatedSettings.applyToNode(this.config, this.configPojo);
        Path path = this.getPath();
        if (Files.exists(path)) {
            this.janksonSettings.deserialize(config, Files.newInputStream(path));
        }
        // save to update/create the config file
        this.save();
    }

    public void save() {
        try {
            janksonSettings.serialize(this.config, Files.newOutputStream(getPath()), false);
        } catch (IOException e) {
            SpawnerControl.LOGGER.error("Exception while synchronizing custom spawner config", e);
        }
    }

    @Nonnull
    protected Path getPath() {
        return MscConfigManager.CONFIG_ROOT.resolve(this.config.getName() + ".json5");
    }

    public C getConfigObject() {
        return configPojo;
    }
}
