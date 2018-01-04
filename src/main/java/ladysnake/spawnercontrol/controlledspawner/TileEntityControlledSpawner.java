package ladysnake.spawnercontrol.controlledspawner;

import ladysnake.spawnercontrol.Configuration;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.tileentity.TileEntityMobSpawner;

import javax.annotation.Nonnull;

public class TileEntityControlledSpawner extends TileEntityMobSpawner {

    protected int spawnedMobsCount;
    protected MobSpawnerBaseLogic spawnerLogic = new ControlledSpawnerLogic(this);

    /**
     * Increments the number of mobs spawned by this spawner
     * @return true if the spawner has reached the threshold set in the config
     */
    public boolean incrementSpawnedMobs() {
        if (++this.spawnedMobsCount >= Configuration.mobThreshold) {
            if(Configuration.breakSpawner)
                world.setBlockToAir(pos);
            return true;
        }
        return false;
    }

    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.spawnerLogic.readFromNBT(compound);
        this.spawnedMobsCount = compound.getInteger("spawnedMobs");
    }

    @Nonnull
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        this.spawnerLogic.writeToNBT(compound);
        compound.setInteger("spawnedMobs", this.spawnedMobsCount);
        return compound;
    }

    /**
     * Like the old updateEntity(), except more generic.
     */
    public void update() {
        if (this.spawnedMobsCount < Configuration.mobThreshold)
            this.spawnerLogic.updateSpawner();
        else if (Configuration.breakSpawner)
            world.setBlockToAir(pos);
    }

    public boolean receiveClientEvent(int id, int type) {
        return this.spawnerLogic.setDelayToMin(id) || super.receiveClientEvent(id, type);
    }

    @Nonnull
    public MobSpawnerBaseLogic getSpawnerBaseLogic() {
        return this.spawnerLogic;
    }

}
