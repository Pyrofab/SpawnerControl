package ladysnake.spawnercontrol;

import ladysnake.spawnercontrol.config.MSCConfig;
import ladysnake.spawnercontrol.controlledspawner.CapabilityControllableSpawner;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.world.World;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.thread.SidedThreadGroups;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SpawnerEventHandlerTest {

    private TileEntityMobSpawner vanillaSpawner;

    static {
        TestSetup.init();
    }

    @Before
    public void setUp() {
        vanillaSpawner = TestSetup.makeSpawner(TileEntityMobSpawner::new);
    }

    @After
    public void tearDown() {
        SpawnerEventHandler.allSpawners.clear();
    }

    @Test
    public void testSetUp() {
        assertNotNull("The spawner capability was not initialized properly", CapabilityControllableSpawner.CAPABILITY_SPAWNER);
    }

    @Test
    public void onAttachCapabilities() throws InterruptedException {
        TileEntity nonSpawnerTE = mock(TileEntityChest.class);
        AttachCapabilitiesEvent<TileEntity> event = new AttachCapabilitiesEvent<>(TileEntity.class, nonSpawnerTE);
        // need to call this in a separate thread because onAttachCapabilities checks for side using the thread
        Thread serverThread = SidedThreadGroups.SERVER.newThread(() -> SpawnerEventHandler.onAttachCapabilities(event));
        serverThread.start();
        serverThread.join();
        assertFalse(event.getCapabilities().containsKey(CapabilityControllableSpawner.CAPABILITY_KEY));
    }

    @Test
    public void onAttachCapabilities2() throws InterruptedException {
        TileEntity spawnerTE = mock(TileEntityMobSpawner.class);
        AttachCapabilitiesEvent<TileEntity> event = new AttachCapabilitiesEvent<>(TileEntity.class, spawnerTE);
        // need to call this in a separate thread because onAttachCapabilities checks for side using the thread
        Thread serverThread = SidedThreadGroups.SERVER.newThread(() -> SpawnerEventHandler.onAttachCapabilities(event));
        serverThread.start();
        serverThread.join();
        assertTrue(event.getCapabilities().containsKey(CapabilityControllableSpawner.CAPABILITY_KEY));
    }

    @Test
    public void onCheckNonSpawnerSpawn() {
        LivingSpawnEvent.CheckSpawn event = new LivingSpawnEvent.CheckSpawn(null, null, 0, 0, 0, null);
        SpawnerEventHandler.onCheckSpawnerSpawn(event);
        assertEquals(Event.Result.DEFAULT, event.getResult());
    }

    @Test
    public void onCheckSpawnerSpawn() {
        World worldMock = mock(World.class);
        when(worldMock.getTileEntity(any())).thenReturn(vanillaSpawner);
        MSCConfig.vanillaSpawnerConfig.spawnConditions.forceSpawnerAllSpawns = true;
        EntityLiving entityMock = mock(EntityCreeper.class);
        LivingSpawnEvent.CheckSpawn event = new LivingSpawnEvent.CheckSpawn(entityMock, worldMock, 0, 0, 0, vanillaSpawner.getSpawnerBaseLogic());
        event.setResult(Event.Result.DENY);
        SpawnerEventHandler.onCheckSpawnerSpawn(event);
        assertEquals("A predefined result has been changed", Event.Result.DENY, event.getResult());
        event = new LivingSpawnEvent.CheckSpawn(entityMock, worldMock, 0, 0, 0, vanillaSpawner.getSpawnerBaseLogic());
        SpawnerEventHandler.onCheckSpawnerSpawn(event);
        assertEquals("An entity that should not be able to spawn has been allowed", Event.Result.DENY, event.getResult());
        when(entityMock.isNotColliding()).thenReturn(true);
        event = new LivingSpawnEvent.CheckSpawn(entityMock, worldMock, 0, 0, 0, vanillaSpawner.getSpawnerBaseLogic());
        SpawnerEventHandler.onCheckSpawnerSpawn(event);
        assertEquals("An entity that should be forced to spawn has been denied", Event.Result.ALLOW, event.getResult());
        when(entityMock.getCanSpawnHere()).thenReturn(true);
        MSCConfig.vanillaSpawnerConfig.spawnConditions.forceSpawnerAllSpawns = false;
        MSCConfig.vanillaSpawnerConfig.spawnConditions.forceSpawnerMobSpawns = false;
        event = new LivingSpawnEvent.CheckSpawn(entityMock, worldMock, 0, 0, 0, vanillaSpawner.getSpawnerBaseLogic());
        SpawnerEventHandler.onCheckSpawnerSpawn(event);
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