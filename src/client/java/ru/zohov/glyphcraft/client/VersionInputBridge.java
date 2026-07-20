package ru.zohov.glyphcraft.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import net.minecraft.client.gui.components.EditBox;

/** Bridges the pre-1.21.9 primitive input API and the newer event API. */
public final class VersionInputBridge {
    private VersionInputBridge() { }

    public static boolean keyPressed(EditBox box, int key, int scanCode, int modifiers) {
        try {
            for (Method method : box.getClass().getMethods()) {
                if (!method.getName().equals("keyPressed")) continue;
                Class<?>[] types = method.getParameterTypes();
                if (types.length == 3 && types[0] == int.class) {
                    return (boolean) method.invoke(box, key, scanCode, modifiers);
                }
                if (types.length == 1) {
                    Constructor<?> ctor = types[0].getConstructor(int.class, int.class, int.class);
                    return (boolean) method.invoke(box, ctor.newInstance(key, scanCode, modifiers));
                }
            }
        } catch (ReflectiveOperationException ignored) { }
        return false;
    }

    public static boolean charTyped(EditBox box, int codepoint, int modifiers) {
        try {
            for (Method method : box.getClass().getMethods()) {
                if (!method.getName().equals("charTyped")) continue;
                Class<?>[] types = method.getParameterTypes();
                if (types.length == 2 && types[0] == char.class) {
                    return (boolean) method.invoke(box, (char) codepoint, modifiers);
                }
                if (types.length == 1) {
                    Constructor<?> ctor = types[0].getConstructor(int.class, int.class);
                    return (boolean) method.invoke(box, ctor.newInstance(codepoint, modifiers));
                }
            }
        } catch (ReflectiveOperationException ignored) { }
        return false;
    }
}
