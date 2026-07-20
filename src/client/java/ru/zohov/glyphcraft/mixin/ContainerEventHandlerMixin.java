package ru.zohov.glyphcraft.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.zohov.glyphcraft.client.ContextualEditorOverlay;

@Mixin(ContainerEventHandler.class)
public interface ContainerEventHandlerMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void glyphcraft$paletteClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (Minecraft.getInstance().screen instanceof Screen screen && ContextualEditorOverlay.click(screen, event.x(), event.y(), event.button())) {
                cir.setReturnValue(true);
            }
        } catch (RuntimeException failure) {
            ContextualEditorOverlay.recoverFromInputFailure(failure);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void glyphcraft$modalKey(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (Minecraft.getInstance().screen instanceof Screen screen && ContextualEditorOverlay.keyPressed(screen, event.key(), event.scancode(), event.modifiers())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void glyphcraft$modalChar(CharacterEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (Minecraft.getInstance().screen instanceof Screen screen && ContextualEditorOverlay.charTyped(screen, event.codepoint(), event.modifiers())) {
            cir.setReturnValue(true);
        }
    }
}
