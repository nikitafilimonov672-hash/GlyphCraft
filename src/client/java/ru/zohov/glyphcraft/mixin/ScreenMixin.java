package ru.zohov.glyphcraft.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zohov.glyphcraft.client.ContextualEditorOverlay;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void glyphcraft$renderPalette(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        // Sign screens render their 3-D sign after Screen.render returns. A
        // dedicated sign mixin draws our overlay after that final sign pass.
        if (!(screen instanceof AbstractSignEditScreen)) {
            ContextualEditorOverlay.render(graphics, screen, mouseX, mouseY);
        }
    }
}
