package dev.isaac.nativegui;

import com.mojang.logging.LogUtils;
import dev.isaac.nativegui.client.GuiPackReload;
import dev.isaac.nativegui.client.InteractionTracker;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/** Native client-only custom container GUI support for NeoForge 1.21.1. */
@Mod(value = NativeGui.MOD_ID, dist = Dist.CLIENT)
public final class NativeGui {
    public static final String MOD_ID = "nativegui";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NativeGui(IEventBus modBus) {
        modBus.addListener(NativeGui::onClientReloadListeners);
        NeoForge.EVENT_BUS.addListener(InteractionTracker::rightClick);
        NeoForge.EVENT_BUS.addListener(InteractionTracker::entityClick);
        NeoForge.EVENT_BUS.addListener(InteractionTracker::specificEntityClick);
    }

    private static void onClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(GuiPackReload.INSTANCE);
    }
}
