package ladysnake.spawnercontrol.client;

import ladysnake.spawnercontrol.SpawnerControl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;

import java.util.Set;

@SuppressWarnings("unused")
public class MSCConfigFactory implements IModGuiFactory {
    @Override
    public void initialize(Minecraft minecraftInstance) {

    }

    @Override
    public boolean hasConfigGui() {
        return true;
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parentScreen) {
        return new MSCGuiConfig(parentScreen, SpawnerControl.MOD_ID, "Mob Spawner Control Config");
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }

    // 1.11.2 compat
//    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) {
//        return null;
//    }
//
//    public Class<? extends GuiScreen> mainConfigGuiClass() {
//        return MSCGuiConfig.class;
//    }
}
