package ladysnake.spawnercontrol.client;

import ladysnake.spawnercontrol.config.CustomSpawnersConfig;
import ladysnake.spawnercontrol.config.SpawnerConfigHolder;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.DummyConfigElement;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MSCGuiConfig extends GuiConfig {
    public MSCGuiConfig(GuiScreen parentScreen, String modid, String title) {
        super(parentScreen, modid, title);
        // add config options for custom spawner
        List<IConfigElement> elements = CustomSpawnersConfig.getCustomSpawnerConfigs().stream()
                .map(SpawnerConfigHolder::getConfigCategory)
                .map(category -> {
                    DummyConfigElement.DummyCategoryElement element = new DummyConfigElement.DummyCategoryElement(category.getName(), category.getLanguagekey(), new ConfigElement(category).getChildElements());
                    element.setRequiresMcRestart(category.requiresMcRestart());
                    element.setRequiresWorldRestart(category.requiresWorldRestart());
                    return element;
                }).collect(Collectors.toList());
        this.configElements.add(new DummyConfigElement.DummyCategoryElement("custom spawners", "msc.config.custom_spawners", elements));

        // put the damn categories first
        this.configElements.sort(Comparator.comparing(IConfigElement::isProperty));
    }
}
