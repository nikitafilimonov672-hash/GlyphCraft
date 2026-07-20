package ru.zohov.glyphcraft.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.zohov.glyphcraft.client.ContextualEditorOverlay;

/** Input hooks used by Minecraft 1.21 through 1.21.8. */
@Mixin(ContainerEventHandler.class)
public interface LegacyContainerEventHandlerMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void glyphcraft$paletteClick(double x, double y, int button, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (Minecraft.getInstance().screen instanceof Screen screen
                    && ContextualEditorOverlay.click(screen, x, y, button)) cir.setReturnValue(true);
        } catch (RuntimeException failure) {
            ContextualEditorOverlay.recoverFromInputFailure(failure);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void glyphcraft$modalKey(int key, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (Minecraft.getInstance().screen instanceof Screen screen
                && ContextualEditorOverlay.keyPressed(screen, key, scanCode, modifiers)) cir.setReturnValue(true);
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void glyphcraft$modalChar(char codepoint, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (Minecraft.getInstance().screen instanceof Screen screen
                && ContextualEditorOverlay.charTyped(screen, codepoint, modifiers)) cir.setReturnValue(true);
    }
}
