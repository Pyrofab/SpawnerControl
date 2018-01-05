package ladysnake.spawnercontrol;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = SpawnerControl.MOD_ID,
        name = SpawnerControl.MOD_NAME,
        version = SpawnerControl.VERSION
)
public class SpawnerControl {

    public static final String MOD_ID = "spawnercontrol";
    public static final String MOD_NAME = "SpawnerControl";
    public static final String VERSION = "1.0";

    public static Logger LOGGER;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
    }

}
