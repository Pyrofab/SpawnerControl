package ladysnake.spawnercontrol.controlledspawner;

import net.minecraft.tileentity.TileEntityMobSpawner;

public class TileEntityControlledSpawner extends TileEntityMobSpawner {
    public TileEntityControlledSpawner() {
        super();
        this.spawnerLogic = new ControlledSpawnerLogic(this);
    }
}
