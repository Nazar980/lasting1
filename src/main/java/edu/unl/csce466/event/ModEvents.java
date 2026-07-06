package edu.unl.csce466.event;

import edu.unl.csce466.ExampleMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class ModEvents {

    @Mod.EventBusSubscriber(modid = ExampleMod.MODID)
    public static class ForgeEvents {
        public static boolean start = false;

        @SubscribeEvent
        public static void onPlayerInteract(PlayerInteractEvent event) {
            if (event instanceof PlayerInteractEvent.RightClickEmpty) {
                if (start) {
                    System.out.println("right click");
                    LivingEntity player = event.getPlayer();

                    Vector3d plrPos = player.getEyePosition(0);
                    Vector3d spawnLightingPos = new Vector3d(plrPos.x + 10, plrPos.y, plrPos.z);

                    System.out.println("Player looking at: " + spawnLightingPos);
                }
            }
        }
    }
}
