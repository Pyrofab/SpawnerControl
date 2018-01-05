package ladysnake.spawnercontrol.controlledspawner;

import net.minecraft.block.BlockMobSpawner;
import net.minecraft.block.SoundType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockControlledSpawner extends BlockMobSpawner {

    public BlockControlledSpawner() {
        super();
        this.setHardness(5.0F);
        this.setSoundType(SoundType.METAL);
        this.disableStats();
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityControlledSpawner();
    }
}
