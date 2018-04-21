package ladysnake.spawnercontrol;

import ladysnake.spawnercontrol.config.CustomSpawnersConfig;
import ladysnake.spawnercontrol.config.MSCConfig;
import ladysnake.spawnercontrol.config.SpawnerConfigHolder;
import ladysnake.spawnercontrol.controlledspawner.CapabilityControllableSpawner;
import ladysnake.spawnercontrol.controlledspawner.IControllableSpawner;
import ladysnake.spawnercontrol.controlledspawner.TileEntityControlledSpawner;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;

@Mod(
        modid = SpawnerControl.MOD_ID,
        name = SpawnerControl.MOD_NAME,
        version = SpawnerControl.VERSION,
        acceptedMinecraftVersions = SpawnerControl.ACCEPTED_VERSIONS,
        guiFactory = "ladysnake.spawnercontrol.client.MSCConfigFactory",
        dependencies = "required:forge@[14.23.2.2596,);",
        acceptableRemoteVersions = "*"
)
public class SpawnerControl {

    public static final String MOD_ID = "spawnercontrol";
    static final String MOD_NAME = "Mob Spawner Control";
    static final String VERSION = "@VERSION@";
    static final String ACCEPTED_VERSIONS = "[1.12, 1.13)";

    public static Logger LOGGER;
    public static CreativeTabs creativeTab;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
        CustomSpawnersConfig.configDir = event.getModConfigurationDirectory();
        MSCConfig.portConfig();
        CustomSpawnersConfig.initCustomConfig();
        // register the capability storing extra spawner information
        CapabilityManager.INSTANCE.register(IControllableSpawner.class, new CapabilityControllableSpawner.Storage(), CapabilityControllableSpawner.DefaultControllableSpawner::new);
        // No need to register a tile entity that's not used anywhere
        if (MSCConfig.customSpawners.length > 0)
            GameRegistry.registerTileEntity(TileEntityControlledSpawner.class, "spawnercontrol:controlled_spawner");
        if (MSCConfig.makeCreativeTab) {
            creativeTab = new CreativeTabs(MOD_ID) {
                @Nonnull
                @Override
                public ItemStack getTabIconItem() {
                    return new ItemStack(Blocks.MOB_SPAWNER);
                }
            };
        }
    }

    @Mod.EventBusSubscriber
    public static class RegistryHandler {

        @SubscribeEvent
        public static void onBlockRegister(RegistryEvent.Register<Block> event) {
            CustomSpawnersConfig.getCustomSpawnerConfigs().stream()
                    .map(SpawnerConfigHolder::createBlock)
                    .forEach(event.getRegistry()::register);
        }

        @SubscribeEvent
        public static void onItemRegister(RegistryEvent.Register<Item> event) {
            CustomSpawnersConfig.getCustomSpawnerConfigs().stream()
                    .map(SpawnerConfigHolder::createItem)
                    .forEach(event.getRegistry()::register);
        }

        @SubscribeEvent
        @SideOnly(Side.CLIENT)
        public static void registerRenders(ModelRegistryEvent event) {
            ModelResourceLocation mrl = new ModelResourceLocation("mob_spawner", "inventory");
            for (SpawnerConfigHolder spawnerConfigHolder : CustomSpawnersConfig.getCustomSpawnerConfigs()) {
                ModelLoader.setCustomModelResourceLocation(spawnerConfigHolder.getItem(), 0, mrl);
                mapToSpawnerModel(spawnerConfigHolder.getBlock(), mrl);
            }
        }

        /**
         * Maps all states of a block to a custom {@link net.minecraft.client.renderer.block.model.IBakedModel}
         *
         * @param block the block to be mapped
         */
        @SideOnly(Side.CLIENT)
        private static void mapToSpawnerModel(Block block, ModelResourceLocation mrl) {
            StateMapperBase ignoreState = new StateMapperBase() {
                @Nonnull
                @Override
                protected ModelResourceLocation getModelResourceLocation(@Nonnull IBlockState iBlockState) {
                    return mrl;
                }
            };
            ModelLoader.setCustomStateMapper(block, ignoreState);
        }
    }

}
