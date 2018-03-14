package ladysnake.spawnercontrol.controlledspawner;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;

import javax.annotation.Nonnull;

@SuppressWarnings("deprecation") // need to use the shitty I18n since this method is not client-side only
public class ItemBlockControlSpawner extends ItemBlock {
    private static final String DEFAULT_LANG_KEY = "tile.msc.controlled_spawner.name";
    private String name;

    public ItemBlockControlSpawner(Block block, String name) {
        super(block);
        this.name = name;
    }

    @Nonnull
    @Override
    public String getItemStackDisplayName(@Nonnull ItemStack stack) {
        // in case someone makes a custom lang file to translate their spawner names
        if (I18n.canTranslate(getUnlocalizedName(stack) + ".name"))
            return super.getItemStackDisplayName(stack);
        // return a default readable value
        return I18n.translateToLocalFormatted(DEFAULT_LANG_KEY, name).trim();
    }
}
