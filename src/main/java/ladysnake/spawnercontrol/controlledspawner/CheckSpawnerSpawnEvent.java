package ladysnake.spawnercontrol.controlledspawner;

import net.minecraft.entity.EntityLiving;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;

/**
 * A version of {@link LivingSpawnEvent.CheckSpawn} with information about the spawner the spawn originates from. <br/>
 * Fired by {@link ControlledSpawnerLogic} each time an attempt to spawn a mob is made
 */
public class CheckSpawnerSpawnEvent extends LivingSpawnEvent.CheckSpawn {
    protected final TileEntityMobSpawner spawner;

    public CheckSpawnerSpawnEvent(EntityLiving entity, World world, float x, float y, float z, TileEntityMobSpawner spawner) {
        super(entity, world, x, y, z, true);
//        super(entity, world, x, y, z);    // 1.11.2 constructor
        this.spawner = spawner;
    }

    public TileEntityMobSpawner getSpawner() {
        return spawner;
    }

    public static boolean canEntitySpawnSpawner(EntityLiving entity, World world, float x, float y, float z, TileEntityMobSpawner spawner) {
        Result result;
        if (entity == null)
            return false;
        else {
            LivingSpawnEvent.CheckSpawn event = new CheckSpawnerSpawnEvent(entity, world, x, y, z, spawner);
            MinecraftForge.EVENT_BUS.post(event);
            result = event.getResult();
        }
        if (result == Result.DEFAULT) {
            return entity.getCanSpawnHere() && entity.isNotColliding(); // vanilla logic
        } else {
            return result == Result.ALLOW;
        }
    }
}
