package com.brandon3055.brandonscore.client.gui.modulargui;

import codechicken.lib.gui.modular.elements.GuiEnergyBar;
import codechicken.lib.gui.modular.elements.GuiRectangle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.brandon3055.brandonscore.client.render.GuiRenderBridge;
import com.brandon3055.brandonscore.client.render.NativeEnergyBar;
import codechicken.lib.gui.render.GuiGraphicsExtension;
import net.minecraft.client.renderer.RenderPipelines;
import codechicken.lib.gui.modular.lib.geometry.GuiParent;
import codechicken.lib.util.FormatUtil;
import com.brandon3055.brandonscore.api.power.IOInfo;
import com.brandon3055.brandonscore.api.power.IOPStorage;
import com.brandon3055.brandonscore.client.BCGuiTextures;
import com.brandon3055.brandonscore.utils.EnergyUtils;
import com.brandon3055.brandonscore.utils.Utils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static net.minecraft.ChatFormatting.*;

/**
 * Created by brandon3055 on 13/02/2024
 */
public class ShaderEnergyBar extends GuiEnergyBar {
    private Supplier<Boolean> shaderEnabled = () -> true;
    private Supplier<Boolean> disabled = () -> false;

    public ShaderEnergyBar(@NotNull GuiParent<?> parent) {
        super(parent);
    }

    public static BiFunction<Long, Long, List<Component>> opEnergyFormatter(@Nullable IOPStorage storage) {
        return (energy, capacity) -> {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("mod_gui.brandonscore.energy_bar.operational_potential").withStyle(DARK_AQUA));
            boolean shift = net.minecraft.client.Minecraft.getInstance().hasShiftDown();
            tooltip.add(Component.translatable("mod_gui.brandonscore.energy_bar.capacity")
                    .withStyle(GOLD)
                    .append(" ")
                    .append(Component.literal(shift ? FormatUtil.addCommas(capacity) : FormatUtil.formatNumber(capacity))
                            .withStyle(GRAY)
                            .append(" ")
                            .append(Component.translatable("mod_gui.brandonscore.energy_bar.op")
                                    .withStyle(GRAY)
                            )
                    )
            );
            tooltip.add(Component.translatable("mod_gui.brandonscore.energy_bar.stored")
                    .withStyle(GOLD)
                    .append(" ")
                    .append(Component.literal(shift ? FormatUtil.addCommas(energy) : FormatUtil.formatNumber(energy))
                            .withStyle(GRAY)
                    )
                    .append(" ")
                    .append(Component.translatable("mod_gui.brandonscore.energy_bar.op")
                            .withStyle(GRAY)
                    )
                    .append(Component.literal(String.format(" (%.2f%%)", ((double) energy / (double) capacity) * 100D))
                            .withStyle(GRAY)
                    )
            );
            if (storage != null && storage.getIOInfo() != null) {
                IOInfo ioInfo = storage.getIOInfo();
                if (net.minecraft.client.Minecraft.getInstance().hasShiftDown()) {
                    tooltip.add(Component.translatable("mod_gui.brandonscore.energy_bar.input")
                            .withStyle(GOLD)
                            .append(Component.literal(" +")
                                    .withStyle(GREEN)
                                    .append(Utils.formatNumber(ioInfo.currentInput()))
                            )
                    );
                    tooltip.add(Component.translatable("mod_gui.brandonscore.energy_bar.output")
                            .withStyle(GOLD)
                            .append(Component.literal(" -")
                                    .withStyle(RED)
                                    .append(Utils.formatNumber(ioInfo.currentOutput()))
                            )
                    );
                } else {
                    long io = ioInfo.currentInput() - ioInfo.currentOutput();
                    tooltip.add(Component.translatable("mod_gui.brandonscore.energy_bar.io")
                            .withStyle(GOLD)
                            .append(Component.literal((io > 0 ? " +" : " ") + io + " ")
                                    .withStyle(io > 0 ? GREEN : io < 0 ? RED : GRAY)
                                    .append(Component.translatable("mod_gui.brandonscore.energy_bar.op"))
                            )
                    );
                }
            }
            return tooltip;
        };
    }

    public ShaderEnergyBar setShaderEnabled(Supplier<Boolean> shaderEnabled) {
        this.shaderEnabled = shaderEnabled;
        return this;
    }

    public GuiEnergyBar setDisabled(Supplier<Boolean> disabled) {
        this.disabled = disabled;
        return this;
    }

    public ShaderEnergyBar bindOpStorage(@Nullable IOPStorage storage) {
        if (storage == null) {
            setEnergy(0).setCapacity(0);
        } else {
            setEnergy(storage::getOPStored).setCapacity(storage::getMaxOPStored);
        }
        return this;
    }

    public ShaderEnergyBar setItemSupplier(Supplier<ItemStack> stackSupplier) {
        setCapacity(() -> EnergyUtils.isEnergyItem(stackSupplier.get()) ? EnergyUtils.getMaxEnergyStored(stackSupplier.get()) : 0);
        setEnergy(() -> EnergyUtils.isEnergyItem(stackSupplier.get()) ? EnergyUtils.getEnergyStored(stackSupplier.get()) : 0);
        return this;
    }

    @Override
    public void renderBehind(GuiGraphicsExtractor graphics, double mouseX, double mouseY, float partialTicks) {
        if (disabled.get()) {
            GuiRenderBridge.rect(graphics, xMin(), yMin(), xSize(), ySize(), 0xFF000000);
            return;
        }
        double charge = getCapacity() <= 0 ? 0 : Math.clamp(getEnergy() / (double)getCapacity(), 0, 1);
        if (shaderEnabled.get()) {
            NativeEnergyBar.extract(graphics,xMin(),yMin(),xSize(),ySize(),charge,(mc().gui.getGuiTicks()+partialTicks)/10F);
            return;
        }
        NativeEnergyBar.extractTextured(graphics,xMin(),yMin(),xSize(),ySize(),charge,
                BCGuiTextures.get("bars/energy_empty"),BCGuiTextures.get("bars/energy_full"));
    }

    public Rectangle toScreenSpace(double xPos, double yPos, double xSize, double ySize) {
        double yResScale = (double) mc().getWindow().getHeight() / scaledScreenHeight();
        double xResScale = (double) mc().getWindow().getWidth() / scaledScreenWidth();
        double scaledWidth = xSize * xResScale;
        double scaledHeight = ySize * yResScale;
        int x = (int) (xPos * xResScale);
        int y = (int) (mc().getWindow().getHeight() - (yPos * yResScale) - scaledHeight);
        return new Rectangle(x, y, (int) scaledWidth, (int) scaledHeight);
    }

    public record EnergyBar(GuiRectangle container, ShaderEnergyBar bar) {}

}
