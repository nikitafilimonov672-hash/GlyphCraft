package ru.zohov.glyphcraft.mixin;

import java.util.List;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BookEditScreen.class)
public interface BookEditScreenAccessor {
    @Accessor("pages") List<String> glyphcraft$getPages();
    @Accessor("currentPage") int glyphcraft$getCurrentPage();
    @Accessor("currentPage") void glyphcraft$setCurrentPage(int page);
    @Accessor("page") MultiLineEditBox glyphcraft$getPageEditor();
    @Invoker("updatePageContent") void glyphcraft$updatePageContent();
    @Invoker("updateButtonVisibility") void glyphcraft$updateButtonVisibility();
}
