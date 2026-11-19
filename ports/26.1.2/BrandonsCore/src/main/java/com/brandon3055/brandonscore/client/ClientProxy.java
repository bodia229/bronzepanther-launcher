package com.brandon3055.brandonscore.client;

import com.brandon3055.brandonscore.CommonProxy;
import com.brandon3055.brandonscore.handlers.IProcess;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

/**
 * Created by Brandon on 14/5/2015.
 */
public class ClientProxy extends CommonProxy {

    @Override
    public MinecraftServer getMCServer() {
        return super.getMCServer();
    }

    @Override
    public Level getClientWorld() {
        return Minecraft.getInstance().level;
    }

    @Override
    public boolean isCTRLKeyDown() {
        return Minecraft.getInstance().hasControlDown();
    }

    @Override
    public Player getClientPlayer() {
        return Minecraft.getInstance().player;
    }

    @Override
    public void addProcess(IProcess iProcess) {
        ProcessHandlerClient.addProcess(iProcess);
    }

    @Override
    public void runSidedProcess(IProcess process) {
        ProcessHandlerClient.addProcess(process);
    }

    @Override
    public void sendIndexedMessage(Player player, Component message, MessageSignature signature) {
        IndexedChatMessages.update(Minecraft.getInstance().gui.getChat(), message, signature);
    }

    public void deleteMessage(MessageSignature signature) {
        Minecraft mc=Minecraft.getInstance();
        if(mc!=null&&mc.gui.getChat()!=null)IndexedChatMessages.delete(mc.gui.getChat(),signature);
    }

    @Override
    public void setClipboardString(String text) {
        Minecraft.getInstance().keyboardHandler.setClipboard(text);
    }
}
