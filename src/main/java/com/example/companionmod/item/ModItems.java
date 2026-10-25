package com.example.companionmod.item;

import com.example.companionmod.CompanionMod;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CompanionMod.MOD_ID);

    public static final DeferredItem<Item> HEART_CHARM =
            ITEMS.register("heart_charm", () -> new HeartCharmItem(new Item.Properties().stacksTo(16)));
}
