package ladysnake.spawnercontrol.config;

import ladysnake.spawnercontrol.SpawnerControl;
import ladysnake.spawnercontrol.controlledspawner.BlockControlledSpawner;
import ladysnake.spawnercontrol.controlledspawner.ItemBlockControlSpawner;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class SpawnerConfigHolder {
    private Configuration configuration;
    private String name;
    private ResourceLocation registryName;
    private SpawnerConfig configObject;

    SpawnerConfigHolder(Configuration configuration, String name) {
        this.configuration = configuration;
        this.name = name;
        // avoid errors that would be way too stupid
        this.registryName = new ResourceLocation(SpawnerControl.MOD_ID, name.replaceAll(" ", "_"));
        this.configObject = new SpawnerConfig();
    }

    public void sync() {
        try {
            // sync the object with the config
            CustomSpawnersConfig.configManager$sync.invoke(configuration, SpawnerConfig.class, SpawnerControl.MOD_ID, name, true, configObject);
            this.configObject.mobLoot.lootEntries.invalidateAll();
            configuration.save();
        } catch (Throwable throwable) {
            SpawnerControl.LOGGER.error("Exception while synchronizing custom spawner config", throwable);
        }
    }

    /**
     *
     * @return a new {@link BlockControlledSpawner} instance using this config holder and named accordingly
     */
    public Block createBlock() {
        return new BlockControlledSpawner(getConfigObject())
                .setRegistryName(this.registryName)
                .setUnlocalizedName("msc." + name)
                .setCreativeTab(SpawnerControl.creativeTab);
    }

    public Item createItem() {
        return new ItemBlockControlSpawner(getBlock(), getName()).setRegistryName(this.registryName);
    }

    public Block getBlock() {
        return ForgeRegistries.BLOCKS.getValue(this.registryName);
    }

    public Item getItem() {
        return ForgeRegistries.ITEMS.getValue(this.registryName);
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public ConfigCategory getConfigCategory() {
        return getConfiguration().getCategory(name);
    }

    public String getName() {
        return name;
    }

    public ResourceLocation getRegistryName() {
        return registryName;
    }

    public SpawnerConfig getConfigObject() {
        return configObject;
    }
}
