package com.example.companionmod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Equipable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Дівчина-компаньйонка (котяче вушко, дзвіночки, добре серце).
 *
 *  ПКМ порожньою рукою        -> поговорити (діалоги залежать від рівня симпатії)
 *  Shift + ПКМ порожньою      -> сісти / іти за гравцем
 *  ПКМ квіткою                -> подарунок (+симпатія)
 *  ПКМ їжею                   -> лікування
 *  ПКМ бронею/зброєю          -> одягає на себе
 *
 *  Сцени: нові рівні симпатії (подарунки), зорі вночі, вогнище, дощ, ніч, поранення.
 */
public class CompanionEntity extends TamableAnimal {
    private int affection = 0;
    private int talkCooldown = 0;
    private int ambientCooldown = 600;
    private long nextTalkBonus = 0L;

    public CompanionEntity(EntityType<? extends CompanionEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.1D, 8.0F, 2.0F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (this.isOwnedBy(target)) {
            return false;
        }
        return super.canAttack(target);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    // ================= Тік: фонові репліки і сцени =================

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (this.talkCooldown > 0) {
            this.talkCooldown--;
        }
        if (this.tickCount % 20 == 0) {
            ambient();
        }
    }

    private void ambient() {
        if (!this.isTame() || !(this.getOwner() instanceof Player owner)) {
            return;
        }
        if (this.distanceToSqr(owner) > 24.0 * 24.0) {
            return;
        }
        if (this.ambientCooldown > 0) {
            this.ambientCooldown -= 20;
            return;
        }

        Level lv = this.level();
        int lvl = getLoveLevel();
        boolean sky = lv.canSeeSky(this.blockPosition());

        if (this.getHealth() < this.getMaxHealth() * 0.3F) {
            say(owner, "ambient.companionmod.hurt", 2);
            this.ambientCooldown = 1200;
            return;
        }
        if (lvl >= 1 && lv.isNight() && nearLitCampfire()) {
            scene(owner, "ambient.companionmod.campfire", 3);
            hearts(8);
            this.playSound(SoundEvents.CAT_PURR, 0.8F, 1.3F);
            this.ambientCooldown = 6000;
            return;
        }
        if (lvl >= 1 && lv.isNight() && sky && this.isInSittingPose()) {
            scene(owner, "ambient.companionmod.stars", 3);
            hearts(8);
            this.ambientCooldown = 6000;
            return;
        }
        if (lv.isRaining() && sky) {
            say(owner, "ambient.companionmod.rain", 3);
            this.ambientCooldown = 3000;
            return;
        }
        if (lv.isNight() && this.random.nextInt(4) == 0) {
            say(owner, "ambient.companionmod.night", 3);
            this.ambientCooldown = 3000;
        }
    }

    private boolean nearLitCampfire() {
        BlockPos c = this.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-4, -1, -4), c.offset(4, 1, 4))) {
            BlockState s = this.level().getBlockState(p);
            if (s.is(BlockTags.CAMPFIRES) && s.hasProperty(CampfireBlock.LIT) && s.getValue(CampfireBlock.LIT)) {
                return true;
            }
        }
        return false;
    }

    // ================= Діалоги =================

    private void say(Player player, String keyPrefix, int count) {
        Component line = Component.translatable(keyPrefix + "." + this.random.nextInt(count));
        player.sendSystemMessage(Component.translatable("message.companionmod.say", this.getDisplayName(), line));
    }

    private void scene(Player player, String keyPrefix, int count) {
        for (int i = 0; i < count; i++) {
            Component line = Component.translatable(keyPrefix + "." + i);
            player.sendSystemMessage(Component.translatable("message.companionmod.say", this.getDisplayName(), line));
        }
    }

    private void talk(Player player) {
        if (this.talkCooldown > 0) {
            player.displayClientMessage(Component.translatable("message.companionmod.busy"), true);
            return;
        }
        this.talkCooldown = 40;
        say(player, "dialogue.companionmod.t" + getLoveLevel(), 5);
        this.playSound(SoundEvents.CAT_PURR, 0.6F, 1.4F);
        if (this.level().getGameTime() >= this.nextTalkBonus) {
            this.nextTalkBonus = this.level().getGameTime() + 1200L;
            hearts(3);
            addAffection(player, 1);
        }
    }

    // ================= Симпатія =================

    /** 0: незнайомка (0-24), 1: друг (25-74), 2: близька (75-149), 3: найкраща (150+) */
    public int getLoveLevel() {
        if (this.affection >= 150) return 3;
        if (this.affection >= 75) return 2;
        if (this.affection >= 25) return 1;
        return 0;
    }

    public int getAffection() {
        return this.affection;
    }

    private void addAffection(Player player, int amount) {
        int before = getLoveLevel();
        this.affection += amount;
        int after = getLoveLevel();
        for (int l = before + 1; l <= after; l++) {
            levelUpScene(player, l);
        }
    }

    private void levelUpScene(Player player, int level) {
        scene(player, "scene.companionmod.l" + level, 3);
        hearts(16);
        this.playSound(SoundEvents.CAT_PURR, 1.0F, 1.2F);
        ItemStack reward = switch (level) {
            case 1 -> new ItemStack(Items.COOKIE, 8);
            case 2 -> new ItemStack(Items.GOLDEN_APPLE, 2);
            default -> new ItemStack(Items.DIAMOND, 3);
        };
        if (!player.getInventory().add(reward)) {
            player.drop(reward, false);
        }
        player.displayClientMessage(Component.translatable("message.companionmod.reward"), true);
    }

    private void hearts(int count) {
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HEART,
                    this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0),
                    count, 0.3, 0.3, 0.3, 0.02);
        }
    }

    // ================= Взаємодія =================

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.isTame() || !this.isOwnedBy(player)) {
            return InteractionResult.PASS;
        }
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = player.getItemInHand(hand);

        if (stack.isEmpty()) {
            if (player.isShiftKeyDown()) {
                boolean sit = !this.isOrderedToSit();
                this.setOrderedToSit(sit);
                this.setInSittingPose(sit);
                this.jumping = false;
                this.navigation.stop();
                this.setTarget(null);
                player.displayClientMessage(Component.translatable(
                        sit ? "message.companionmod.sit" : "message.companionmod.follow"), true);
            } else {
                talk(player);
            }
            return InteractionResult.SUCCESS;
        }

        if (stack.is(ItemTags.FLOWERS)) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            hearts(6);
            this.playSound(SoundEvents.CAT_PURR, 1.0F, 1.5F);
            player.displayClientMessage(Component.translatable("message.companionmod.gift"), true);
            addAffection(player, 5);
            return InteractionResult.SUCCESS;
        }

        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food != null && this.getHealth() < this.getMaxHealth()) {
            this.heal(Math.max(2, food.nutrition()));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            this.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
            hearts(3);
            return InteractionResult.SUCCESS;
        }

        if (tryEquip(player, stack)) {
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private boolean tryEquip(Player player, ItemStack stack) {
        EquipmentSlot slot = null;
        Equipable equipable = Equipable.get(stack);
        if (equipable != null) {
            slot = equipable.getEquipmentSlot();
        } else if (stack.getItem() instanceof ShieldItem) {
            slot = EquipmentSlot.OFFHAND;
        } else if (stack.getItem() instanceof TieredItem
                || stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem) {
            slot = EquipmentSlot.MAINHAND;
        }
        if (slot == null) {
            return false;
        }

        ItemStack old = this.getItemBySlot(slot);
        this.setItemSlot(slot, stack.copyWithCount(1));
        this.setDropChance(slot, 1.0F);
        this.setPersistenceRequired();
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (!old.isEmpty() && !player.getInventory().add(old)) {
            player.drop(old, false);
        }
        this.playSound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1.0F, 1.0F);
        return true;
    }

    // ================= Збереження =================

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Affection", this.affection);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.affection = tag.getInt("Affection");
    }
}
