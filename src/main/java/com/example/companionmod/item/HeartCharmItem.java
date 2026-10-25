package com.example.companionmod.item;

import com.example.companionmod.entity.CompanionEntity;
import com.example.companionmod.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** ПКМ по блоку — призиває компаньйонку, яка одразу стає твоєю. */
public class HeartCharmItem extends Item {
    public HeartCharmItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        CompanionEntity companion = ModEntities.COMPANION.get().create(server);
        if (companion == null) {
            return InteractionResult.FAIL;
        }

        companion.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, context.getRotation(), 0.0F);
        companion.setPersistenceRequired();
        Player player = context.getPlayer();
        if (player != null) {
            companion.tame(player);
        }
        server.addFreshEntity(companion);
        context.getItemInHand().consume(1, player);
        return InteractionResult.CONSUME;
    }
}
