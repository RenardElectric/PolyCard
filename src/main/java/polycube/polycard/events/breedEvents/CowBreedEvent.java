package polycube.polycard.events.breedEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import polycube.polycard.PolyCard;
import polycube.polycard.card.Card;
import polycube.polycard.card.CardType;
import polycube.polycard.manager.CardManager;

import java.util.Optional;

public class CowBreedEvent implements BreedEvent {
    @Override
    public InteractionResult handle(ServerPlayer player, Animal parent, Animal partner, Optional<AgeableMob> child) {
        if (parent.getType() == EntityType.COW) {
            Card card = CardManager.createCard(CardType.COW);
            player.getInventory().add(card.getItem());
            PolyCard.LOGGER.debug("[Polycard] {} bred a cow and received a card: {}", player.getName(), card.getDisplayName());
            player.sendSystemMessage(Component.literal("✨ You found a ").append(card.getFormatedName()).append(" card!").withStyle(ChatFormatting.GREEN));
        }
        return InteractionResult.PASS;
    }
}
