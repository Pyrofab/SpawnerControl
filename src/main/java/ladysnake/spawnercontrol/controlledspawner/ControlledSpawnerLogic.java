package ladysnake.spawnercontrol.controlledspawner;

import com.google.common.collect.Lists;
import ladysnake.spawnercontrol.Configuration;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Almost the same as the vanilla one, everything is just copied here because all the fields are private and I'm lazy
 */
public class ControlledSpawnerLogic extends MobSpawnerBaseLogic {

    public static final String NBT_TAG_SPAWNER_POS = "spawnercontrol:spawnerPos";

    private TileEntityMobSpawner tileEntityControlledSpawner;
    /**
     * The delay to spawn.
     */
    private int spawnDelay = 20;
    /**
     * List of potential entities to spawn
     */
    private final List<WeightedSpawnerEntity> potentialSpawns = Lists.newArrayList();
    // Don't ever rename that field or Nut will be angery
    private WeightedSpawnerEntity spawnData = new WeightedSpawnerEntity();
    /**
     * The rotation of the mob inside the mob spawner
     */
    private double mobRotation;
    /**
     * the previous rotation of the mob inside the mob spawner
     */
    private double prevMobRotation;
    private int minSpawnDelay = 200;
    private int maxSpawnDelay = 800;
    private int spawnCount = 4;
    /**
     * Cached instance of the entity to render inside the spawner.
     */
    private Entity cachedEntity;
    private int maxNearbyEntities = 6;
    /**
     * The distance from which a player activates the spawner.
     */
    private int activatingRangeFromPlayer = 16;
    /**
     * The range coefficient for spawning entities around.
     */
    private int spawnRange = 4;

    private int spawnedMobsCount;

    public ControlledSpawnerLogic(TileEntityMobSpawner tileEntityControlledSpawner) {
        super();
        this.tileEntityControlledSpawner = tileEntityControlledSpawner;
    }

    @Nullable
    private ResourceLocation getEntityId() {
        String s = this.spawnData.getNbt().getString("id");
        return StringUtils.isNullOrEmpty(s) ? null : new ResourceLocation(s);
    }

    public void setEntityId(@Nullable ResourceLocation id) {
        if (id != null) {
            this.spawnData.getNbt().setString("id", id.toString());
        }
    }

    /**
     * Returns true if there's a player close enough to this mob spawner to activate it AND the spawner hasn't reached its max spawn count.
     */
    private boolean isActivated() {
        BlockPos blockpos = this.getSpawnerPosition();
        return this.spawnedMobsCount < Configuration.mobThreshold
                && this.getSpawnerWorld().isAnyPlayerWithinRangeAt((double) blockpos.getX() + 0.5D, (double) blockpos.getY() + 0.5D, (double) blockpos.getZ() + 0.5D, (double) this.activatingRangeFromPlayer);
    }

    @Override
    public void updateSpawner() {
        BlockPos blockpos = this.getSpawnerPosition();
        if (!this.isActivated()) {
            this.prevMobRotation = this.mobRotation;
            if (this.spawnedMobsCount >= Configuration.mobThreshold) {
                if (Configuration.breakSpawner)
                    getSpawnerWorld().setBlockToAir(getSpawnerPosition());
                if (!this.getSpawnerWorld().isRemote) {
                    double d3 = (double) ((float) blockpos.getX() + this.getSpawnerWorld().rand.nextFloat());
                    double d4 = (double) ((float) blockpos.getY() + this.getSpawnerWorld().rand.nextFloat());
                    double d5 = (double) ((float) blockpos.getZ() + this.getSpawnerWorld().rand.nextFloat());
                    ((WorldServer) this.getSpawnerWorld()).spawnParticle(EnumParticleTypes.SMOKE_LARGE, d3, d4, d5, 3, 0, 0, 0, 0.0);
                }
            }
        } else {
            if (this.getSpawnerWorld().isRemote) {
                double d3 = (double) ((float) blockpos.getX() + this.getSpawnerWorld().rand.nextFloat());
                double d4 = (double) ((float) blockpos.getY() + this.getSpawnerWorld().rand.nextFloat());
                double d5 = (double) ((float) blockpos.getZ() + this.getSpawnerWorld().rand.nextFloat());
                this.getSpawnerWorld().spawnParticle(EnumParticleTypes.SMOKE_NORMAL, d3, d4, d5, 0.0D, 0.0D, 0.0D);
                this.getSpawnerWorld().spawnParticle(EnumParticleTypes.FLAME, d3, d4, d5, 0.0D, 0.0D, 0.0D);

                if (this.spawnDelay > 0) {
                    --this.spawnDelay;
                }

                this.prevMobRotation = this.mobRotation;
                this.mobRotation = (this.mobRotation + (double) (1000.0F / ((float) this.spawnDelay + 200.0F))) % 360.0D;
            } else {
                if (this.spawnDelay == -1) {
                    this.resetTimer();
                }

                if (this.spawnDelay > 0) {
                    --this.spawnDelay;
                    return;
                }

                boolean flag = false;

                for (int i = 0; i < this.spawnCount; ++i) {
                    NBTTagCompound nbttagcompound = this.spawnData.getNbt();
                    NBTTagList nbttaglist = nbttagcompound.getTagList("Pos", 6);
                    World world = this.getSpawnerWorld();
                    int j = nbttaglist.tagCount();
                    double d0 = j >= 1 ? nbttaglist.getDoubleAt(0) : (double) blockpos.getX() + (world.rand.nextDouble() - world.rand.nextDouble()) * (double) this.spawnRange + 0.5D;
                    double d1 = j >= 2 ? nbttaglist.getDoubleAt(1) : (double) (blockpos.getY() + world.rand.nextInt(3) - 1);
                    double d2 = j >= 3 ? nbttaglist.getDoubleAt(2) : (double) blockpos.getZ() + (world.rand.nextDouble() - world.rand.nextDouble()) * (double) this.spawnRange + 0.5D;
                    Entity entity = AnvilChunkLoader.readWorldEntityPos(nbttagcompound, world, d0, d1, d2, false);

                    if (entity == null) {
                        return;
                    }

                    int k = world.getEntitiesWithinAABB(entity.getClass(), (new AxisAlignedBB((double) blockpos.getX(), (double) blockpos.getY(), (double) blockpos.getZ(), (double) (blockpos.getX() + 1), (double) (blockpos.getY() + 1), (double) (blockpos.getZ() + 1))).grow((double) this.spawnRange)).size();

                    if (k >= this.maxNearbyEntities) {
                        this.resetTimer();
                        return;
                    }

                    EntityLiving entityliving = entity instanceof EntityLiving ? (EntityLiving) entity : null;
                    entity.setLocationAndAngles(entity.posX, entity.posY, entity.posZ, world.rand.nextFloat() * 360.0F, 0.0F);

                    if (entityliving == null || net.minecraftforge.event.ForgeEventFactory.canEntitySpawnSpawner(entityliving, getSpawnerWorld(), (float) entity.posX, (float) entity.posY, (float) entity.posZ)) {
                        if (this.spawnData.getNbt().getSize() == 1 && this.spawnData.getNbt().hasKey("id", 8) && entity instanceof EntityLiving) {
                            if (!net.minecraftforge.event.ForgeEventFactory.doSpecialSpawn(entityliving, this.getSpawnerWorld(), (float) entity.posX, (float) entity.posY, (float) entity.posZ))
                                ((EntityLiving) entity).onInitialSpawn(world.getDifficultyForLocation(new BlockPos(entity)), null);
                        }

                        AnvilChunkLoader.spawnEntity(entity, world);
                        world.playEvent(2004, blockpos, 0);

                        if (entityliving != null) {
                            entityliving.spawnExplosionParticle();
                        }

                        entity.getEntityData().setLong(NBT_TAG_SPAWNER_POS, this.getSpawnerPosition().toLong());

                        // stop spawning mobs if we reached the max
                        if (!Configuration.incrementOnMobDeath && incrementSpawnedMobsCount()) return;

                        flag = true;
                    }
                }

                if (flag) {
                    this.resetTimer();
                }
            }
        }
    }

    public boolean incrementSpawnedMobsCount() {
        if (++this.spawnedMobsCount >= Configuration.mobThreshold) {
            if (Configuration.breakSpawner)
                this.getSpawnerWorld().setBlockToAir(getSpawnerPosition());
            return true;
        }
        this.minSpawnDelay *= Configuration.spawnRateModifier;
        this.maxSpawnDelay *= Configuration.spawnRateModifier;
        return false;
    }

    private void resetTimer() {
        if (this.maxSpawnDelay <= this.minSpawnDelay) {
            this.spawnDelay = this.minSpawnDelay;
        } else {
            int i = this.maxSpawnDelay - this.minSpawnDelay;
            this.spawnDelay = this.minSpawnDelay + this.getSpawnerWorld().rand.nextInt(i);
        }

        if (!this.potentialSpawns.isEmpty()) {
            this.setNextSpawnData(WeightedRandom.getRandomItem(this.getSpawnerWorld().rand, this.potentialSpawns));
        }

        this.broadcastEvent(1);
    }


    public void broadcastEvent(int id) {
        tileEntityControlledSpawner.getWorld().addBlockEvent(tileEntityControlledSpawner.getPos(), Blocks.MOB_SPAWNER, id, 0);
    }

    public World getSpawnerWorld() {
        return tileEntityControlledSpawner.getWorld();
    }

    @Nonnull
    public BlockPos getSpawnerPosition() {
        return tileEntityControlledSpawner.getPos();
    }

    public void setNextSpawnData(@Nonnull WeightedSpawnerEntity weightedSpawnerEntity) {
        this.spawnData = weightedSpawnerEntity;

        if (this.getSpawnerWorld() != null) {
            IBlockState iblockstate = this.getSpawnerWorld().getBlockState(this.getSpawnerPosition());
            this.getSpawnerWorld().notifyBlockUpdate(tileEntityControlledSpawner.getPos(), iblockstate, iblockstate, 4);
        }
    }

    public void readFromNBT(NBTTagCompound nbt) {
        this.spawnDelay = nbt.getShort("Delay");
        this.potentialSpawns.clear();

        if (nbt.hasKey("SpawnPotentials", 9)) {
            NBTTagList nbttaglist = nbt.getTagList("SpawnPotentials", 10);

            for (int i = 0; i < nbttaglist.tagCount(); ++i) {
                this.potentialSpawns.add(new WeightedSpawnerEntity(nbttaglist.getCompoundTagAt(i)));
            }
        }

        if (nbt.hasKey("SpawnData", 10)) {
            this.setNextSpawnData(new WeightedSpawnerEntity(1, nbt.getCompoundTag("SpawnData")));
        } else if (!this.potentialSpawns.isEmpty()) {
            this.setNextSpawnData(WeightedRandom.getRandomItem(this.getSpawnerWorld().rand, this.potentialSpawns));
        }

        if (nbt.hasKey("MinSpawnDelay", 99)) {
            this.minSpawnDelay = nbt.getShort("MinSpawnDelay");
            this.maxSpawnDelay = nbt.getShort("MaxSpawnDelay");
            this.spawnCount = nbt.getShort("SpawnCount");
        }

        if (nbt.hasKey("MaxNearbyEntities", 99)) {
            this.maxNearbyEntities = nbt.getShort("MaxNearbyEntities");
            this.activatingRangeFromPlayer = nbt.getShort("RequiredPlayerRange");
        }

        if (nbt.hasKey("SpawnRange", 99)) {
            this.spawnRange = nbt.getShort("SpawnRange");
        }

        if (this.getSpawnerWorld() != null) {
            this.cachedEntity = null;
        }

        this.spawnedMobsCount = nbt.getInteger("spawnedMobs");
    }

    @Nonnull
    public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound nbt) {
        ResourceLocation resourcelocation = this.getEntityId();

        if (resourcelocation == null) {
            return nbt;
        } else {
            nbt.setShort("Delay", (short) this.spawnDelay);
            nbt.setShort("MinSpawnDelay", (short) this.minSpawnDelay);
            nbt.setShort("MaxSpawnDelay", (short) this.maxSpawnDelay);
            nbt.setShort("SpawnCount", (short) this.spawnCount);
            nbt.setShort("MaxNearbyEntities", (short) this.maxNearbyEntities);
            nbt.setShort("RequiredPlayerRange", (short) this.activatingRangeFromPlayer);
            nbt.setShort("SpawnRange", (short) this.spawnRange);
            nbt.setTag("SpawnData", this.spawnData.getNbt().copy());
            NBTTagList nbttaglist = new NBTTagList();

            if (this.potentialSpawns.isEmpty()) {
                nbttaglist.appendTag(this.spawnData.toCompoundTag());
            } else {
                for (WeightedSpawnerEntity weightedspawnerentity : this.potentialSpawns) {
                    nbttaglist.appendTag(weightedspawnerentity.toCompoundTag());
                }
            }

            nbt.setTag("SpawnPotentials", nbttaglist);
            nbt.setInteger("spawnedMobs", this.spawnedMobsCount);
            return nbt;
        }
    }

    @Nonnull
    @SideOnly(Side.CLIENT)
    public Entity getCachedEntity() {
        if (this.cachedEntity == null) {
            this.cachedEntity = AnvilChunkLoader.readWorldEntity(this.spawnData.getNbt(), this.getSpawnerWorld(), false);

            if (this.spawnData.getNbt().getSize() == 1 && this.spawnData.getNbt().hasKey("id", 8) && this.cachedEntity instanceof EntityLiving) {
                ((EntityLiving) this.cachedEntity).onInitialSpawn(this.getSpawnerWorld().getDifficultyForLocation(new BlockPos(this.cachedEntity)), null);
            }
        }

        return this.cachedEntity;
    }

    @SideOnly(Side.CLIENT)
    public double getMobRotation() {
        return this.mobRotation;
    }

    @SideOnly(Side.CLIENT)
    public double getPrevMobRotation() {
        return this.prevMobRotation;
    }
}