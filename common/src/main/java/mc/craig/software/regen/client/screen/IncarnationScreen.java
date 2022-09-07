package mc.craig.software.regen.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import mc.craig.software.regen.client.skin.VisualManipulator;
import mc.craig.software.regen.client.visual.SkinRetriever;
import mc.craig.software.regen.common.regen.RegenerationData;
import mc.craig.software.regen.network.messages.NextSkinMessage;
import mc.craig.software.regen.util.ClientUtil;
import mc.craig.software.regen.util.PlayerUtil;
import mc.craig.software.regen.util.RConstants;
import mc.craig.software.regen.util.RegenUtil;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;


public class IncarnationScreen extends AbstractContainerScreen {

    private static final ResourceLocation screenBackground = new ResourceLocation(RConstants.MODID, "textures/gui/customizer.png");
    public static boolean isAlex = true;
    private static ResourceLocation currentTexture = DefaultPlayerSkin.getDefaultSkin();
    private static PlayerUtil.SkinType currentSkinType = RegenerationData.get(Objects.requireNonNull(Minecraft.getInstance().player)).orElse(null).preferredModel();
    private static PlayerUtil.SkinType renderChoice = currentSkinType;
    private static List<File> skins = null;
    private static List<ResourceLocation> skinTextures = new ArrayList<>();
    private static int position = 0;
    private RCheckbox excludeTrending;
    private EditBox searchField;
    private ArrayList<DescButton> descButtons = new ArrayList<>();

    public IncarnationScreen() {
        super(new BlankContainer(), Objects.requireNonNull(Minecraft.getInstance().player).getInventory(), Component.literal("Next Incarnation"));
        imageWidth = 256;
        imageHeight = 173;
    }

    public static void updateModels() {
        if (!skins.isEmpty()) {

            for (ResourceLocation resourceLocation : skinTextures) {
              //TODO why he sceram  Minecraft.getInstance().getTextureManager().release(resourceLocation);
            }

            for (File file : skins) {
                ResourceLocation resource = SkinRetriever.fileTotexture(file);
                skinTextures.add(resource);
            }

            currentTexture = skinTextures.get(position);
            isAlex = skins.get(position).toString().contains("\\skins\\alex");
            renderChoice = isAlex ? PlayerUtil.SkinType.ALEX : PlayerUtil.SkinType.STEVE;

        }
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void init() {
        super.init();

        int buttonOffset = 15;
        int cx = (width - imageWidth) / 2;
        int cy = (height - imageHeight) / 2;
        final int btnW = 55, btnH = 18;
        position = 0;
        skins = SkinRetriever.listAllSkins(PlayerUtil.SkinType.EITHER);
        if (skins.isEmpty()) {
            Minecraft.getInstance().setScreen(new RErrorScreen(Component.translatable("No Skins for " + Component.translatable("regeneration.skin_type." + currentSkinType.name().toLowerCase()).getString()), Component.translatable("Please place skins in the local Directory")));
        }


        this.searchField = new EditBox(this.font, cx + 10, cy + 30 + buttonOffset, cx - 15, 20, this.searchField, Component.translatable("skins.search"));

        this.searchField.setMaxLength(128);
        this.searchField.setFocus(true);

        this.searchField.setResponder((s) -> {
            position = 0;
            skins.removeIf(file -> !file.getName().toLowerCase().contains(s.toLowerCase()));
            if (skins.isEmpty() || searchField.getValue().isEmpty()) {
                skins = SkinRetriever.listAllSkins(currentSkinType);
            }
            stripTrending();
            Collections.sort(skins);
            updateModels();
        });
        this.setInitialFocus(this.searchField);
        this.addWidget(this.searchField);



        DescButton btnPrevious = new DescButton(cx + 140, cy + 60, 20, 20, Component.translatable("regen.gui.previous"), button -> {
            if (searchField.getValue().isEmpty()) {
                skins = SkinRetriever.listAllSkins(currentSkinType);
            }
            stripTrending();

            if (!currentTexture.equals(Minecraft.getInstance().player.getSkinTextureLocation())) {
                if (position >= skins.size() - 1) {
                    position = 0;
                } else {
                    position++;
                }
                updateModels();
            }
        }).setDescription(new String[]{"button.tooltip.previous_skin"});

        DescButton btnNext = new DescButton(cx + 215, cy + 60, 20, 20, Component.translatable("regen.gui.next"), button -> {
            // Previous
            if (searchField.getValue().isEmpty()) {
                skins = SkinRetriever.listAllSkins(currentSkinType);
            }

            stripTrending();

            if (!currentTexture.equals(Minecraft.getInstance().player.getSkinTextureLocation())) {
                if (position > 0) {
                    position--;
                } else {
                    position = skins.size() - 1;
                }
                currentTexture = SkinRetriever.fileTotexture(skins.get(position));
                updateModels();
            }
        }).setDescription(new String[]{"button.tooltip.next_skin"});

        DescButton btnBack = new DescButton(cx + 10, cy + 115 - buttonOffset, btnW, btnH + 2, Component.translatable("regen.gui.back"), button -> Minecraft.getInstance().setScreen(new PreferencesScreen()));

        DescButton btnOpenFolder = new DescButton(cx + 90 - 20, cy + 115 - buttonOffset, btnW, btnH + 2, Component.translatable("regen.gui.open_folder"), button -> Util.getPlatform().openFile(SkinRetriever.SKINS_DIRECTORY)).setDescription(new String[]{"button.tooltip.open_folder"});

        DescButton btnSave = new DescButton(cx + 90 - 20, cy + 90 - buttonOffset, btnW, btnH + 2, Component.translatable("regen.gui.save"), button -> {
            updateModels();
           new NextSkinMessage(RegenUtil.fileToBytes(skins.get(position)), isAlex).send();
        }).setDescription(new String[]{"button.tooltip.save_skin"});

        DescButton btnResetSkin = new DescButton(cx + 10, cy + 90 - buttonOffset, btnW, btnH + 2, Component.translatable("regen.gui.reset_skin"), button -> VisualManipulator.sendResetMessage()).setDescription(new String[]{"button.tooltip.reset_mojang"});

        this.excludeTrending = new RCheckbox( cx + 10, cy + 145, 150, 20, Component.translatable("Trending?"), true, checkboxButton -> {
            if (checkboxButton instanceof Checkbox check) {
                position = 0;
                searchField.setValue("");
                if (!check.selected()) {
                    skins.removeIf(file -> file.getAbsolutePath().contains("web"));
                } else {
                    skins = SkinRetriever.listAllSkins(currentSkinType);
                }
                updateModels();
            }
        });
        this.addRenderableWidget(this.excludeTrending);

        addRenderableDescButton(btnNext);
        addRenderableDescButton(btnPrevious);
        addRenderableDescButton(btnOpenFolder);
        addRenderableDescButton(btnBack);
        addRenderableDescButton(btnSave);
        addRenderableDescButton(btnResetSkin);

        RegenerationData.get(minecraft.player).ifPresent((data) -> currentSkinType = data.preferredModel());

        RegenerationData.get(Minecraft.getInstance().player).ifPresent((data) -> currentSkinType = data.preferredModel());
        updateModels();

        excludeTrending.active = true;


    }

    public void addRenderableDescButton(DescButton descButton){
        addRenderableWidget(descButton);
        descButtons.add(descButton);
    }

    private void stripTrending() {
        if (!excludeTrending.selected()) {
            skins.removeIf(file -> file.getAbsoluteFile().toPath().toString().contains("mineskin"));
        }
    }

    @Override
    protected void renderBg(@NotNull PoseStack matrixStack, float partialTicks, int x, int y) {
        this.renderBackground(matrixStack);
        RenderSystem.setShaderTexture(0, screenBackground);
        blit(matrixStack, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        renderSkinToGui(matrixStack, x, y);
        drawCenteredString(matrixStack, Minecraft.getInstance().font, Component.translatable("regen.gui.current_skin").getString(), width / 2 + 60, height / 2 + 30, Color.WHITE.getRGB());
        if (!skins.isEmpty() && position < skins.size()) {
            matrixStack.pushPose();
            String name = skins.get(position).getName().replaceAll(".png", "");
            renderWidthScaledText(name, matrixStack, this.font, width / 2 + 60, height / 2 + 40, Color.WHITE.getRGB(), 100);
            matrixStack.popPose();
        }
        drawCenteredString(matrixStack, Minecraft.getInstance().font, Component.translatable("Search"), (width - imageWidth) / 2 + 28, (height - imageHeight) / 2 + 20 + 15, Color.WHITE.getRGB());
    }

    private void renderSkinToGui(PoseStack matrixStack, int x, int y) {
        matrixStack.pushPose();
        LocalPlayer player = Minecraft.getInstance().player;
        ResourceLocation backup = ClientUtil.getPlayerInfo(player).getSkinLocation();
        boolean backupSkinType = ClientUtil.isAlex(player);
        VisualManipulator.PLAYER_SKINS.put(player.getUUID(), currentTexture);
        VisualManipulator.setPlayerSkinType(Minecraft.getInstance().player, renderChoice == PlayerUtil.SkinType.ALEX);
        InventoryScreen.renderEntityInInventory(width / 2 + 60, height / 2 + 20, 45, (float) (leftPos + 170) - x, (float) (topPos + 75 - 25) - y, Minecraft.getInstance().player);
        VisualManipulator.PLAYER_SKINS.put(player.getUUID(), backup);
        VisualManipulator.setPlayerSkinType(Minecraft.getInstance().player, backupSkinType);
        matrixStack.popPose();
    }

    @Override
    public void resize(Minecraft p_96575_, int p_96576_, int p_96577_) {
        String s = this.searchField.getValue();
        super.resize(p_96575_, p_96576_, p_96577_);
        this.searchField.setValue(s);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {

        searchField.keyPressed(keyCode, scanCode, modifiers);

        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            if (!currentTexture.equals(Minecraft.getInstance().player.getSkinTextureLocation())) {
                if (position >= skins.size() - 1) {
                    position = 0;
                } else {
                    position++;
                }
                Minecraft.getInstance().getTextureManager().release(currentTexture);
                currentTexture = SkinRetriever.fileTotexture(skins.get(position));
                updateModels();
            }
        }

        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            if (!currentTexture.equals(Minecraft.getInstance().player.getSkinTextureLocation())) {
                if (position > 0) {
                    position--;
                } else {
                    position = skins.size() - 1;
                }
                currentTexture = SkinRetriever.fileTotexture(skins.get(position));
                updateModels();
            }
        }

        if (keyCode == 256 && this.shouldCloseOnEsc()) {
            this.onClose();
            return true;
        }
        return false;
    }

    @Override
    protected void renderLabels(@NotNull PoseStack matrixStack, int x, int y) {
        this.font.draw(matrixStack, this.title.getString(), (float) this.titleLabelX, (float) this.titleLabelY, 4210752);
    }

    @Override
    public void render(@NotNull PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {

        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.searchField.render(matrixStack, mouseX, mouseY, partialTicks);

        for (int i = 0; i < 12; i++) {
            RenderSystem.setShaderTexture(0, skinTextures.get(i));
            GuiComponent.blit(matrixStack, Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2 + (i * 10) - 80, Minecraft.getInstance().getWindow().getGuiScaledHeight() / 2 - 45, 8, 8, 8, 8, 64, 64);
            GuiComponent.blit(matrixStack, Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2 + (i * 10) - 80, Minecraft.getInstance().getWindow().getGuiScaledHeight() / 2 - 45, 40, 8, 8, 8, 64, 64);
        }

        //TODO
        descButtons.forEach(descButton -> {
            if (descButton.isHoveredOrFocused()) {
                if (descButton.getDescription() != null) {
                    //    public void renderTooltip(PoseStack poseStack, List<Component> tooltips, Optional<TooltipComponent> visualTooltipComponent, int mouseX, int mouseY) {
                    this.renderTooltip(matrixStack, descButton.getDescription(), Optional.empty(), mouseX, mouseY);
                }
            }
        });
    }

    //Spectre0987

    /**
     * @param text  - The text you'd like to draw
     * @param width - The max width of the text, scales to maintain this width if larger than it
     */
    public void renderWidthScaledText(String text, PoseStack matrix, Font font, float x, float y, int color, int width) {
        matrix.pushPose();
        int textWidth = font.width(text);
        float scale = width / (float) textWidth;
        scale = Mth.clamp(scale, 0.0F, 1.0F);
        matrix.translate(x, y, 0);
        matrix.scale(scale, scale, scale);
        drawCenteredString(matrix, Minecraft.getInstance().font, text, 0, 0, color);
        matrix.popPose();
    }

}