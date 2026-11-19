package com.brandon3055.brandonscore.client;

import com.brandon3055.brandonscore.mixin.ChatComponentAccess;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import java.util.Objects;

/** Replaces indexed mod messages without rebuilding chat wrapping by hand. */
public final class IndexedChatMessages {
    private IndexedChatMessages(){}
    public static void delete(ChatComponent chat,MessageSignature signature){
        var access=(ChatComponentAccess)(Object)chat;
        if(access.brandonscore$messages().removeIf(message->Objects.equals(message.signature(),signature)))access.brandonscore$refresh();
    }
    public static void update(ChatComponent chat,Component message,MessageSignature signature){
        delete(chat,signature);
        if(message!=null)((ChatComponentAccess)(Object)chat).brandonscore$add(message,signature,GuiMessageSource.SYSTEM_SERVER,null);
    }
}
