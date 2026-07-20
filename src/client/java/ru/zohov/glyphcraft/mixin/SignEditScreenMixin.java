package ru.zohov.glyphcraft.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.zohov.glyphcraft.client.ContextualEditorOverlay;

/** Sign-specific hooks are needed because AbstractSignEditScreen overrides
 * keyPressed/charTyped instead of routing those events through the generic
 * ContainerEventHandler hook. */
@Mixin(AbstractSignEditScreen.class)
public abstract class SignEditScreenMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void glyphcraft$renderOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ContextualEditorOverlay.render(graphics, (AbstractSignEditScreen) (Object) this, mouseX, mouseY);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void glyphcraft$modalKey(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (ContextualEditorOverlay.keyPressed((AbstractSignEditScreen) (Object) this, event.key(), event.scancode(), event.modifiers())) cir.setReturnValue(true);
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void glyphcraft$modalChar(CharacterEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (ContextualEditorOverlay.charTyped((AbstractSignEditScreen) (Object) this, event.codepoint(), event.modifiers())) cir.setReturnValue(true);
    }
}
