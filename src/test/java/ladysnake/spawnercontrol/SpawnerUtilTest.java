package ladysnake.spawnercontrol;

import ladysnake.spawnercontrol.config.MscConfig;
import ladysnake.spawnercontrol.config.SpawnerConfig;
import ladysnake.spawnercontrol.controlledspawner.BlockControlledSpawner;
import ladysnake.spawnercontrol.controlledspawner.TileEntityControlledSpawner;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.tileentity.MobSpawnerTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SpawnerUtilTest {

    private MscConfig mainConfig = new MscConfig();
    private SpawnerEventHandler eventHandler = new SpawnerEventHandler(mainConfig);
    private MobSpawnerTileEntity vanillaSpawner;
    private TileEntityControlledSpawner customSpawner;

    static {
        TestSetup.init();
    }

    @Before
    public void setUp() {
        vanillaSpawner = TestSetup.makeSpawner(MobSpawnerTileEntity::new);
        customSpawner = TestSetup.makeSpawner(TileEntityControlledSpawner::new);
    }

    @After
    public void tearDown() {
        eventHandler.allSpawners.clear();
    }

    @Test
    public void getConfig() {
        TileEntity nonSpawnerTE = new ChestTileEntity();
        World worldMock = mock(World.class);
        when(worldMock.getTileEntity(any())).thenReturn(nonSpawnerTE);
        assertNull("Things that are not a spawner can't have a spawner config", SpawnerUtil.getConfig(worldMock, BlockPos.ZERO));
    }

    @Test
    public void getConfig1() {
        World worldMock = mock(World.class);
        when(worldMock.getTileEntity(any())).thenReturn(vanillaSpawner);
        mainConfig.alterVanillaSpawner = false;
        assertNull("Vanilla mob spawners should not have a config", SpawnerUtil.getConfig(worldMock, BlockPos.ZERO));
        mainConfig.alterVanillaSpawner = true;
        assertEquals("Config for vanilla spawner is not the vanilla config",
                mainConfig.vanillaSpawnerConfig,
                SpawnerUtil.getConfig(worldMock, BlockPos.ZERO));
    }

    @Test
    public void getConfig2() {
        World worldMock = mock(World.class);
        when(worldMock.getTileEntity(any())).thenReturn(customSpawner);
        mainConfig.customSpawners = new String[1];
        SpawnerConfig oracle = new SpawnerConfig();
        Block block = new BlockControlledSpawner(Block.Properties.from(Blocks.SPAWNER), oracle, "oracle");
        ObfuscationReflectionHelper.setPrivateValue(TileEntity.class, customSpawner, block, "blockType");
        SpawnerConfig ret = SpawnerUtil.getConfig(worldMock, BlockPos.ZERO);
        assertEquals(ret == mainConfig.vanillaSpawnerConfig
                ? "Returned vanilla config for custom spawner"
                : "Returned the wrong config", oracle, ret);
    }

    @Test
    public void getHandlerIfAffected() {
        World worldMock = mock(World.class);
        when(worldMock.getTileEntity(any())).thenReturn(null);
        assertNull("Things that are not a spawner can't have a spawner config",
                SpawnerUtil.getHandlerIfAffected(worldMock, BlockPos.ZERO));
    }

    @Test
    public void getHandlerIfAffected1() {
        World worldMock = mock(World.class);
        when(worldMock.getTileEntity(any())).thenReturn(vanillaSpawner);
        mainConfig.alterVanillaSpawner = false;
        assertNull("Vanilla mob spawners should not be affected",
                SpawnerUtil.getHandlerIfAffected(worldMock, BlockPos.ZERO));
        mainConfig.alterVanillaSpawner = true;
        assertNotNull(SpawnerUtil.getHandlerIfAffected(worldMock, BlockPos.ZERO));
    }
}