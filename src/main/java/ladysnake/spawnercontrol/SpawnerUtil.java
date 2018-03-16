package ladysnake.spawnercontrol;

import ladysnake.spawnercontrol.config.MSCConfig;
import ladysnake.spawnercontrol.config.SpawnerConfig;
import ladysnake.spawnercontrol.controlledspawner.CapabilityControllableSpawner;
import ladysnake.spawnercontrol.controlledspawner.IControllableSpawner;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class SpawnerUtil {
    @Nullable
    public static SpawnerConfig getConfig(World world, BlockPos spawnerPos) {
        // if no custom spawner is registered, every spawner is a vanilla one
        if (MSCConfig.customSpawners.length == 0)
            return MSCConfig.alterVanillaSpawner ? MSCConfig.vanillaSpawnerConfig : null;

        TileEntity spawnerTE = world.getTileEntity(spawnerPos);
        // if it is not a spawner, it does not have a config
        if (!(spawnerTE instanceof TileEntityMobSpawner)) return null;
        SpawnerConfig ret = CapabilityControllableSpawner.getHandler((TileEntityMobSpawner) spawnerTE).getConfig();
        // filter out non-mod spawners directly if they are to be ignored
        if (ret == MSCConfig.vanillaSpawnerConfig && !MSCConfig.alterVanillaSpawner)
            return null;
        return ret;
    }

    @Nullable
    public static IControllableSpawner getHandlerIfAffected(World world, BlockPos spawnerPos) {
        TileEntity spawnerTE = world.getTileEntity(spawnerPos);
        if (!(spawnerTE instanceof TileEntityMobSpawner)) return null;
        IControllableSpawner ret = CapabilityControllableSpawner.getHandler((TileEntityMobSpawner) spawnerTE);
        // filter out non-mod spawners directly if they are to be ignored
        if (ret.getConfig() == MSCConfig.vanillaSpawnerConfig && !MSCConfig.alterVanillaSpawner)
            return null;
        return ret;
    }
}
