package ladysnake.spawnercontrol;

import ladysnake.spawnercontrol.config.MscConfig;
import ladysnake.spawnercontrol.controlledspawner.CapabilityControllableSpawner;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.tileentity.MobSpawnerTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.common.thread.SidedThreadGroups;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SpawnerEventHandlerTest {

    private MobSpawnerTileEntity vanillaSpawner;
    private MscConfig mainConfig = new MscConfig();
    private SpawnerEventHandler handler = new SpawnerEventHandler(mainConfig);

    static {
        TestSetup.init();
    }

    @Before
    public void setUp() {
        vanillaSpawner = TestSetup.makeSpawner(MobSpawnerTileEntity::new);
    }

    @After
    public void tearDown() {
        handler.allSpawners.clear();
    }

    @Test
    public void testSetUp() {
        assertNotNull("The spawner capability was not initialized properly", CapabilityControllableSpawner.CAPABILITY_SPAWNER);
    }

    @Test
    public void onAttachCapabilities() throws InterruptedException {
        TileEntity nonSpawnerTE = mock(ChestTileEntity.class);
        AttachCapabilitiesEvent<TileEntity> event = new AttachCapabilitiesEvent<>(TileEntity.class, nonSpawnerTE);
        // need to call this in a separate thread because onAttachCapabilities checks for side using the thread
        Thread serverThread = SidedThreadGroups.SERVER.newThread(() -> handler.onAttachCapabilities(event));
        serverThread.start();
        serverThread.join();
        assertFalse(event.getCapabilities().containsKey(CapabilityControllableSpawner.CAPABILITY_KEY));
    }

    @Test
    public void onAttachCapabilities2() throws InterruptedException {
        TileEntity spawnerTE = mock(MobSpawnerTileEntity.class);
        AttachCapabilitiesEvent<TileEntity> event = new AttachCapabilitiesEvent<>(TileEntity.class, spawnerTE);
        // need to call this in a separate thread because onAttachCapabilities checks for side using the thread
        Thread serverThread = SidedThreadGroups.SERVER.newThread(() -> handler.onAttachCapabilities(event));
        serverThread.start();
        serverThread.join();
        assertTrue(event.getCapabilities().containsKey(CapabilityControllableSpawner.CAPABILITY_KEY));
    }

    @Test
    public void onCheckNonSpawnerSpawn() {
        LivingSpawnEvent.CheckSpawn event = new LivingSpawnEvent.CheckSpawn(null, null, 0, 0, 0, null, SpawnReason.NATURAL);
        handler.onCheckSpawnerSpawn(event);
        assertEquals(Event.Result.DEFAULT, event.getResult());
    }

    @Test
    public void onCheckSpawnerSpawn() {
        World worldMock = mock(World.class);
        when(worldMock.getTileEntity(any())).thenReturn(vanillaSpawner);
        mainConfig.vanillaSpawnerConfig.spawnConditions.forceSpawnerAllSpawns = true;
        MobEntity entityMock = mock(CreeperEntity.class);
        LivingSpawnEvent.CheckSpawn event = new LivingSpawnEvent.CheckSpawn(entityMock, worldMock, 0, 0, 0, vanillaSpawner.getSpawnerBaseLogic(), SpawnReason.SPAWNER);
        event.setResult(Event.Result.DENY);
        handler.onCheckSpawnerSpawn(event);
        assertEquals("A predefined result has been changed", Event.Result.DENY, event.getResult());
        event = new LivingSpawnEvent.CheckSpawn(entityMock, worldMock, 0, 0, 0, vanillaSpawner.getSpawnerBaseLogic(), SpawnReason.SPAWNER);
        handler.onCheckSpawnerSpawn(event);
        assertEquals("An entity that should not be able to spawn has been allowed", Event.Result.DENY, event.getResult());
        when(entityMock.isNotColliding(worldMock)).thenReturn(true);
        event = new LivingSpawnEvent.CheckSpawn(entityMock, worldMock, 0, 0, 0, vanillaSpawner.getSpawnerBaseLogic(), SpawnReason.SPAWNER);
        handler.onCheckSpawnerSpawn(event);
        assertEquals("An entity that should be forced to spawn has been denied", Event.Result.ALLOW, event.getResult());
        when(entityMock.canSpawn(worldMock, SpawnReason.SPAWNER)).thenReturn(true);
        mainConfig.vanillaSpawnerConfig.spawnConditions.forceSpawnerAllSpawns = false;
        mainConfig.vanillaSpawnerConfig.spawnConditions.forceSpawnerMobSpawns = false;
        event = new LivingSpawnEvent.CheckSpawn(entityMock, worldMock, 0, 0, 0, vanillaSpawner.getSpawnerBaseLogic(), SpawnReason.SPAWNER);
        handler.onCheckSpawnerSpawn(event);
        assertEquals("An entity that should be able to spawn has been denied", Event.Result.ALLOW, event.getResult());
    }

    @Test
    public void onLivingSpawnSpecialSpawn() {
        // TODO
    }

    @Test
    public void onLivingDeath() {
        // TODO
    }

    @Test
    public void onBlockBreak() {
        // TODO
    }

    @Test
    public void onBlockHarvestDrops() {
        // TODO

    }
}