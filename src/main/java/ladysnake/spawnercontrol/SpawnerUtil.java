package ladysnake.spawnercontrol;

import ladysnake.spawnercontrol.config.MscConfig;
import ladysnake.spawnercontrol.config.SpawnerConfig;
import ladysnake.spawnercontrol.controlledspawner.CapabilityControllableSpawner;
import ladysnake.spawnercontrol.controlledspawner.IControllableSpawner;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.MobSpawnerTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import javax.annotation.Nullable;

public class SpawnerUtil {

    @Nullable
    public static SpawnerConfig getConfig(MinecraftServer server, CompoundNBT nbt) {
        if (nbt.contains("pos")) {
            BlockPos spawnerPos = NBTUtil.readBlockPos(nbt.getCompound("pos"));
            @SuppressWarnings("ConstantConditions") DimensionType dimension = DimensionType.byName(ResourceLocation.tryCreate(nbt.getString("dimension")));
            if (dimension != null) {
                World world = server.getWorld(dimension);
                return getConfig(world, spawnerPos);
            }
        }
        return null;
    }

    @Nullable
    public static SpawnerConfig getConfig(IWorld world, BlockPos spawnerPos) {
        TileEntity spawnerTE = world.getTileEntity(spawnerPos);
        // if it is not a spawner, it does not have a config
        if (!(spawnerTE instanceof MobSpawnerTileEntity)) return null;
        MscConfig mainConfig = SpawnerControl.instance().getConfigManager().getMainConfig();
        // if no custom spawner is registered, every spawner is a vanilla one or equivalent
        if (mainConfig.customSpawners.length == 0) {
            return mainConfig.alterVanillaSpawner ? mainConfig.vanillaSpawnerConfig : null;
        }
        SpawnerConfig ret = CapabilityControllableSpawner.getHandler((MobSpawnerTileEntity) spawnerTE).getConfig();
        // filter out non-mod spawners directly if they are to be ignored
        if (ret == mainConfig.vanillaSpawnerConfig && !mainConfig.alterVanillaSpawner)
            return null;
        return ret;
    }

    @Nullable
    public static IControllableSpawner getHandlerIfAffected(IWorld world, BlockPos spawnerPos) {
        TileEntity spawnerTE = world.getTileEntity(spawnerPos);
        if (!(spawnerTE instanceof MobSpawnerTileEntity)) return null;
        IControllableSpawner ret = CapabilityControllableSpawner.getHandler((MobSpawnerTileEntity) spawnerTE);
        // filter out non-mod spawners directly if they are to be ignored
        MscConfig mainConfig = SpawnerControl.instance().getConfigManager().getMainConfig();
        if (ret.getConfig() == mainConfig.vanillaSpawnerConfig && !mainConfig.alterVanillaSpawner) {
            return null;
        }
        return ret;
    }
}
