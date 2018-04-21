package ladysnake.spawnercontrol;

import ladysnake.spawnercontrol.config.MSCConfig;
import ladysnake.spawnercontrol.config.SpawnerConfig;
import ladysnake.spawnercontrol.controlledspawner.BlockControlledSpawner;
import ladysnake.spawnercontrol.controlledspawner.TileEntityControlledSpawner;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SpawnerUtilTest {

    private TileEntityMobSpawner vanillaSpawner;
    private TileEntityControlledSpawner customSpawner;

    static {
        TestSetup.init();
    }

    @Before
    public void setUp() {
        vanillaSpawner = TestSetup.makeSpawner(TileEntityMobSpawner::new);
        customSpawner = TestSetup.makeSpawner(TileEntityControlledSpawner::new);
    }

    @After
    public void tearDown() {
        SpawnerEventHandler.allSpawners.clear();
    }

    @Test
    public void getConfig() {
        TileEntity nonSpawnerTE = new TileEntityChest();
        World worldMock = mock(World.class);
        when(worldMock.getTileEntity(any())).thenReturn(nonSpawnerTE);
        assertNull("Things that are not a spawner can't have a spawner config", SpawnerUtil.getConfig(worldMock, BlockPos.ORIGIN));
    }

    @Test
    public void getConfig1() {
        World worldMock = mock(World.class);
        when(worldMock.getTileEntity(any())).thenReturn(vanillaSpawner);
        MSCConfig.alterVanillaSpawner = false;
        assertNull("Vanilla mob spawners should not have a config", SpawnerUtil.getConfig(worldMock, BlockPos.ORIGIN));
        MSCConfig.alterVanillaSpawner = true;
        assertEquals("Config for vanilla spawner is not the vanilla config",
                MSCConfig.vanillaSpawnerConfig,
                SpawnerUtil.getConfig(worldMock, BlockPos.ORIGIN));
    }

    @Test
    public void getConfig2() {
        World worldMock = mock(World.class);
        when(worldMock.getTileEntity(any())).thenReturn(customSpawner);
        MSCConfig.customSpawners = new String[1];
        SpawnerConfig oracle = new SpawnerConfig();
        Block block = new BlockControlledSpawner(oracle);
        ReflectionHelper.setPrivateValue(TileEntity.class, customSpawner, block, "blockType");
        SpawnerConfig ret = SpawnerUtil.getConfig(worldMock, BlockPos.ORIGIN);
        assertEquals(ret == MSCConfig.vanillaSpawnerConfig
                ? "Returned vanilla config for custom spawner"
                : "Returned the wrong config", oracle, ret);
    }

    @Test
    public void getHandlerIfAffected() {
        World worldMock = mock(World.class);
        when(worldMock.getTileEntity(any())).thenReturn(null);
        assertNull("Things that are not a spawner can't have a spawner config",
                SpawnerUtil.getHandlerIfAffected(worldMock, BlockPos.ORIGIN));
    }

    @Test
    public void getHandlerIfAffected1() {
        World worldMock = mock(World.class);
        when(worldMock.getTileEntity(any())).thenReturn(vanillaSpawner);
        MSCConfig.alterVanillaSpawner = false;
        assertNull("Vanilla mob spawners should not be affected",
                SpawnerUtil.getHandlerIfAffected(worldMock, BlockPos.ORIGIN));
        MSCConfig.alterVanillaSpawner = true;
        assertNotNull(SpawnerUtil.getHandlerIfAffected(worldMock, BlockPos.ORIGIN));
    }
}