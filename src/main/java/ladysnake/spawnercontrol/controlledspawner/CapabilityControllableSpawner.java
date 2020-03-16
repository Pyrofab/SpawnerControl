package ladysnake.spawnercontrol.controlledspawner;

import ladysnake.spawnercontrol.SpawnerControl;
import ladysnake.spawnercontrol.config.SpawnerConfig;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.tileentity.MobSpawnerTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.spawner.AbstractSpawner;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.NoSuchElementException;
import java.util.Objects;

public class CapabilityControllableSpawner {
    @CapabilityInject(IControllableSpawner.class)
    public static Capability<IControllableSpawner> CAPABILITY_SPAWNER;

    public static final ResourceLocation CAPABILITY_KEY = new ResourceLocation(SpawnerControl.MOD_ID, "controllable_spawner_cap");

    public static IControllableSpawner getHandler(MobSpawnerTileEntity entity) {
        return entity.getCapability(CAPABILITY_SPAWNER, null).orElseThrow(NoSuchElementException::new);
    }

    public static class DefaultControllableSpawner implements IControllableSpawner {
        private final MobSpawnerTileEntity spawner;
        private int spawnedMobsCount;

        public DefaultControllableSpawner() {
            this(null);
        }

        DefaultControllableSpawner(MobSpawnerTileEntity spawner) {
            this.spawner = spawner;
        }

        @Override
        public void setSpawnedMobsCount(int mobCount) {
            this.spawnedMobsCount = mobCount;
        }

        @Override
        public boolean incrementSpawnedMobsCount() {
            SpawnerConfig cfg = getConfig();
            if(++this.spawnedMobsCount >= cfg.mobThreshold) {
                if (cfg.breakSpawner) {
                    Objects.requireNonNull(spawner.getWorld()).setBlockState(spawner.getPos(), Blocks.AIR.getDefaultState());
                }
                return true;
            }
            this.adjustDelayAfterSpawn(spawner.getSpawnerBaseLogic(), cfg.spawnRateModifier);
            return false;
        }

        /**
         * Should be called after every spawn to tweak the cooldown according to the config
         */
        protected void adjustDelayAfterSpawn(AbstractSpawner spawnerBaseLogic, double spawnRateModifier) {
            spawnerBaseLogic.minSpawnDelay *= spawnRateModifier;
            spawnerBaseLogic.maxSpawnDelay *= spawnRateModifier;
        }

        @Override
        public int getSpawnedMobsCount() {
            return spawnedMobsCount;
        }

        @Override
        public boolean canSpawn() {
            return this.spawnedMobsCount < getConfig().mobThreshold;
        }

        @Nonnull
        @Override
        public SpawnerConfig getConfig() {
            return SpawnerControl.instance().getConfigManager().getMainConfig().vanillaSpawnerConfig;
        }
    }

    public static class Provider implements ICapabilitySerializable<CompoundNBT> {
        final LazyOptional<IControllableSpawner> instance;

        public Provider(MobSpawnerTileEntity spawner) {
            this.instance = LazyOptional.of(() -> new DefaultControllableSpawner(spawner));
        }

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
            return CAPABILITY_SPAWNER.orEmpty(capability, this.instance);
        }

        @Override
        public CompoundNBT serializeNBT() {
            return (CompoundNBT) CAPABILITY_SPAWNER.getStorage().writeNBT(CAPABILITY_SPAWNER, instance.orElseThrow(NoSuchElementException::new), null);
        }

        @Override
        public void deserializeNBT(CompoundNBT nbt) {
            CAPABILITY_SPAWNER.getStorage().readNBT(CAPABILITY_SPAWNER, instance.orElseThrow(NoSuchElementException::new), null, nbt);
        }
    }

    public static class Storage implements Capability.IStorage<IControllableSpawner> {

        @Nullable
        @Override
        public CompoundNBT writeNBT(Capability<IControllableSpawner> capability, IControllableSpawner instance, Direction side) {
            CompoundNBT nbt = new CompoundNBT();
            nbt.putInt("SpawnedMobsCount", instance.getSpawnedMobsCount());
            return nbt;
        }

        @Override
        public void readNBT(Capability<IControllableSpawner> capability, IControllableSpawner instance, Direction side, INBT nbt) {
            if (nbt instanceof CompoundNBT) {
                instance.setSpawnedMobsCount(((CompoundNBT) nbt).getInt("SpawnedMobsCount"));
            }
        }
    }
}
