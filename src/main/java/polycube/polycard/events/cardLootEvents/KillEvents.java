package polycube.polycard.events.cardLootEvents;

import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.zombie.*;
import polycube.polycard.card.CardType;
import polycube.polycard.events.callBacks.KillEventCallback;
import polycube.polycard.utils.CardHelper;

/// Awards cards from player kills while excluding mob variants that should not count.
public class KillEvents {
    public static void register() {
        KillEventCallback.EVENT.register((player, entity, killingBlow) -> {
            switch (entity) {
                case ZombieVillager _, Husk _, Drowned _ -> { }
                case EnderMan _ -> CardHelper.receiveCard(player, CardType.ENDERMAN);
                case Squid _ -> CardHelper.receiveCard(player, CardType.SQUID);
                case Piglin _ -> CardHelper.receiveCard(player, CardType.PIGLIN);
                case ZombifiedPiglin _ -> CardHelper.receiveCard(player, CardType.ZOMBIFIED_PIGLIN);
                case Zombie _ -> CardHelper.receiveCard(player, CardType.ZOMBIE);
                case Bat _ -> CardHelper.receiveCard(player, CardType.BAT);
                case Creeper _ -> CardHelper.receiveCard(player, CardType.CREEPER);
                default -> { }
            }
        });
    }
}
