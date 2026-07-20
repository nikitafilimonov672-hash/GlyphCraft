package ru.zohov.glyphcraft.client;

import net.fabricmc.api.ClientModInitializer;
import ru.zohov.glyphcraft.GlyphCraftMod;

public final class GlyphCraftClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        GlyphCraftMod.LOGGER.info("GlyphCraft активируется только в книге и редакторе таблички внутри мира");
    }
}
