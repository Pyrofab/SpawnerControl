package ladysnake.spawnercontrol.config;

import ladysnake.spawnercontrol.SpawnerControl;
import ladysnake.spawnercontrol.controlledspawner.BlockControlledSpawner;
import ladysnake.spawnercontrol.controlledspawner.ItemBlockControlSpawner;
import me.zeroeightsix.fiber.tree.Node;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.nio.file.Path;

public class SpawnerConfigHolder extends ConfigHolder<SpawnerConfig> {
    private static final Path CUSTOM_SPAWNER_CONFIGS = MscConfigManager.CONFIG_ROOT.resolve("custom_spawners");

    private final ResourceLocation registryName;
    private final RegistryObject<Block> block;
    private final RegistryObject<Item> item;

    SpawnerConfigHolder(Node configuration, SpawnerConfig configObject) {
        super(configuration, configObject);
        // avoid errors that would be way too stupid
        this.registryName = new ResourceLocation(SpawnerControl.MOD_ID, this.getName().replaceAll(" ", "_"));
        this.block = RegistryObject.of(this.registryName, ForgeRegistries.BLOCKS);
        this.item = RegistryObject.of(this.registryName, ForgeRegistries.ITEMS);
    }

    @Override
    public void save() {
        super.save();
        this.configPojo.mobLoot.lootEntries.invalidateAll();
    }

    @Nonnull
    @Override
    protected Path getPath() {
        return CUSTOM_SPAWNER_CONFIGS.resolve(this.config.getName() + ".json5");
    }

    public Block createBlock() {
        return new BlockControlledSpawner(Block.Properties.from(Blocks.SPAWNER), this.getConfigObject(), this.getName())
                .setRegistryName(this.registryName);
    }

    public Item createItem(Item.Properties properties) {
        return new ItemBlockControlSpawner(this.getBlock(), properties)
                .setRegistryName(this.registryName);
    }

    /**
     * @return a {@link BlockControlledSpawner} instance using this config holder and named accordingly
     */
    public Block getBlock() {
        return this.block.get();
    }

    public Item getItem() {
        return this.item.get();
    }

    public Node getConfiguration() {
        return config;
    }

    private String getName() {
        return this.config.getName();
    }

    public ResourceLocation getRegistryName() {
        return registryName;
    }
}
