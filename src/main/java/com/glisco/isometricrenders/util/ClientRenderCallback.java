package com.glisco.isometricrenders.util;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.MinecraftClient;

public interface ClientRenderCallback {

    Event<ClientRenderCallback> EVENT = EventFactory.createArrayBacked(ClientRenderCallback.class, clientRenderCallbacks -> client -> {
        for (var callback : clientRenderCallbacks) {
            callback.onRenderStart(client);
        }
    });

    void onRenderStart(MinecraftClient client);

}
