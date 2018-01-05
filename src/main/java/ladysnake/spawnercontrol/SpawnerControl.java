package ladysnake.spawnercontrol;

import ladysnake.spawnercontrol.controlledspawner.BlockControlledSpawner;
import ladysnake.spawnercontrol.controlledspawner.TileEntityControlledSpawner;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = SpawnerControl.MOD_ID,
        name = SpawnerControl.MOD_NAME,
        version = SpawnerControl.VERSION
)
public class SpawnerControl {

    public static final String MOD_ID = "spawnercontrol";
    public static final String MOD_NAME = "SpawnerControl";
    public static final String VERSION = "1.0";

    public static Logger LOGGER;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
        GameRegistry.registerTileEntity(TileEntityControlledSpawner.class, "spawnercontrol:controlled_spawner");
    }

    @GameRegistry.ObjectHolder(MOD_ID)
    public static class ModBlocks {
        public static final Block CONTROLLED_SPAWNER = Blocks.AIR;
    }

    @GameRegistry.ObjectHolder(MOD_ID)
    public static class ModItems {
        public static final Item CONTROLLED_SPAWNER = Items.AIR;
    }

    @Mod.EventBusSubscriber
    public static class RegistryHandler {

        @SubscribeEvent
        public static void onBlockRegister(RegistryEvent.Register<Block> event) {
            event.getRegistry().register(new BlockControlledSpawner().setRegistryName("controlled_spawner").setUnlocalizedName("spawnercontrol.controlled_spawner"));
        }

        @SubscribeEvent
        public static void onItemRegister(RegistryEvent.Register<Item> event) {
            event.getRegistry().register(new ItemBlock(ModBlocks.CONTROLLED_SPAWNER).setRegistryName("controlled_spawner").setUnlocalizedName("spawnercontrol.controlled_spawner"));
        }

        @SideOnly(Side.CLIENT)
        @SubscribeEvent
        public void registerRenders(ModelRegistryEvent event) {
            ModelLoader.setCustomModelResourceLocation(ModItems.CONTROLLED_SPAWNER, 0, new ModelResourceLocation("spawnercontrol:controlled_spawner"));
        }

    }

}
