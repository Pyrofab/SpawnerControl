package ladysnake.spawnercontrol.controlledspawner;

import ladysnake.spawnercontrol.config.MSCConfig;
import ladysnake.spawnercontrol.config.SpawnerConfig;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TileEntityControlledSpawner extends TileEntityMobSpawner {
    private IControllableSpawner handler;

    public TileEntityControlledSpawner() {
        super();
        handler = new ControlledSpawnerHandler();
        this.spawnerLogic = new ControlledSpawnerLogic(this);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityControllableSpawner.CAPABILITY_SPAWNER)
            return CapabilityControllableSpawner.CAPABILITY_SPAWNER.cast(handler);
        return super.getCapability(capability, facing);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        CapabilityControllableSpawner.CAPABILITY_SPAWNER.readNBT(handler, null, compound.getCompoundTag("spawnerCap"));
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("spawnerCap", CapabilityControllableSpawner.CAPABILITY_SPAWNER.writeNBT(handler, null));
        return compound;
    }

    public class ControlledSpawnerHandler extends CapabilityControllableSpawner.DefaultControllableSpawner {

        public ControlledSpawnerHandler() {
            super(TileEntityControlledSpawner.this);
        }

        @Nonnull
        public SpawnerConfig getConfig() {
            return getBlockType() instanceof BlockControlledSpawner
                    ? ((BlockControlledSpawner) getBlockType()).getConfig()
                    : MSCConfig.vanillaSpawnerConfig;
        }

    }
}
