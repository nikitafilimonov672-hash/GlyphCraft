package ru.zohov.glyphcraft.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.zohov.glyphcraft.client.ContextualEditorOverlay;

@Mixin(AbstractSignEditScreen.class)
public abstract class LegacySignEditScreenMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void glyphcraft$renderOverlay(GuiGraphics graphics, int mouseX, int mouseY, float tick, CallbackInfo ci) {
        ContextualEditorOverlay.render(graphics, (AbstractSignEditScreen) (Object) this, mouseX, mouseY);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void glyphcraft$modalKey(int key, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (ContextualEditorOverlay.keyPressed((AbstractSignEditScreen) (Object) this, key, scanCode, modifiers)) cir.setReturnValue(true);
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void glyphcraft$modalChar(char codepoint, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (ContextualEditorOverlay.charTyped((AbstractSignEditScreen) (Object) this, codepoint, modifiers)) cir.setReturnValue(true);
    }
}
