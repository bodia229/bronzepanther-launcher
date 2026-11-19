package com.brandon3055.brandonscore.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import java.util.List;

@Mixin(ChatComponent.class)
public interface ChatComponentAccess {
    @Accessor("allMessages") List<GuiMessage> brandonscore$messages();
    @Accessor("trimmedMessages") List<GuiMessage.Line> brandonscore$displayLines();
    @Invoker("refreshTrimmedMessages") void brandonscore$refresh();
    @Invoker("addMessage") void brandonscore$add(Component message,MessageSignature signature,GuiMessageSource source,GuiMessageTag tag);
}
