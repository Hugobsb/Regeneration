package mc.craig.software.regen.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import mc.craig.software.regen.client.screen.widgets.ColorWidget;
import mc.craig.software.regen.common.regen.RegenerationData;
import mc.craig.software.regen.common.regen.transitions.TransitionType;
import mc.craig.software.regen.network.messages.ColorChangeMessage;
import mc.craig.software.regen.util.RConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class ColorScreen extends AbstractContainerScreen {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(RConstants.MODID, "textures/gui/customizer.png");
    public static final ResourceLocation PREFERENCES_BUTTON_LOCATION = new ResourceLocation(RConstants.MODID, "textures/gui/preferences_button.png");

    private Vec3 initialPrimary, initialSecondary;
    private ColorWidget colorChooserPrimary, colorChooserSecondary;

    public ColorScreen() {
        super(new BlankContainer(), Minecraft.getInstance().player.getInventory(), Component.translatable("regen.gui.color_gui"));
        imageWidth = 256;
        imageHeight = 173;
    }

    @Override
    public void init() {
        super.init();
        int cx = (width - imageWidth) / 2;
        int cy = (height - imageHeight) / 2;

        RegenerationData.get(Minecraft.getInstance().player).ifPresent((data) -> {
            initialPrimary = data.getPrimaryColors();
            initialSecondary = data.getSecondaryColors();
        });


        final int btnW = 60, btnH = 18;

        this.addRenderableWidget(new ImageButton(4, 4, 20, 18, 0, 0, 19, ColorScreen.PREFERENCES_BUTTON_LOCATION, (button) -> {
            Minecraft.getInstance().setScreen(null);
        }));

        // Reset Style Button
        this.addRenderableWidget(new Button(cx + 100, cy + 145, btnW, btnH + 2, Component.translatable("regen.gui.undo"), button -> {
            Color primaryColour = new Color((float) initialPrimary.x, (float) initialPrimary.y, (float) initialPrimary.z);
            Color secondaryColour = new Color((float) initialSecondary.x, (float) initialSecondary.y, (float) initialSecondary.z);
            colorChooserPrimary.setColor(primaryColour.getRGB());
            colorChooserSecondary.setColor(secondaryColour.getRGB());
            updateScreenAndServer();
        }));

        // Close Button
        this.addRenderableWidget(new Button(cx + 25, cy + 145, btnW, btnH + 2, Component.translatable("regen.gui.back"), button -> Minecraft.getInstance().setScreen(new PreferencesScreen())));

        // Default Button
        this.addRenderableWidget(new Button(cx + (90 * 2), cy + 145, btnW, btnH + 2, Component.translatable("regen.gui.default"), button -> RegenerationData.get(Minecraft.getInstance().player).ifPresent((data) -> {
            TransitionType regenType = data.transitionType();
            Vec3 primColor = regenType.getDefaultPrimaryColor();
            Vec3 secColor = regenType.getDefaultSecondaryColor();
            Color primaryColour = new Color((float) primColor.x, (float) primColor.y, (float) primColor.z);
            Color secondaryColour = new Color((float) secColor.x, (float) secColor.y, (float) secColor.z);
            colorChooserPrimary.setColor(primaryColour.getRGB());
            colorChooserSecondary.setColor(secondaryColour.getRGB());
            updateScreenAndServer();
        })));

        colorChooserPrimary = new ColorWidget(font, cx + 20, cy + 35, 70, 20, Component.literal("Regen"), new Color((float) initialPrimary.x, (float) initialPrimary.y, (float) initialPrimary.z).getRGB(), p_onPress_1_ -> updateScreenAndServer());

        colorChooserSecondary = new ColorWidget(font, cx + 150, cy + 35, 70, 20, Component.literal("Regen"), new Color((float) initialSecondary.x, (float) initialSecondary.y, (float) initialSecondary.z).getRGB(), p_onPress_1_ -> updateScreenAndServer());

        addRenderableWidget(colorChooserPrimary);
        addRenderableWidget(colorChooserSecondary);
    }

    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
        this.font.draw(poseStack, this.title.getString(), (float) this.titleLabelX, (float) this.titleLabelY, 4210752);
    }

    public void updateScreenAndServer() {
        CompoundTag nbt = new CompoundTag();
        Color primary = new Color(colorChooserPrimary.getColor());
        Color secondary = new Color(colorChooserSecondary.getColor());
        nbt.putFloat(RConstants.PRIMARY_RED, (float) primary.getRed() / 255F);
        nbt.putFloat(RConstants.PRIMARY_GREEN, (float) primary.getGreen() / 255F);
        nbt.putFloat(RConstants.PRIMARY_BLUE, (float) primary.getBlue() / 255F);

        nbt.putFloat(RConstants.SECONDARY_RED, (float) secondary.getRed() / 255F);
        nbt.putFloat(RConstants.SECONDARY_GREEN, (float) secondary.getGreen() / 255F);
        nbt.putFloat(RConstants.SECONDARY_BLUE, (float) secondary.getBlue() / 255F);
        new ColorChangeMessage(nbt).send();
    }

    @Override
    protected void renderBg(@NotNull PoseStack matrixStack, float partialTicks, int x, int y) {

        super.tick();
        colorChooserPrimary.tick();
        colorChooserSecondary.tick();

        this.renderBackground(matrixStack);

        if (this.minecraft != null) {
            RenderSystem.setShaderTexture(0, BACKGROUND);
            int i = (this.width - this.imageWidth) / 2;
            int j = (this.height - this.imageHeight) / 2;
            this.blit(matrixStack, i, j, 0, 0, this.imageWidth, this.imageHeight);
        }

        int cx = (width - imageWidth) / 2;
        int cy = (height - imageHeight) / 2;

        colorChooserPrimary.render(matrixStack, x, y, partialTicks);
        colorChooserSecondary.render(matrixStack, x, y, partialTicks);

        RegenerationData.get(Minecraft.getInstance().player).ifPresent((cap) -> {
            String str = Component.translatable("regen.gui.primary").getString();
            int length = Minecraft.getInstance().font.width(str);
            this.font.draw(matrixStack, Component.literal(str).getString(), (float) cx + 55 - length / 2, cy + 19, 4210752);
            str = Component.translatable("regen.gui.secondary").getString();
            length = font.width(str);
            this.font.draw(matrixStack, Component.literal(str).getString(), cx + 185 - length / 2, cy + 19, 4210752);
        });


    }
}