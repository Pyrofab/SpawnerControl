package ladysnake.spawnercontrol;

import net.minecraft.block.BlockMobSpawner;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Class handling spawner-related events
 */
@Mod.EventBusSubscriber(modid = SpawnerControl.MOD_ID)
public class SpawnerEventHandler {

    private static Set<TileEntityMobSpawner> spawners = new HashSet<TileEntityMobSpawner>();

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<TileEntity> event) {
        if (event.getObject() instanceof TileEntityMobSpawner) {
            //need to wait a tick after construction, as the field will be reassigned
            spawners.add((TileEntityMobSpawner) event.getObject());
        }
    }

    @SubscribeEvent
    public static void onTickWorldTick(TickEvent.WorldTickEvent event) {
        for (Iterator<TileEntityMobSpawner> iterator = spawners.iterator(); iterator.hasNext(); ) {
            TileEntityMobSpawner spawner = iterator.next();
            spawner.spawnerLogic = new ControlledSpawnerLogic(spawner);
            iterator.remove();
        }
    }

    /**
     * Changes the amount of experience dropped by a spawner when broken
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getState().getBlock() instanceof BlockMobSpawner) {
            int xp = Configuration.xpDropped;
            if (Configuration.randXpVariation > 0) {
                xp += event.getWorld().rand.nextInt(Configuration.randXpVariation)
                        + event.getWorld().rand.nextInt(Configuration.randXpVariation);
            }
            event.setExpToDrop(xp);
        }
    }

    /**
     * Adds items specified in the config to the spawner's drops
     */
    @SubscribeEvent
    public static void onBlockHarvestDrops(BlockEvent.HarvestDropsEvent event) {
        if (event.getHarvester() != null && event.getState().getBlock() instanceof BlockMobSpawner) {
            List<ItemStack> drops = event.getDrops();

            for (String entry : Configuration.itemsDropped) {
                String[] split = entry.split(":");
                if (split.length > 1) {
                    Item item = Item.REGISTRY.getObject(new ResourceLocation(split[0], split[1]));
                    try {
                        if (item != null) {
                            int count = split.length >= 3 ? Integer.parseInt(split[2]) : 1;
                            int meta = split.length >= 4 ? Integer.parseInt(split[3]) : 0;
                            drops.add(new ItemStack(item, count, meta));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
    }
}
