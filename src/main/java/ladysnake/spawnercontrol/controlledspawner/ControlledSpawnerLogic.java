package ladysnake.spawnercontrol.controlledspawner;

import ladysnake.spawnercontrol.SpawnerControl;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;

import javax.annotation.Nonnull;

/**
 * Copy of the vanilla logic to insert dedicated hooks without ASM <br/>
 * Additional information cannot be stored here as it will be erased on world load, use the capability instead
 * <p>
 *     As of the
 * </p>
 * @see IControllableSpawner
 * @see CapabilityControllableSpawner
 */
public class ControlledSpawnerLogic extends MobSpawnerBaseLogic {

    public static final String NBT_TAG_SPAWNER_POS = SpawnerControl.MOD_ID + ":spawnerPos";
    private final TileEntityMobSpawner tileEntityControlledSpawner;

    public ControlledSpawnerLogic(TileEntityMobSpawner tileEntityControlledSpawner) {
        super();
        this.tileEntityControlledSpawner = tileEntityControlledSpawner;
    }

    /**
     * Returns true if there's a player close enough to this mob spawner to activate it AND the spawner hasn't reached its max spawn count.
     */
    @Override
    protected boolean isActivated() {
        return CapabilityControllableSpawner.getHandler(tileEntityControlledSpawner).canSpawn() && super.isActivated();
    }

    /**
     * Almost the same as the vanilla one, everything is just copied here because ASM is annoying
     */
    @Override
    public void updateSpawner() {
        BlockPos blockpos = this.getSpawnerPosition();
        if (!this.isActivated()) {
            this.prevMobRotation = this.mobRotation;
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

                    // Patch: post a custom event with custom spawner information
                    // TODO replace by a call to ForgeEventFactory in 1.13
                    if (entityliving == null || CheckSpawnerSpawnEvent.canEntitySpawnSpawner(entityliving, getSpawnerWorld(), (float) entity.posX, (float) entity.posY, (float) entity.posZ, this)) {
                        if (this.spawnData.getNbt().getSize() == 1 && this.spawnData.getNbt().hasKey("id", 8) && entity instanceof EntityLiving) {
                            if (!SpawnerSpecialSpawnEvent.doSpecialSpawn(entityliving, this.getSpawnerWorld(), (float) entity.posX, (float) entity.posY, (float) entity.posZ, this))
                                ((EntityLiving) entity).onInitialSpawn(world.getDifficultyForLocation(new BlockPos(entity)), null);
                        }

                        AnvilChunkLoader.spawnEntity(entity, world);
                        world.playEvent(2004, blockpos, 0);

                        if (entityliving != null) {
                            entityliving.spawnExplosionParticle();
                        }

                        NBTTagCompound compound = new NBTTagCompound();
                        compound.setInteger("dimension", this.getSpawnerWorld().provider.getDimension());
                        compound.setLong("pos", this.getSpawnerPosition().toLong());
                        entity.getEntityData().setTag(NBT_TAG_SPAWNER_POS, compound);


                        flag = true;
                    }
                }

                if (flag) {
                    this.resetTimer();
                }
            }
        }
    }

    /**
     * Should be called after every spawn to tweak the cooldown according to the config
     */
    public void adjustDelayAfterSpawn(double spawnRateModifier) {
        this.minSpawnDelay *= spawnRateModifier;
        this.maxSpawnDelay *= spawnRateModifier;
    }

    public void broadcastEvent(int id) {
        tileEntityControlledSpawner.getWorld().addBlockEvent(tileEntityControlledSpawner.getPos(), Blocks.MOB_SPAWNER, id, 0);
    }

    // this can actually return null on world load
    @SuppressWarnings("NullableProblems")
    public World getSpawnerWorld() {
        return tileEntityControlledSpawner.getWorld();
    }

    @Nonnull
    public BlockPos getSpawnerPosition() {
        return tileEntityControlledSpawner.getPos();
    }
}
