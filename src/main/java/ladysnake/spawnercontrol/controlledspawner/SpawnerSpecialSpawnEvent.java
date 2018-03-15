package ladysnake.spawnercontrol.controlledspawner;

import net.minecraft.entity.EntityLiving;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;

public class SpawnerSpecialSpawnEvent extends LivingSpawnEvent.SpecialSpawn {
    protected final MobSpawnerBaseLogic spawner;

    private SpawnerSpecialSpawnEvent(EntityLiving entity, World world, float x, float y, float z, MobSpawnerBaseLogic spawner) {
        super(entity, world, x, y, z);
        this.spawner = spawner;
    }

    public static boolean doSpecialSpawn(EntityLiving entity, World world, float x, float y, float z, MobSpawnerBaseLogic spawner) {
        return MinecraftForge.EVENT_BUS.post(new SpawnerSpecialSpawnEvent(entity, world, x, y, z, spawner));
    }

    public MobSpawnerBaseLogic getSpawner() {
        return spawner;
    }
}
