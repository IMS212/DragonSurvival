package by.dragonsurvivalteam.dragonsurvival.mixins;

import by.dragonsurvivalteam.dragonsurvival.DragonSurvivalMod;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Set;

@Mixin(Item.class)
public class ItemMixin
{
    @Inject(method = "appendHoverText", at = @At("HEAD"))
    private void appendEnchantmentDescriptionToDSEnchantments(ItemStack pStack, Item.TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag, CallbackInfo ci)
    {
        if (pStack.getItem() instanceof EnchantedBookItem enchantedBookItem)
        {
            ItemEnchantments enchantments = pStack.get(DataComponents.STORED_ENCHANTMENTS);
            if(enchantments != null) {
                // Do not write the tooltip unless we have only one enchantment so that we don't cause confusion by writing the enchantment description on the tooltip of a book with multiple enchantments
                if(enchantments.size() == 1) {
                    enchantments.keySet().forEach(enchantment -> {
                        if(pContext.registries().lookup(Registries.ENCHANTMENT).get().getOrThrow(enchantment.getKey()).getKey().location().getNamespace().equals(DragonSurvivalMod.MODID)) {
                            // Remove the "dragonsurvival:" prefix and add the "ds.description." prefix to the registered name
                            pTooltipComponents.add(Component.translatable("ds.description." + enchantment.getRegisteredName().substring(DragonSurvivalMod.MODID.length() + 1)));
                        }
                    });
                }
            }
        }
    }
}
