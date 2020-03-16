package ladysnake.spawnercontrol.controlledspawner;

import ladysnake.spawnercontrol.SpawnerControl;
import ladysnake.spawnercontrol.config.SpawnerConfig;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.MobSpawnerTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class TileEntityControlledSpawner extends MobSpawnerTileEntity {
    public static final ResourceLocation TYPE_ID = SpawnerControl.id("controlled_spawner");
    public static final RegistryObject<TileEntityType<TileEntityControlledSpawner>> TYPE = RegistryObject.of(TYPE_ID, ForgeRegistries.TILE_ENTITIES);

    private final LazyOptional<IControllableSpawner> handler;

    public TileEntityControlledSpawner() {
        super();
        handler = LazyOptional.of(ControlledSpawnerHandler::new);
    }

    @Nonnull
    @Override
    public TileEntityType<?> getType() {
        return TYPE.get();
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
        if (capability == CapabilityControllableSpawner.CAPABILITY_SPAWNER) {
            return handler.cast();
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.handler.invalidate();
    }

    @Override
    public void read(CompoundNBT compound) {
        super.read(compound);
        this.handler.ifPresent(handler ->
                CapabilityControllableSpawner.CAPABILITY_SPAWNER.readNBT(handler, null, compound.getCompound("spawnerCap")));
    }

    @Nonnull
    @Override
    public CompoundNBT write(CompoundNBT compound) {
        super.write(compound);
        this.handler.ifPresent(handler -> compound.put(
                "spawnerCap",
                Objects.requireNonNull(CapabilityControllableSpawner.CAPABILITY_SPAWNER.writeNBT(handler, null))
        ));
        return compound;
    }

    public class ControlledSpawnerHandler extends CapabilityControllableSpawner.DefaultControllableSpawner {

        public ControlledSpawnerHandler() {
            super(TileEntityControlledSpawner.this);
        }

        @Nonnull
        public SpawnerConfig getConfig() {
            if (getBlockState().getBlock() instanceof BlockControlledSpawner) {
                return ((BlockControlledSpawner) getBlockState().getBlock()).getConfig();
            }
            return super.getConfig();
        }

    }
}
