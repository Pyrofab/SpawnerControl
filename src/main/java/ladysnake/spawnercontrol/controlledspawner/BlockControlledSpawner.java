package ladysnake.spawnercontrol.controlledspawner;

import ladysnake.spawnercontrol.Configuration;
import net.minecraft.block.BlockMobSpawner;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BlockControlledSpawner extends BlockMobSpawner {

    public BlockControlledSpawner() {
        this.setSoundType(SoundType.METAL);
        this.disableStats();
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityControlledSpawner();
    }

    @Override
    public int getExpDrop(IBlockState state, IBlockAccess world, BlockPos pos, int fortune) {
        return Configuration.xpDropped + RANDOM.nextInt(Configuration.randXpVariation) + RANDOM.nextInt(Configuration.randXpVariation);
    }

    @Nonnull
    @Override
    public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        List<ItemStack> drops = new ArrayList<ItemStack>();
        for (Map.Entry<String, Integer> entry : Configuration.itemsDropped.entrySet()) {
            Item item = Item.REGISTRY.getObject(new ResourceLocation(entry.getKey()));
            drops.add(new ItemStack(item, entry.getValue()));
        }
        return drops;
    }
}
