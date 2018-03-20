package ladysnake.spawnercontrol.controlledspawner;

import ladysnake.spawnercontrol.config.SpawnerConfig;
import net.minecraft.block.BlockMobSpawner;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockControlledSpawner extends BlockMobSpawner {
    private SpawnerConfig config;

    public BlockControlledSpawner(SpawnerConfig config) {
        super();
        this.config = config;
        this.setHardness(5.0F);
        this.setSoundType(SoundType.METAL);
        this.disableStats();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack itemstack = player.getHeldItem(hand);
        if (itemstack.getItem() instanceof ItemMonsterPlacer) {
            TileEntity tileentity = worldIn.getTileEntity(pos);

            if (tileentity instanceof TileEntityMobSpawner)
            {
                MobSpawnerBaseLogic mobspawnerbaselogic = ((TileEntityMobSpawner)tileentity).getSpawnerBaseLogic();
                mobspawnerbaselogic.setEntityId(ItemMonsterPlacer.getNamedIdFrom(itemstack));
                tileentity.markDirty();
                worldIn.notifyBlockUpdate(pos, state, state, 3);

                if (!player.capabilities.isCreativeMode)
                {
                    itemstack.shrink(1);
                }

                return true;
            }
        }
        return super.onBlockActivated(worldIn, pos, state, player, hand, facing, hitX, hitY, hitZ);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityControlledSpawner();
    }

    public SpawnerConfig getConfig() {
        return config;
    }
}
