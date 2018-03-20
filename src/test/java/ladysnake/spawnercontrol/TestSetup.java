package ladysnake.spawnercontrol;

import ladysnake.spawnercontrol.controlledspawner.CapabilityControllableSpawner;
import ladysnake.spawnercontrol.controlledspawner.IControllableSpawner;
import net.minecraft.init.Bootstrap;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class TestSetup {

    private static boolean alreadySetup;

    public static synchronized void init() {
        if (!alreadySetup) {
            Launch.blackboard = new HashMap<>();
            Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
            Bootstrap.register();
            ModMetadata testModMeta = new ModMetadata();
            testModMeta.modId = SpawnerControl.MOD_ID;
            Loader.instance().setupTestHarness(new DummyModContainer(testModMeta));
            CapabilityManager.INSTANCE.register(IControllableSpawner.class, new CapabilityControllableSpawner.Storage(), CapabilityControllableSpawner.DefaultControllableSpawner::new);
            Map<String, Capability<?>> providers = ReflectionHelper.getPrivateValue(CapabilityManager.class, CapabilityManager.INSTANCE, "providers");
            // artificially inject the capability as forge has not scanned the mod for annotations
            //noinspection unchecked
            CapabilityControllableSpawner.CAPABILITY_SPAWNER = (Capability<IControllableSpawner>) providers.get(IControllableSpawner.class.getName().intern());
            alreadySetup = true;
        }
    }

    public static <T extends TileEntityMobSpawner> T makeSpawner(Supplier<T> factory) {
        T spawner = factory.get();
        Map<ResourceLocation, ICapabilityProvider> list = new HashMap<>();
        list.put(CapabilityControllableSpawner.CAPABILITY_KEY, new CapabilityControllableSpawner.Provider(spawner));
        CapabilityDispatcher dispatcher = new CapabilityDispatcher(list);
        ReflectionHelper.setPrivateValue(TileEntity.class, spawner, dispatcher, "capabilities");
        return spawner;
    }
}
