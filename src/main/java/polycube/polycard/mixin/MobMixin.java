package polycube.polycard.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import polycube.polycard.PolyCard;
import polycube.polycard.card.CardType;
import polycube.polycard.card.RarityLevel;

@Mixin(Mob.class)
public abstract class MobMixin extends LivingEntity implements Targeting, EquipmentUser, Leashable {
    protected MobMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @WrapMethod(method = "asValidTarget")
    public LivingEntity onTrigger(LivingEntity target, Operation<LivingEntity> original) {
        LivingEntity validTarget = original.call(target);
        if (validTarget != null) {
            if (validTarget instanceof Player player) {
                player.sendSystemMessage(Component.literal(this.toString() + " is targeting you!"));
                return null;
//                if (PolyCard.cardManager.getStorage().data(player).hasCardOrRarer(CardType.PIGLIN, RarityLevel.RARE)) {
//                    return null;
//                }
            }
        }
        return validTarget;
    }
}
