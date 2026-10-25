package com.example.companionmod.entity;

import com.example.companionmod.CompanionMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, CompanionMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<CompanionEntity>> COMPANION =
            ENTITIES.register("companion", () ->
                    EntityType.Builder.of(CompanionEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(10)
                            .build("companion"));
}
