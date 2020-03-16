package ladysnake.spawnercontrol.controlledspawner;

import ladysnake.spawnercontrol.config.SpawnerConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.SpawnerBlock;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.tileentity.MobSpawnerTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.spawner.AbstractSpawner;

import javax.annotation.Nonnull;

public class BlockControlledSpawner extends SpawnerBlock {
    private static final String DEFAULT_LANG_KEY = "tile.msc.controlled_spawner.name";

    private final SpawnerConfig config;
    private final String name;

    public BlockControlledSpawner(Properties props, SpawnerConfig config, String name) {
        super(props);
        this.config = config;
        this.name = name;
    }


    @SuppressWarnings("deprecation")
    @Nonnull
    @Override
    public ActionResultType onUse(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult) {
        ItemStack stack = player.getHeldItem(hand);
        if (stack.getItem() instanceof SpawnEggItem) {
            TileEntity tileentity = worldIn.getTileEntity(pos);

            if (tileentity instanceof MobSpawnerTileEntity) {
                AbstractSpawner logic = ((MobSpawnerTileEntity) tileentity).getSpawnerBaseLogic();
                logic.setEntityType(((SpawnEggItem) stack.getItem()).getType(stack.getTag()));
                tileentity.markDirty();
                worldIn.setBlockState(pos, state, 3);

                if (!player.abilities.isCreativeMode) {
                    stack.shrink(1);
                }

                return ActionResultType.SUCCESS;
            }
        }
        return super.onUse(state, worldIn, pos, player, hand, rayTraceResult);
    }

    @Override
    public TileEntity createNewTileEntity(IBlockReader reader) {
        return new TileEntityControlledSpawner();
    }

    @Nonnull
    @Override
    public ITextComponent getNameTextComponent() {
        // in case someone makes a custom lang file to translate their spawner names
        if (I18n.hasKey(getTranslationKey())) {
            return super.getNameTextComponent();
        }
        // return a default readable value
        return new TranslationTextComponent(DEFAULT_LANG_KEY, this.name);
    }

    public SpawnerConfig getConfig() {
        return config;
    }
}
