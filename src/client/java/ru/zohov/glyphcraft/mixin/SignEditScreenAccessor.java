package ru.zohov.glyphcraft.mixin;

import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractSignEditScreen.class)
public interface SignEditScreenAccessor {
    @Accessor("messages") String[] glyphcraft$getMessages();
    @Accessor("line") int glyphcraft$getLine();
    @Accessor("line") void glyphcraft$setLine(int line);
    @Accessor("signField") TextFieldHelper glyphcraft$getSignField();
    @Invoker("setMessage") void glyphcraft$setMessage(String value);
}
