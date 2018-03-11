package ladysnake.spawnercontrol.client;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.util.Comparator;

public class MSCGuiConfig extends GuiConfig {
    public MSCGuiConfig(GuiScreen parentScreen, String modid, String title) {
        super(parentScreen, modid, title);
        // put the damn category first
        this.configElements.sort(Comparator.comparing(IConfigElement::isProperty));
    }
}
