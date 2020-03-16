package ladysnake.spawnercontrol;

import ladysnake.spawnercontrol.config.MscConfigManager;
import ladysnake.spawnercontrol.config.SpawnerConfigHolder;
import ladysnake.spawnercontrol.controlledspawner.CapabilityControllableSpawner;
import ladysnake.spawnercontrol.controlledspawner.IControllableSpawner;
import ladysnake.spawnercontrol.controlledspawner.TileEntityControlledSpawner;
import me.zeroeightsix.fiber.exception.FiberException;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.IForgeRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.stream.Stream;

/*
    modid = SpawnerControl.MOD_ID,
    name = SpawnerControl.MOD_NAME,
    version = SpawnerControl.VERSION,
    acceptedMinecraftVersions = SpawnerControl.ACCEPTED_VERSIONS,
    guiFactory = "ladysnake.spawnercontrol.client.MSCConfigFactory",
    dependencies = "required-after:forge@[14.23.2.2596,);",
    acceptableRemoteVersions = "*"
*/
@Mod(SpawnerControl.MOD_ID)
public class SpawnerControl {

    public static final String MOD_ID = "spawnercontrol";
    public static final Logger LOGGER = LogManager.getLogger("Mob Spawner Control");
    private static SpawnerControl instance;

    public static SpawnerControl instance() {
        if (instance == null) {
            throw new IllegalStateException("Not yet initialized");
        }
        return instance;
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    private final MscConfigManager configManager = new MscConfigManager();

    {
        // bad constructor side effects
        instance = this;
        this.preInit();
    }

    public void preInit() {
        try {
            this.configManager.init();
        } catch (FiberException | IOException e) {
            SpawnerControl.LOGGER.error("Failed to load config", e);
        }
        IEventBus setupBus = FMLJavaModLoadingContext.get().getModEventBus();
        setupBus.register(this);
        setupBus.register(new RegistryHandler());
        MinecraftForge.EVENT_BUS.register(new SpawnerEventHandler(this.configManager.getMainConfig()));
    }

    @SubscribeEvent
    public void init(FMLCommonSetupEvent event) {
        // register the capability storing extra spawner information
        CapabilityManager.INSTANCE.register(IControllableSpawner.class, new CapabilityControllableSpawner.Storage(), CapabilityControllableSpawner.DefaultControllableSpawner::new);
    }

    public MscConfigManager getConfigManager() {
        return this.configManager;
    }

    public final class RegistryHandler {
        private final ItemGroup creativeTab = new ItemGroup(MOD_ID) {
            @Nonnull
            @Override
            public ItemStack createIcon() {
                return new ItemStack(Blocks.SPAWNER);
            }
        };

        @SubscribeEvent
        public void onBlockRegister(RegistryEvent.Register<Block> event) {
            IForgeRegistry<Block> blocks = event.getRegistry();
            for (SpawnerConfigHolder cfg : SpawnerControl.this.configManager.getCustomSpawnerConfigs()) {
                blocks.register(cfg.createBlock());
            }
        }

        @SubscribeEvent
        public void onItemRegister(RegistryEvent.Register<Item> event) {
            Item.Properties properties = new Item.Properties();
            if (SpawnerControl.this.configManager.getMainConfig().makeCreativeTab) {
                properties.group(creativeTab);
            }
            IForgeRegistry<Item> items = event.getRegistry();
            for (SpawnerConfigHolder cfg : SpawnerControl.this.configManager.getCustomSpawnerConfigs()) {
                items.register(cfg.createItem(properties));
            }
        }

        @SubscribeEvent
        public void onTeRegister(RegistryEvent.Register<TileEntityType<?>> event) {
            // No need to register a tile entity that's not used anywhere
            if (SpawnerControl.this.configManager.getMainConfig().customSpawners.length > 0) {
                //noinspection ConstantConditions
                event.getRegistry().register(TileEntityType.Builder.create(
                        TileEntityControlledSpawner::new,
                        SpawnerControl.this.configManager.getCustomSpawnerConfigs().stream().map(SpawnerConfigHolder::getBlock).toArray(Block[]::new)
                ).build(null).setRegistryName(TileEntityControlledSpawner.TYPE_ID));
            }
        }

        @SubscribeEvent
        @OnlyIn(Dist.CLIENT)
        public void registerRenders(ModelBakeEvent event) {
            ModelLoader modelLoader = event.getModelLoader();
            ModelResourceLocation spawnerModelId = new ModelResourceLocation("mob_spawner", "inventory");
            IUnbakedModel defaultSpawnerModel = modelLoader.getUnbakedModel(spawnerModelId);

            for (ModelResourceLocation modelId : gatherMissingSpawnerModels(modelLoader)) {
                modelLoader.field_217849_F.put(modelId, defaultSpawnerModel);
            }
        }

        @Contract(pure = true)
        private Iterable<ModelResourceLocation> gatherMissingSpawnerModels(ModelLoader modelLoader) {
            IUnbakedModel missingModel = modelLoader.getUnbakedModel(ModelBakery.MODEL_MISSING);
            return SpawnerControl.this.configManager.getCustomSpawnerConfigs().stream()
                    .flatMap(spawnerConfigHolder -> Stream.concat(
                            spawnerConfigHolder.getBlock().getStateContainer().getValidStates().stream().map(BlockModelShapes::getModelLocation),
                            Stream.of(ModelLoader.getInventoryVariant(spawnerConfigHolder.getRegistryName().toString()))
                    )).filter(id -> modelLoader.getModelOrMissing(id) == missingModel)::iterator;
        }
    }
}
