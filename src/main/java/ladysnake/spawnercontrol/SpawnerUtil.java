package ladysnake.spawnercontrol;

import ladysnake.spawnercontrol.config.MSCConfig;
import ladysnake.spawnercontrol.config.SpawnerConfig;
import ladysnake.spawnercontrol.controlledspawner.CapabilityControllableSpawner;
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
        if (!(spawnerTE instanceof TileEntityMobSpawner)) return null;
        SpawnerConfig ret = CapabilityControllableSpawner.getHandler((TileEntityMobSpawner) spawnerTE).getConfig();
        // filter out non-mod spawners directly if they are to be ignored
        if (ret == MSCConfig.vanillaSpawnerConfig && !MSCConfig.alterVanillaSpawner)
            return null;
        return ret;
    }
}
