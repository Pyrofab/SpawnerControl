package ladysnake.spawnercontrol.controlledspawner;

import net.minecraft.entity.EntityLiving;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;

/**
 * A version of {@link LivingSpawnEvent.CheckSpawn} with information about the spawner the spawn originates from. <br/>
 * Fired by {@link ControlledSpawnerLogic} each time an attempt to spawn a mob is made
 *
 * Will be removed in 1.13 as the CheckSpawn event does what we want
 */
public class CheckSpawnerSpawnEvent extends LivingSpawnEvent.CheckSpawn {
    protected final MobSpawnerBaseLogic spawner;

    public CheckSpawnerSpawnEvent(EntityLiving entity, World world, float x, float y, float z, MobSpawnerBaseLogic spawner) {
        super(entity, world, x, y, z);    // use 1.11.2 constructor for backward compatibility
        this.spawner = spawner;
    }

    // no override annotation to avoid the compiler yelling in older versions
    public MobSpawnerBaseLogic getSpawner() {
        return spawner;
    }

    public boolean isSpawner() {
        return true;
    }

    /**
     * A replacement for forge's hook firing our custom event.
     * @see net.minecraftforge.event.ForgeEventFactory#canEntitySpawnSpawner(EntityLiving, World, float, float, float, MobSpawnerBaseLogic)
     */
    public static boolean canEntitySpawnSpawner(EntityLiving entity, World world, float x, float y, float z, MobSpawnerBaseLogic spawner) {
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
