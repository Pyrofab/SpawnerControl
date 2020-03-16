package ladysnake.spawnercontrol;

import com.google.common.annotations.VisibleForTesting;
import ladysnake.spawnercontrol.config.MscConfig;
import ladysnake.spawnercontrol.config.SpawnerConfig;
import ladysnake.spawnercontrol.controlledspawner.BlockControlledSpawner;
import ladysnake.spawnercontrol.controlledspawner.CapabilityControllableSpawner;
import ladysnake.spawnercontrol.controlledspawner.IControllableSpawner;
import ladysnake.spawnercontrol.controlledspawner.TileEntityControlledSpawner;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.SpawnerBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.tileentity.MobSpawnerTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.spawner.AbstractSpawner;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

/**
 * Class handling spawner-related events
 */
public final class SpawnerEventHandler {

    private static final String NBT_TAG_SPAWNER_POS = SpawnerControl.MOD_ID + ":spawnerPos";
    /**
     * Set caching all known mob spawner tile entities that are affected by the mod
     * TODO consider spawner entities as well (cf. {@link AbstractSpawner#getSpawnerEntity()})
     */
    // use weak references to avoid memory leaks, and synchronize the set in case forge's guess for logical side is wrong
    @VisibleForTesting
    Queue<WeakReference<MobSpawnerTileEntity>> allSpawners = new ConcurrentLinkedQueue<>();
    private final MscConfig mainConfig;

    public SpawnerEventHandler(MscConfig mainConfig) {
        this.mainConfig = mainConfig;
    }

    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<TileEntity> event) {
        // check the side to avoid adding client tile entities to the set, the world isn't set at this time
        if (event.getObject() instanceof MobSpawnerTileEntity) {
            MobSpawnerTileEntity spawner = (MobSpawnerTileEntity) event.getObject();
            if ((mainConfig.alterVanillaSpawner || spawner instanceof TileEntityControlledSpawner)) {
                allSpawners.add(new WeakReference<>(spawner));
            }
            if (!(spawner instanceof TileEntityControlledSpawner)) // custom spawners have their own handler
                event.addCapability(CapabilityControllableSpawner.CAPABILITY_KEY, new CapabilityControllableSpawner.Provider(spawner));
        }
    }

    @SubscribeEvent
    public void onTickWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.START || event.side == LogicalSide.CLIENT) return;
        for (Iterator<WeakReference<MobSpawnerTileEntity>> iterator = allSpawners.iterator(); iterator.hasNext(); ) {
            MobSpawnerTileEntity spawner = iterator.next().get();
            // maintain the spawner list independently to avoid the cost of iterating through every tile entity
            if (spawner == null || spawner.getWorld() == null || spawner.getWorld().isRemote || spawner.isRemoved()) {
                iterator.remove();
                continue;
            }
            if (spawner.getWorld() == event.world) {
                IControllableSpawner handler = CapabilityControllableSpawner.getHandler(spawner);
                // handle obsolete spawners
                if (!handler.canSpawn()) {
                    if (handler.getConfig().breakSpawner) {
                        spawner.getWorld().setBlockState(spawner.getPos(), Blocks.AIR.getDefaultState());
                    }
                    // spawn particles for disabled spawners
                    if (!spawner.getWorld().isRemote) {
                        double x = (float) spawner.getPos().getX() + spawner.getWorld().rand.nextFloat();
                        double y = (float) spawner.getPos().getY() + spawner.getWorld().rand.nextFloat();
                        double z = (float) spawner.getPos().getZ() + spawner.getWorld().rand.nextFloat();
                        ((ServerWorld) spawner.getWorld()).spawnParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 3, 0, 0, 0, 0.0);
                    }
                }
            }
        }
    }

    /**
     * Runs the main logic of the mod as well as most of the logic associated with {@link SpawnerConfig.SpawnConditions}
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onCheckSpawnerSpawn(LivingSpawnEvent.CheckSpawn event) {
        AbstractSpawner spawner = event.getSpawner();
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
            if (event.getResult() == Event.Result.DEFAULT && event.getEntityLiving() instanceof MobEntity) {
                MobEntity spawned = (MobEntity) event.getEntityLiving();
                // keep the collision check because mobs spawning in walls is not good
                canSpawn = spawned.isNotColliding(event.getWorld());
                // Tweaks pendingSpawners to prevent light from disabling spawns, except when the entity can see the sun
                if (cfg.spawnConditions.forceSpawnerMobSpawns && event.getEntity() instanceof IMob) {
                    if (cfg.spawnConditions.checkSunlight) {
                        canSpawn &= !(event.getWorld().canBlockSeeSky(new BlockPos(spawned)) && event.getWorld().getDimension().isDaytime());
                    }
                } else if (!cfg.spawnConditions.forceSpawnerAllSpawns)
                    canSpawn &= spawned.canSpawn(event.getWorld(), event.getSpawnReason()); // this entity is not affected, do not interfere
            } else {
                canSpawn = event.getResult() == Event.Result.ALLOW;
            }
            // increments spawn counts and prevents spawns if over the limit
            if (canSpawn) {
                if (handler.canSpawn()) {
                    if (!handler.getConfig().incrementOnMobDeath) {
                        handler.incrementSpawnedMobsCount();
                    }

                    CompoundNBT compound = new CompoundNBT();
                    World spawnerWorld = spawner.getWorld();
                    // When unit testing, the world will be null
                    //noinspection ConstantConditions
                    if (spawnerWorld != null) {
                        compound.putString("dimension", Objects.requireNonNull(spawnerWorld.getDimension().getType().getRegistryName()).toString());
                        compound.put("pos", NBTUtil.writeBlockPos(spawner.getSpawnerPosition()));
                        event.getEntity().getPersistentData().put(NBT_TAG_SPAWNER_POS, compound);
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
    public void onLivingSpawnSpecialSpawn(LivingSpawnEvent.SpecialSpawn event) {
        if (event.getSpawner() == null) return;     // this can't happen currently but it will with forge's event
        IControllableSpawner handler = SpawnerUtil.getHandlerIfAffected(event.getWorld(), event.getSpawner().getSpawnerPosition());
        if (handler == null) return;
        // just to make sure
        if (!handler.canSpawn()) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        CompoundNBT data = event.getEntityLiving().getPersistentData();
        if (data.contains(NBT_TAG_SPAWNER_POS)) {
            CompoundNBT nbt = data.getCompound(NBT_TAG_SPAWNER_POS);
            @SuppressWarnings("ConstantConditions") DimensionType dimension = DimensionType.byName(ResourceLocation.tryCreate(nbt.getString("dimension")));
            if (dimension != null) {
                World world = ((ServerWorld) event.getEntity().world).getServer().getWorld(dimension);
                TileEntity tile = world.getTileEntity(NBTUtil.readBlockPos(nbt.getCompound("pos")));
                if (tile instanceof MobSpawnerTileEntity) {
                    MobSpawnerTileEntity spawner = (MobSpawnerTileEntity) tile;
                    IControllableSpawner handler = CapabilityControllableSpawner.getHandler(spawner);
                    if (handler.getConfig().incrementOnMobDeath)
                        handler.incrementSpawnedMobsCount();
                }
            }
        }
    }

    @SubscribeEvent
    public void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        if (!event.getEntity().world.isRemote) return;

        SpawnerConfig cfg = SpawnerUtil.getConfig(((ServerWorld) event.getEntity().world).getServer(), event.getEntityLiving().getPersistentData().getCompound(NBT_TAG_SPAWNER_POS));
        if (cfg != null) {
            try {
                ResourceLocation rl = event.getEntity().getType().getRegistryName();
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
    public void onLivingItemDrop(LivingDropsEvent event) {
        LivingEntity livingBase = event.getEntityLiving();
        SpawnerConfig cfg = SpawnerUtil.getConfig(((ServerWorld) event.getEntity().world).getServer(), livingBase.getPersistentData().getCompound(NBT_TAG_SPAWNER_POS));
        if (cfg != null) {
            try {
                ResourceLocation rl = event.getEntity().getType().getRegistryName();
                if (rl != null) {
                    SpawnerConfig.MobLoot.MobLootEntry entry = cfg.mobLoot.lootEntries.get(rl);
                    Collection<ItemEntity> drops = event.getDrops();
                    if (entry.removeAllItems) {
                        drops.clear();
                    } else {
                        for (ItemEntity drop : drops.toArray(new ItemEntity[0])) {
                            ItemStack stack = drop.getItem();
                            for (String s : entry.removedItems) {
                                String[] split = s.split(":");
                                ResourceLocation dropId = ResourceLocation.tryCreate(split[0] + ":" + split[1]);
                                if (Objects.equals(stack.getItem().getRegistryName(), dropId)
// TODO replace with NBT check                                       && (split.length < 3 || stack.getMetadata() == Integer.parseInt(split[2]))
                                ) {
                                    drops.remove(drop);
                                }
                            }
                        }
                    }
                    World world = livingBase.world;
                    double x = livingBase.getX(), y = livingBase.getY(), z = livingBase.getZ();
                    for (String s : entry.addedItems) {
                        String[] split = s.split(":");
                        if (split.length < 5 || world.rand.nextDouble() < Double.parseDouble(split[4])) {
                            ResourceLocation itemRL = new ResourceLocation(split[0], split[1]);
                            Item item = ForgeRegistries.ITEMS.getValue(itemRL);
                            if (item == null) {
                                // the block registry has a default value
                                item = Objects.requireNonNull(ForgeRegistries.BLOCKS.getValue(itemRL)).asItem();
                            }
                            if (item != Items.AIR) {
                                int quantity = split.length < 3 ? 1 : Integer.parseInt(split[2]);
                                // TODO handle custom NBT
//                                int meta = split.length < 4 ? 0 : Integer.parseInt(split[3]);
                                drops.add(new ItemEntity(world, x, y, z, new ItemStack(item, quantity)));
                            } else {
                                SpawnerControl.LOGGER.error("Error while handling spawned item drops");
                            }
                        }
                    }
                }
            } catch (NumberFormatException | ExecutionException e) {
                SpawnerControl.LOGGER.error("Error while handling spawned item drops", e);
            }
        }
    }

    /**
     * Changes the amount of experience dropped by a spawner when broken.
     * Drops from the mod's spawner are also handled here
     */
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        SpawnerConfig cfg = SpawnerUtil.getConfig(event.getWorld(), event.getPos());
        Random rand = event.getWorld().getRandom();
        if (cfg != null) {
            int xp = cfg.xpDropped;
            if (cfg.randXpVariation > 0) {
                xp += rand.nextInt(cfg.randXpVariation)
                        + rand.nextInt(cfg.randXpVariation);
            }
            event.setExpToDrop(xp);
        }
    }

    /**
     * Adds items specified in the config to the spawner's drops.
     * Drops from this mod's own spawner are also handled here for convenience.
     */
    @SubscribeEvent
    public void onBlockHarvestDrops(BlockEvent.HarvestDropsEvent event) {
        if (event.getHarvester() != null) {
            SpawnerConfig cfg;
            // need to use the block as the tile entity is already gone
            Block block = event.getState().getBlock();
            if (block instanceof BlockControlledSpawner)
                cfg = ((BlockControlledSpawner) block).getConfig();
            else if (block instanceof SpawnerBlock && mainConfig.alterVanillaSpawner)
                cfg = mainConfig.vanillaSpawnerConfig;
            else return;

            List<ItemStack> drops = event.getDrops();

            for (String entry : cfg.itemsDropped) {
                String[] split = entry.split(":");
                if (split.length > 1) {
                    Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(split[0], split[1]));
                    try {
                        // get additional properties for the item stack
                        if (item != null) {
                            int count = split.length >= 3 ? Integer.parseInt(split[2]) : 1;
                            // TODO handle custom NBT
//                            int meta = split.length >= 4 ? Integer.parseInt(split[3]) : 0;
                            // default chance is 1
                            if (split.length < 5 || event.getWorld().getRandom().nextDouble() < Double.parseDouble(split[4]))
                                drops.add(new ItemStack(item, count));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
    }
}
