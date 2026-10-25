package com.example.companionmod;

import com.example.companionmod.entity.ModEntities;
import com.example.companionmod.item.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(CompanionMod.MOD_ID)
public class CompanionMod {
    public static final String MOD_ID = "companionmod";

    public CompanionMod(IEventBus modBus) {
        ModEntities.ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
    }
}
