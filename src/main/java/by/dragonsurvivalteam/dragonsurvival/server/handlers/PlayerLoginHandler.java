package by.dragonsurvivalteam.dragonsurvival.server.handlers;

import by.dragonsurvivalteam.dragonsurvival.common.capability.DragonStateProvider;
import by.dragonsurvivalteam.dragonsurvival.config.ServerConfig;
import by.dragonsurvivalteam.dragonsurvival.network.container.AllowOpenDragonAltar;
import by.dragonsurvivalteam.dragonsurvival.network.syncing.SyncComplete;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME)
public class PlayerLoginHandler {

    public static void syncCompleteSingle(Entity tracker, Entity tracked) {
        if(tracker instanceof ServerPlayer){
            if(tracked instanceof ServerPlayer){
                DragonStateProvider.getCap(tracked).ifPresent(dragonStateHandler -> {
                    PacketDistributor.sendToPlayer((ServerPlayer)tracker, new SyncComplete.Data(tracked.getId(), dragonStateHandler.serializeNBT(tracked.registryAccess())));
                });
            }
        }
    }

    public static void syncCompleteSingle(Entity entity) {
        if(entity instanceof ServerPlayer player) {
            DragonStateProvider.getCap(player).ifPresent(dragonStateHandler -> {
                SyncComplete.handleDragonSync(player);
                PacketDistributor.sendToPlayer(player, new SyncComplete.Data(player.getId(), dragonStateHandler.serializeNBT(player.registryAccess())));
            });
        }
    }

    public static void syncCompleteAll(Entity entity) {
        if(entity instanceof ServerPlayer player) {
            DragonStateProvider.getCap(player).ifPresent(dragonStateHandler -> {
                SyncComplete.handleDragonSync(player);
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new SyncComplete.Data(player.getId(), dragonStateHandler.serializeNBT(player.registryAccess())));
            });
        }
    }


    @SubscribeEvent
    public static void onTrackingStart(PlayerEvent.StartTracking startTracking){
        Entity tracker = startTracking.getEntity();
        Entity tracked = startTracking.getTarget();
        syncCompleteSingle(tracker, tracked);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event){
        syncCompleteSingle(event.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event){
        syncCompleteSingle(event.getEntity());
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event){
        syncCompleteSingle(event.getEntity());
    }

    @SubscribeEvent
    public static void startWithDragonChoice(PlayerTickEvent.Post event){
        if(!ServerConfig.startWithDragonChoice) return;
        if(event.getEntity().level().isClientSide()) return;

        if(event.getEntity() instanceof ServerPlayer player){
            if(player.isDeadOrDying()) return;

            if(player.tickCount > 5 * 20){
                DragonStateProvider.getCap(player).ifPresent(cap -> {
                    if(!cap.hasUsedAltar && !cap.isInAltar && !DragonStateProvider.isDragon(player)){
                        PacketDistributor.sendToPlayer(player, new AllowOpenDragonAltar.Data());
                        cap.isInAltar = true;
                    }

                    if(cap.altarCooldown > 0){
                        cap.altarCooldown--;
                    }
                });
            }
        }
    }
}
