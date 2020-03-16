package ladysnake.spawnercontrol;

import cpw.mods.modlauncher.Launcher;
import ladysnake.spawnercontrol.controlledspawner.CapabilityControllableSpawner;
import ladysnake.spawnercontrol.controlledspawner.IControllableSpawner;
import net.minecraft.tileentity.MobSpawnerTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Bootstrap;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class TestSetup {

    private static boolean alreadySetup;

    public static synchronized void init() {
        if (!alreadySetup) {
            Launcher.main();
//            Launcher.INSTANCE.blackboard().put("fml.deobfuscatedEnvironment", s -> true);
            Bootstrap.register();
/*
            IModInfo testModMeta = new ModInfo();
            testModMeta.modId = SpawnerControl.MOD_ID;
            Launcher.INSTANCE.setupTestHarness(new ModInfo(testModMeta));
*/
            CapabilityManager.INSTANCE.register(IControllableSpawner.class, new CapabilityControllableSpawner.Storage(), CapabilityControllableSpawner.DefaultControllableSpawner::new);
            Map<String, Capability<?>> providers = ObfuscationReflectionHelper.getPrivateValue(CapabilityManager.class, CapabilityManager.INSTANCE, "providers");
            assert providers != null;
            // artificially inject the capability as forge has not scanned the mod for annotations
            //noinspection unchecked
            CapabilityControllableSpawner.CAPABILITY_SPAWNER = (Capability<IControllableSpawner>) providers.get(IControllableSpawner.class.getName().intern());
            alreadySetup = true;
        }
    }

    public static <T extends MobSpawnerTileEntity> T makeSpawner(Supplier<T> factory) {
        T spawner = factory.get();
        Map<ResourceLocation, ICapabilityProvider> list = new HashMap<>();
        list.put(CapabilityControllableSpawner.CAPABILITY_KEY, new CapabilityControllableSpawner.Provider(spawner));
        CapabilityDispatcher dispatcher = new CapabilityDispatcher(list, Collections.emptyList());
        ObfuscationReflectionHelper.setPrivateValue(TileEntity.class, spawner, dispatcher, "capabilities");
        return spawner;
    }
}
