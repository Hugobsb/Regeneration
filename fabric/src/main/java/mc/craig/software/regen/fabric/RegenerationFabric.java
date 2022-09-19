package mc.craig.software.regen.fabric;

import mc.craig.software.regen.Regeneration;
import mc.craig.software.regen.common.world.structures.pieces.StructurePieces;
import mc.craig.software.regen.config.RegenConfig;
import mc.craig.software.regen.fabric.handlers.CommonEvents;
import mc.craig.software.regen.util.RConstants;
import mc.craig.software.regen.util.fabric.PlatformImpl;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraftforge.api.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class RegenerationFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Regeneration.init();
        ModLoadingContext.registerConfig(Regeneration.MOD_ID, ModConfig.Type.COMMON, RegenConfig.COMMON_SPEC);
        ModLoadingContext.registerConfig(Regeneration.MOD_ID, ModConfig.Type.CLIENT, RegenConfig.CLIENT_SPEC);
        PlatformImpl.init();
        CommonEvents.init();
        StructurePieces.init();
        levelManipulation();
    }

    private void levelManipulation() {
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_ORES, ResourceKey.create(Registry.PLACED_FEATURE_REGISTRY, new ResourceLocation(RConstants.MODID, "ore_zinc")));
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_ORES, ResourceKey.create(Registry.PLACED_FEATURE_REGISTRY, new ResourceLocation(RConstants.MODID, "ore_zinc_small")));
    }
}
