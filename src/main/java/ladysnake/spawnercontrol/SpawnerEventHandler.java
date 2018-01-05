package ladysnake.spawnercontrol;

import ladysnake.spawnercontrol.controlledspawner.BlockControlledSpawner;
import ladysnake.spawnercontrol.controlledspawner.ControlledSpawnerLogic;
import net.minecraft.block.BlockMobSpawner;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.*;

/**
 * Class handling spawner-related events
 */
@Mod.EventBusSubscriber(modid = SpawnerControl.MOD_ID)
public class SpawnerEventHandler {

    /**Set containing all mob spawners entities that have been constructed this tick*/
    private static Set<TileEntityMobSpawner> spawners;

    static {
        // synchronize the set just in case forge's guess for logical side is wrong
        spawners = Collections.synchronizedSet(new HashSet<TileEntityMobSpawner>());
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<TileEntity> event) {
        // check the side to avoid adding client tile entities to the set, the world isn't set at this time
        if (Configuration.alterVanillaSpawner && event.getObject() instanceof TileEntityMobSpawner && FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER) {
            //need to wait a tick after construction, as the field will be reassigned
            spawners.add((TileEntityMobSpawner) event.getObject());
        }
    }

    @SubscribeEvent
    public static void onTickWorldTick(TickEvent.WorldTickEvent event) {
        for (Iterator<TileEntityMobSpawner> iterator = spawners.iterator(); iterator.hasNext(); ) {
            TileEntityMobSpawner spawner = iterator.next();
            MobSpawnerBaseLogic logic = new ControlledSpawnerLogic(spawner);
            // preserve the spawn information
            logic.readFromNBT(spawner.spawnerLogic.writeToNBT(new NBTTagCompound()));
            spawner.spawnerLogic = logic;
            iterator.remove();
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!Configuration.incrementOnMobDeath) return;
        long spawnerPos = event.getEntityLiving().getEntityData().getLong(ControlledSpawnerLogic.NBT_TAG_SPAWNER_POS);
        if (spawnerPos != 0) {
            TileEntity tile = event.getEntityLiving().getEntityWorld().getTileEntity(BlockPos.fromLong(spawnerPos));
            if (tile instanceof TileEntityMobSpawner) {
                TileEntityMobSpawner spawner = (TileEntityMobSpawner) tile;
                if (spawner.spawnerLogic instanceof ControlledSpawnerLogic)
                    ((ControlledSpawnerLogic) spawner.spawnerLogic).incrementSpawnedMobsCount();
                else
                    SpawnerControl.LOGGER.warn("A mob spawned by the mod points toward an unmodified spawner, skipping");
            }
        }
    }

    /**
     * Changes the amount of experience dropped by a spawner when broken
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getState().getBlock() instanceof BlockMobSpawner && Configuration.alterVanillaSpawner
                // drops from the mod's spawner is also handled here
                || event.getState().getBlock() instanceof BlockControlledSpawner) {
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
        if (event.getHarvester() != null && (event.getState().getBlock() instanceof BlockMobSpawner && Configuration.alterVanillaSpawner
                // drops from the mod's spawner is also handled here
                || event.getState().getBlock() instanceof BlockControlledSpawner)) {
            List<ItemStack> drops = event.getDrops();

            for (String entry : Configuration.itemsDropped) {
                String[] split = entry.split(":");
                if (split.length > 1) {
                    Item item = Item.REGISTRY.getObject(new ResourceLocation(split[0], split[1]));
                    try {
                        if (item != null) {
                            int count = split.length >= 3 ? Integer.parseInt(split[2]) : 1;
                            int meta = split.length >= 4 ? Integer.parseInt(split[3]) : 0;
                            // default chance is 1
                            if (split.length < 5 || event.getWorld().rand.nextFloat() < Double.parseDouble(split[4]))
                                drops.add(new ItemStack(item, count, meta));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
    }
}
