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

class MSCGuiConfig extends GuiConfig {
    MSCGuiConfig(GuiScreen parentScreen, String modId, String title) {
        super(parentScreen, modId, title);
        // create config elements for generated custom spawners config
        List<IConfigElement> elements = CustomSpawnersConfig.getCustomSpawnerConfigs().stream()
                .map(SpawnerConfigHolder::getConfigCategory)
                .map(category -> new DummyConfigElement.DummyCategoryElement(category.getName(), category.getLanguagekey(), new ConfigElement(category).getChildElements()))
                .collect(Collectors.toList());
        // regroup all custom spawners config options into a single category
        this.configElements.add(new DummyConfigElement.DummyCategoryElement("custom spawners", "spawnercontrol.general.custom_spawners", elements));

        // put the damn categories first
        this.configElements.sort(Comparator.comparing(IConfigElement::isProperty));
    }
}
