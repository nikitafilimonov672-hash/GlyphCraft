package ru.zohov.glyphcraft.mixin;

import net.minecraft.client.gui.components.MultilineTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultilineTextField.class)
public interface MultilineTextFieldAccessor {
    @Accessor("cursor") int glyphcraft$getCursorRaw();
    @Accessor("selectCursor") int glyphcraft$getSelectCursorRaw();
    @Accessor("value") void glyphcraft$setValueRaw(String value);
    @Accessor("cursor") void glyphcraft$setCursorRaw(int cursor);
    @Accessor("selectCursor") void glyphcraft$setSelectCursorRaw(int cursor);
    @Invoker("onValueChange") void glyphcraft$onValueChange();
}
