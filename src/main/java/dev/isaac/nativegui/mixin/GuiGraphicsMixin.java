package dev.isaac.nativegui.mixin;

import dev.isaac.nativegui.client.GuiTextureRouter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Vanilla 1.21.1 funnels GuiGraphics.blit background drawing through this overload.
 * Replace only the first (texture) argument before the final blit is submitted.
 * This is our own NeoForge adapter, not an OptiGUI-derived mixin.
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @ModifyVariable(
            method = "blit(Lnet/minecraft/resources/ResourceLocation;IIIIIIIFFII)V",
            at = @At("HEAD"), argsOnly = true, index = 1)
    private ResourceLocation nativegui$replaceGuiTexture(ResourceLocation original) {
        return GuiTextureRouter.route(original);
    }
}
