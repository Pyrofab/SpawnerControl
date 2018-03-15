package ladysnake.spawnercontrol;

import ladysnake.spawnercontrol.config.MSCConfig;
import ladysnake.spawnercontrol.config.SpawnerConfig;
import ladysnake.spawnercontrol.controlledspawner.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockMobSpawner;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.IMob;
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
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.*;

/**
 * Class handling spawner-related events
 */
@Mod.EventBusSubscriber(modid = SpawnerControl.MOD_ID)
public class SpawnerEventHandler {

    /**Set containing all mob spawner tile entities that have been constructed this tick*/
    private static Set<TileEntityMobSpawner> spawners;

    static {
        // synchronize the set just in case forge's guess for logical side is wrong
        spawners = Collections.synchronizedSet(new HashSet<TileEntityMobSpawner>());
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<TileEntity> event) {
        // check the side to avoid adding client tile entities to the set, the world isn't set at this time
        if (event.getObject() instanceof TileEntityMobSpawner) {
            TileEntityMobSpawner spawner = (TileEntityMobSpawner) event.getObject();
            if (MSCConfig.alterVanillaSpawner && FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER) {
                //need to wait a tick after construction, as the field will be reassigned
                spawners.add(spawner);
            }
            if (!(spawner instanceof TileEntityControlledSpawner)) // custom spawners have their own handler
                event.addCapability(CapabilityControllableSpawner.CAPABILITY_KEY, new CapabilityControllableSpawner.Provider(spawner));
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

    /**
     * Runs the main logic of the mod as well as most of the logic associated with {@link SpawnerConfig.SpawnConditions}
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCheckSpawnerSpawn(LivingSpawnEvent.CheckSpawn event) {
        // don't affect natural spawns
        if (event.getSpawner() != null) {
            SpawnerConfig cfg = SpawnerUtil.getConfig(event.getWorld(), event.getSpawner().getSpawnerPosition());
            if (cfg == null) return;
            // Runs logic associated with SpawnConditions
            if (event.getResult() == Event.Result.DEFAULT) {
                EntityLiving spawned = (EntityLiving) event.getEntityLiving();
                // keep the collision check because mobs spawning in walls is not good
                boolean canSpawn = spawned.isNotColliding();

                // Tweaks spawners to prevent light from disabling spawns, except when the entity can see the sun
                if (cfg.spawnConditions.forceSpawnerMobSpawns && event.getEntity() instanceof IMob) {
                    if (cfg.spawnConditions.checkSunlight)
                        canSpawn &= !(event.getWorld().canSeeSky(new BlockPos(spawned)) && event.getWorld().isDaytime());
                } else if (!cfg.spawnConditions.forceSpawnerAllSpawns)
                    return; // this entity is not affected, do not interfere
                event.setResult(canSpawn ? Event.Result.ALLOW : Event.Result.DENY);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        long spawnerPos = event.getEntityLiving().getEntityData().getLong(ControlledSpawnerLogic.NBT_TAG_SPAWNER_POS);
        if (spawnerPos != 0) {
            TileEntity tile = event.getEntityLiving().getEntityWorld().getTileEntity(BlockPos.fromLong(spawnerPos));
            if (tile instanceof TileEntityMobSpawner) {
                TileEntityMobSpawner spawner = (TileEntityMobSpawner) tile;
                IControllableSpawner handler = CapabilityControllableSpawner.getHandler(spawner);
                if (handler.getConfig().incrementOnMobDeath)
                    handler.incrementSpawnedMobsCount();
            }
        }
    }

    /**
     * Changes the amount of experience dropped by a spawner when broken.
     * Drops from the mod's spawner are also handled here
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        SpawnerConfig cfg = SpawnerUtil.getConfig(event.getWorld(), event.getPos());
        if (cfg != null) {
            int xp = cfg.xpDropped;
            if (cfg.randXpVariation > 0) {
                xp += event.getWorld().rand.nextInt(cfg.randXpVariation)
                        + event.getWorld().rand.nextInt(cfg.randXpVariation);
            }
            event.setExpToDrop(xp);
        }
    }

    /**
     * Adds items specified in the config to the spawner's drops.
     * Drops from this mod's own spawner are also handled here for convenience.
     */
    @SubscribeEvent
    public static void onBlockHarvestDrops(BlockEvent.HarvestDropsEvent event) {
        if (event.getHarvester() != null) {
            SpawnerConfig cfg;
            // need to use the block as the tile entity is already gone
            Block block = event.getState().getBlock();
            if (block instanceof BlockControlledSpawner)
                cfg = ((BlockControlledSpawner) block).getConfig();
            else if(block instanceof BlockMobSpawner && MSCConfig.alterVanillaSpawner)
                cfg = MSCConfig.vanillaSpawnerConfig;
            else return;

            List<ItemStack> drops = event.getDrops();

            for (String entry : cfg.itemsDropped) {
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
