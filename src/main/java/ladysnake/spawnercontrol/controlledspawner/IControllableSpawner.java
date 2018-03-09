package ladysnake.spawnercontrol.controlledspawner;

public interface IControllableSpawner {

    void setSpawnedMobsCount(int mobCount);

    /**
     * Convenience method to increment the spawned count and check if it is more than the config allows. <br/>
     * Also runs corresponding logic like breaking the spawner or adjusting the spawn delay
     * @return true if the updated count is above the configured ceiling
     */
    boolean incrementSpawnedMobsCount();

    int getSpawnedMobsCount();

    boolean canSpawn();
}
