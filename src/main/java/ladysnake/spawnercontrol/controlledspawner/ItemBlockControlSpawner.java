package ladysnake.spawnercontrol.controlledspawner;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

public class ItemBlockControlSpawner extends BlockItem {

    public ItemBlockControlSpawner(Block block, Properties props) {
        super(block, props);
    }

    @Nonnull
    @OnlyIn(Dist.CLIENT)
    public ITextComponent getName() {
        return this.getBlock().getNameTextComponent();
    }
}
