package mc.craig.software.regen.forge.handlers;

import mc.craig.software.regen.common.commands.RegenCommand;
import mc.craig.software.regen.common.regen.IRegen;
import mc.craig.software.regen.common.regen.RegenerationData;
import mc.craig.software.regen.common.regen.state.RegenStates;
import mc.craig.software.regen.common.traits.TraitRegistry;
import mc.craig.software.regen.config.RegenConfig;
import mc.craig.software.regen.util.PlayerUtil;
import mc.craig.software.regen.util.RegenSources;
import mc.craig.software.regen.util.RegenUtil;
import mc.craig.software.regen.util.constants.RConstants;
import mc.craig.software.regen.util.constants.RMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonEvents {

    @SubscribeEvent
    public static void onServerStart(ServerStartingEvent serverStartingEvent) {
        RegenUtil.setupNames();
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity livingEntity = event.getEntity();
        if (livingEntity == null) return;

        RegenerationData.get(livingEntity).ifPresent(data -> {

            // Stop certain damages
            if (event.getSource() == RegenSources.REGEN_DMG_KILLED)
                return;

            //Handle Post
            if (data.regenState() == RegenStates.POST && event.getSource() != DamageSource.OUT_OF_WORLD && event.getSource() != RegenSources.REGEN_DMG_HAND) {
                event.setAmount(1.5F);
                PlayerUtil.sendMessage(livingEntity, Component.translatable(RMessages.POST_REDUCED_DAMAGE), true);
            }

            if (data.isTraitActive()) {
                if (data.getCurrentTrait() == TraitRegistry.FIRE_RESISTANCE.get() && event.getSource().isFire()) {
                    event.setCanceled(true);
                }

                if (data.getCurrentTrait() == TraitRegistry.ARROW_DODGE.get() && event.getSource().isProjectile()) {
                    event.setCanceled(true);
                }
            }


            //Handle Death
            if (data.regenState() == RegenStates.REGENERATING && RegenConfig.COMMON.regenFireImmune.get() && event.getSource().isFire() || data.regenState() == RegenStates.REGENERATING && event.getSource().isExplosion()) {
                event.setCanceled(true);
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void adMortemInimicusButForGrace(LivingDamageEvent event) {
        if (event.getEntity() == null) return;
        RegenerationData.get(event.getEntity()).ifPresent((cap -> {
            if ((cap.regenState().isGraceful()) && event.getEntity().getHealth() - event.getAmount() < 0) {
                //uh oh, we're dying in grace. Forcibly regenerate before all (?) death prevention mods
                boolean notDead = cap.stateManager().onKilled(event.getSource());
                event.setCanceled(notDead);
            }
        }));
    }


    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void adMortemInimicus(LivingDeathEvent event) {
        if (event.getEntity() == null) return;
        RegenerationData.get(event.getEntity()).ifPresent((cap) -> {
            if ((event.getSource() == RegenSources.REGEN_DMG_CRITICAL || event.getSource() == RegenSources.REGEN_DMG_KILLED)) {
                if (RegenConfig.COMMON.loseRegensOnDeath.get()) {
                    cap.extractRegens(cap.regens());
                }
                if (event.getEntity() instanceof ServerPlayer serverPlayer)
                    cap.syncToClients(serverPlayer);
                return;
            }
            if (cap.stateManager() == null) return;
            boolean notDead = cap.stateManager().onKilled(event.getSource());
            event.setCanceled(notDead);
        });

    }

    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        LivingEntity livingEntity = event.getEntity();


        RegenerationData.get(livingEntity).ifPresent((data) -> {
            boolean isRegenerating = data.regenState() == RegenStates.REGENERATING;

            if (!isRegenerating) {
                if (data.isTraitActive() && data.getCurrentTrait() == TraitRegistry.KNOCKBACK.get()) {
                    event.setCanceled(true);
                    return;
                }
            }

            event.setCanceled(isRegenerating);
        });
    }

    @SubscribeEvent
    public static void onTrackPlayer(PlayerEvent.StartTracking startTracking) {
        RegenerationData.get(startTracking.getEntity()).ifPresent(iRegen -> iRegen.syncToClients(null));
    }

    @SubscribeEvent
    public static void onPunchBlock(PlayerInteractEvent.LeftClickBlock e) {
        if (e.getEntity().level.isClientSide) return;
        RegenerationData.get(e.getEntity()).ifPresent((data) -> data.stateManager().onPunchBlock(e.getPos(), e.getLevel().getBlockState(e.getPos()), e.getEntity()));
    }

    @SubscribeEvent
    public static void onLive(LivingEvent.LivingTickEvent livingUpdateEvent) {
        RegenerationData.get(livingUpdateEvent.getEntity()).ifPresent(IRegen::tick);
    }

    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        RegenCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onCut(PlayerInteractEvent.RightClickItem event) {
        RegenUtil.spawnHandIfPossible(event.getEntity(), event.getItemStack());
    }


}
