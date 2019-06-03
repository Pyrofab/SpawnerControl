package ladysnake.spawnercontrol;

import com.google.common.annotations.VisibleForTesting;
import ladysnake.spawnercontrol.config.MSCConfig;
import ladysnake.spawnercontrol.config.SpawnerConfig;
import ladysnake.spawnercontrol.controlledspawner.BlockControlledSpawner;
import ladysnake.spawnercontrol.controlledspawner.CapabilityControllableSpawner;
import ladysnake.spawnercontrol.controlledspawner.IControllableSpawner;
import ladysnake.spawnercontrol.controlledspawner.TileEntityControlledSpawner;
import net.minecraft.block.Block;
import net.minecraft.block.BlockMobSpawner;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.IMob;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Class handling spawner-related events
 */
@Mod.EventBusSubscriber(modid = SpawnerControl.MOD_ID)
public class SpawnerEventHandler {

    private static final String NBT_TAG_SPAWNER_POS = SpawnerControl.MOD_ID + ":spawnerPos";
    /**
     * Set caching all known mob spawner tile entities that are affected by the mod
     * TODO consider spawner entities as well (cf. {@link MobSpawnerBaseLogic#getSpawnerEntity()})
     */
    @VisibleForTesting
    static Set<TileEntityMobSpawner> allSpawners;

    static {
        // use weak references to avoid memory leaks, and synchronize the set in case forge's guess for logical side is wrong
        allSpawners = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<TileEntity> event) {
        // check the side to avoid adding client tile entities to the set, the world isn't set at this time
        if (event.getObject() instanceof TileEntityMobSpawner) {
            TileEntityMobSpawner spawner = (TileEntityMobSpawner) event.getObject();
            if ((MSCConfig.alterVanillaSpawner || spawner instanceof TileEntityControlledSpawner)
                    && FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER) {
                allSpawners.add(spawner);
            }
            if (!(spawner instanceof TileEntityControlledSpawner)) // custom spawners have their own handler
                event.addCapability(CapabilityControllableSpawner.CAPABILITY_KEY, new CapabilityControllableSpawner.Provider(spawner));
        }
    }

    @SubscribeEvent
    public static void onTickWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.START || event.side == Side.CLIENT) return;
        for (Iterator<TileEntityMobSpawner> iterator = allSpawners.iterator(); iterator.hasNext(); ) {
            TileEntityMobSpawner spawner = iterator.next();
            // maintain the spawner list independently to avoid the cost of iterating through every tile entity
            if (spawner.isInvalid()) {
                iterator.remove();
                continue;
            }
            if (spawner.getWorld() == event.world) {
                IControllableSpawner handler = CapabilityControllableSpawner.getHandler(spawner);
                // handle obsolete spawners
                if (!handler.canSpawn()) {
                    if (handler.getConfig().breakSpawner)
                        spawner.getWorld().setBlockToAir(spawner.getPos());
                    // spawn particles for disabled spawners
                    if (!spawner.getWorld().isRemote) {
                        double x = (double) ((float) spawner.getPos().getX() + spawner.getWorld().rand.nextFloat());
                        double y = (double) ((float) spawner.getPos().getY() + spawner.getWorld().rand.nextFloat());
                        double z = (double) ((float) spawner.getPos().getZ() + spawner.getWorld().rand.nextFloat());
                        ((WorldServer) spawner.getWorld()).spawnParticle(EnumParticleTypes.SMOKE_LARGE, x, y, z, 3, 0, 0, 0, 0.0);
                    }
                }
            }
        }
    }

    /**
     * Runs the main logic of the mod as well as most of the logic associated with {@link SpawnerConfig.SpawnConditions}
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCheckSpawnerSpawn(LivingSpawnEvent.CheckSpawn event) {
        MobSpawnerBaseLogic spawner = event.getSpawner();
        // only affect spawner-caused spawns
        if (spawner != null) {
            // if there is no tile entity nor spawner entity, the spawner was very likely destroyed by a previous spawn of the same batch
            if (event.getWorld().getTileEntity(spawner.getSpawnerPosition()) == null && spawner.getSpawnerEntity() == null) {
                event.setResult(Event.Result.DENY);
            }
            IControllableSpawner handler = SpawnerUtil.getHandlerIfAffected(event.getWorld(), spawner.getSpawnerPosition());
            if (handler == null) return;

            SpawnerConfig cfg = handler.getConfig();
            boolean canSpawn;

            // Runs logic associated with SpawnConditions
            if (event.getResult() == Event.Result.DEFAULT && event.getEntityLiving() instanceof EntityLiving) {
                EntityLiving spawned = (EntityLiving) event.getEntityLiving();
                // keep the collision check because mobs spawning in walls is not good
                canSpawn = spawned.isNotColliding();
                // Tweaks pendingSpawners to prevent light from disabling spawns, except when the entity can see the sun
                if (cfg.spawnConditions.forceSpawnerMobSpawns && event.getEntity() instanceof IMob) {
                    if (cfg.spawnConditions.checkSunlight)
                        canSpawn &= !(event.getWorld().canSeeSky(new BlockPos(spawned)) && event.getWorld().isDaytime());
                } else if (!cfg.spawnConditions.forceSpawnerAllSpawns)
                    canSpawn &= spawned.getCanSpawnHere(); // this entity is not affected, do not interfere
            } else {
                canSpawn = event.getResult() == Event.Result.ALLOW;
            }
            // increments spawn counts and prevents spawns if over the limit
            if (canSpawn) {
                if (handler.canSpawn()) {
                    if (!handler.getConfig().incrementOnMobDeath)
                        handler.incrementSpawnedMobsCount();

                    NBTTagCompound compound = new NBTTagCompound();
                    World spawnerWorld = spawner.getSpawnerWorld();
                    // When unit testing, the world will be null
                    //noinspection ConstantConditions
                    if (spawnerWorld != null) {
                        compound.setInteger("dimension", spawnerWorld.provider.getDimension());
                        compound.setLong("pos", spawner.getSpawnerPosition().toLong());
                        event.getEntity().getEntityData().setTag(NBT_TAG_SPAWNER_POS, compound);
                    }
                    event.setResult(Event.Result.ALLOW);
                } else {
                    event.setResult(Event.Result.DENY);
                }
            } else {
                event.setResult(Event.Result.DENY);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingSpawnSpecialSpawn(LivingSpawnEvent.SpecialSpawn event) {
        if (event.getSpawner() == null) return;     // this can't happen currently but it will with forge's event
        IControllableSpawner handler = SpawnerUtil.getHandlerIfAffected(event.getWorld(), event.getSpawner().getSpawnerPosition());
        if (handler == null) return;
        // just to make sure
        if (!handler.canSpawn()) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        NBTTagCompound data = event.getEntityLiving().getEntityData();
        if (data.hasKey(NBT_TAG_SPAWNER_POS)) {
            NBTBase nbt = event.getEntityLiving().getEntityData().getTag(NBT_TAG_SPAWNER_POS);
            World world;
            long spawnerPos;
            if (nbt instanceof NBTTagCompound) {
                spawnerPos = ((NBTTagCompound) nbt).getLong("pos");
                world = FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(((NBTTagCompound) nbt).getInteger("dimension"));
            } else if (nbt instanceof NBTTagLong) {
                spawnerPos = ((NBTTagLong) nbt).getLong();
                world = event.getEntity().getEntityWorld();
            } else return;
            TileEntity tile = world.getTileEntity(BlockPos.fromLong(spawnerPos));
            if (tile instanceof TileEntityMobSpawner) {
                TileEntityMobSpawner spawner = (TileEntityMobSpawner) tile;
                IControllableSpawner handler = CapabilityControllableSpawner.getHandler(spawner);
                if (handler.getConfig().incrementOnMobDeath)
                    handler.incrementSpawnedMobsCount();
            }
        }
    }

    @SubscribeEvent
    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        SpawnerConfig cfg = SpawnerUtil.getConfig(event.getEntityLiving().getEntityData().getTag(NBT_TAG_SPAWNER_POS));
        if (cfg != null) {
            try {
                ResourceLocation rl = EntityList.getKey(event.getEntity());
                if (rl != null) {
                    SpawnerConfig.MobLoot.MobLootEntry entry = cfg.mobLoot.lootEntries.get(rl);
                    event.setDroppedExperience(MathHelper.floor(event.getOriginalExperience() * entry.xpMultiplier + entry.flatXpIncrease + (event.getDroppedExperience() - event.getOriginalExperience())));
                }
            } catch (ExecutionException e) {
                SpawnerControl.LOGGER.error("Error while handling spawned experience drop", e);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingItemDrop(LivingDropsEvent event) {
        EntityLivingBase livingBase = event.getEntityLiving();
        SpawnerConfig cfg = SpawnerUtil.getConfig(livingBase.getEntityData().getTag(NBT_TAG_SPAWNER_POS));
        if (cfg != null) {
            try {
                ResourceLocation rl = EntityList.getKey(event.getEntity());
                if (rl != null) {
                    SpawnerConfig.MobLoot.MobLootEntry entry = cfg.mobLoot.lootEntries.get(rl);
                    List<EntityItem> drops = event.getDrops();
                    if (entry.removeAllItems) {
                        drops.clear();
                    }
                    else {
                        for (EntityItem drop : drops.toArray(new EntityItem[0])) {
                            ItemStack stack = drop.getItem();
                            for (String s : entry.removedItems) {
                                String[] split = s.split(":");
                                if (stack.getUnlocalizedName().equals(split[0] + ":" + split[1])
                                        && (split.length < 3 || stack.getMetadata() == Integer.parseInt(split[2]))) {
                                    drops.remove(drop);
                                }
                            }
                        }
                    }
                    World world = livingBase.world;
                    double x = livingBase.posX, y = livingBase.posY, z = livingBase.posZ;
                    for (String s : entry.addedItems)
                    {
                        String[] split = s.split(":");
                        if (split.length < 5 || world.rand.nextFloat() < Double.parseDouble(split[4])) {
                            ResourceLocation itemRL = new ResourceLocation(split[0], split[1]);
                            Item item = ForgeRegistries.ITEMS.getValue(itemRL);
                            if (item == null)
                            {
                                //Try/catch handles this if null
                                item = Item.getItemFromBlock(ForgeRegistries.BLOCKS.getValue(itemRL));
                            }
                            drops.add(new EntityItem(world, x, y, z, new ItemStack(item, split.length < 3 ? 1 : Integer.parseInt(split[2]), split.length < 4 ? 0 : Integer.parseInt(split[3]))));
                        }
                    }
                }
            } catch (Exception e) {
                SpawnerControl.LOGGER.error("Error while handling spawned item drops", e);
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
            else if (block instanceof BlockMobSpawner && MSCConfig.alterVanillaSpawner)
                cfg = MSCConfig.vanillaSpawnerConfig;
            else return;

            List<ItemStack> drops = event.getDrops();

            for (String entry : cfg.itemsDropped) {
                String[] split = entry.split(":");
                if (split.length > 1) {
                    Item item = Item.REGISTRY.getObject(new ResourceLocation(split[0], split[1]));
                    try {
                        // get additional properties for the item stack
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
