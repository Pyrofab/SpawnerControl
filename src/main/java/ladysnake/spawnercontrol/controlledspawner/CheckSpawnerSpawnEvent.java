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

    private CheckSpawnerSpawnEvent(EntityLiving entity, World world, float x, float y, float z, MobSpawnerBaseLogic spawner) {
        super(entity, world, x, y, z, spawner);
//        super(entity, world, x, y, z);    // 1.11.2 constructor
        this.spawner = spawner;
    }

    public MobSpawnerBaseLogic getSpawner() {
        return spawner;
    }

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
