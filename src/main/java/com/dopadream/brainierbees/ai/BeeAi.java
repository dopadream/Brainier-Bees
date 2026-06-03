package com.dopadream.brainierbees.ai;

import com.dopadream.brainierbees.ai.tasks.*;
import com.dopadream.brainierbees.registry.ModMemoryTypes;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import java.util.function.Predicate;

public class BeeAi {
    private static final UniformInt TIME_BETWEEN_POLLINATING = UniformInt.of(10, 15);
    private static final UniformInt ADULT_FOLLOW_RANGE = UniformInt.of(3, 16);

    public static void initMemories(Bee bee, RandomSource randomSource) {
        bee.getBrain().setMemory(ModMemoryTypes.POLLINATING_COOLDOWN, TIME_BETWEEN_POLLINATING.sample(randomSource));
    }

    private static ActivityData<Bee> initStingActivity() {
        return ActivityData.create(
                Activity.FIGHT,
                0,
                ImmutableList.of(
                        StopAttackingIfTargetInvalid.create(),
                        SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1F),
                        MeleeAttack.create(20),
                        EraseMemoryIf.create(Predicate.not(Bee::isAngry), MemoryModuleType.ATTACK_TARGET),
                        EraseMemoryIf.create(Bee::hasStung, MemoryModuleType.ATTACK_TARGET)
                ),
                MemoryModuleType.ATTACK_TARGET
        );
    }



    private static ActivityData<Bee> initPollinateActivity() {
        return ActivityData.create(
                Activity.CELEBRATE,
                ImmutableList.of(
                        Pair.of(0, BabyFollowAdult.create(ADULT_FOLLOW_RANGE, 1.25F)),
                        Pair.of(1, new FindFlowerTask()),
                        Pair.of(2, new PollinateFlowerTask()),
                        Pair.of(3, EraseMemoryIf.create(Bee::hasNectar, ModMemoryTypes.FLOWER_POS)),
                        Pair.of(4, EraseMemoryIf.create(
                                (bee) -> bee.getBrain().hasMemoryValue(ModMemoryTypes.WANTS_HIVE),
                                ModMemoryTypes.FLOWER_POS
                        ))
                ),
                ImmutableSet.of(
                        Pair.of(MemoryModuleType.TEMPTING_PLAYER, MemoryStatus.VALUE_ABSENT),
                        Pair.of(MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT),
                        Pair.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT),
                        Pair.of(ModMemoryTypes.WANTS_HIVE, MemoryStatus.VALUE_ABSENT),
                        Pair.of(ModMemoryTypes.POLLINATING_COOLDOWN, MemoryStatus.VALUE_ABSENT)
                )
        );
    }

    private static ActivityData<Bee> initCoreActivity() {
        return ActivityData.create(Activity.CORE, 0, ImmutableList.of(
                new MoveToTargetSink(),
                new CountDownCooldownTicks(ModMemoryTypes.POLLINATING_COOLDOWN),
                new CountDownCooldownTicks(ModMemoryTypes.COOLDOWN_LOCATE_HIVE),
                new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS)));

    }


    private static ActivityData<Bee> initIdleActivity() {
        return ActivityData.create(
                Activity.IDLE,
                ImmutableList.of(
                        Pair.of(0, new FloatTask()),
                        Pair.of(1, new GoToHiveTask()),
                        Pair.of(2, new EnterHiveTask()),
                        Pair.of(3, new FollowTemptation(livingEntity -> 0.6F)),
                        Pair.of(4, BabyFollowAdult.create(ADULT_FOLLOW_RANGE, 1.25F)),
                        Pair.of(5, new AnimalMakeLove(EntityTypes.BEE)),
                        Pair.of(6, new LocateHiveTask()),
                        Pair.of(7, new GrowCropTask()),
                        Pair.of(
                                8,
                                new RunOne(
                                        ImmutableList.of(
                                                Pair.of(new BeePathfinding(), 1))
                                )
                        )
                ),
                ImmutableSet.of(Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT))
        );
    }

    public static Predicate<ItemStack> getTemptations() {
        return itemStack -> itemStack.is(ItemTags.BEE_FOOD);
    }


    public static boolean isHiveNearFire(ServerLevel level, Bee bee) {
        if (bee.getBrain().getMemory(ModMemoryTypes.HIVE_POS).isEmpty()) {
            return false;
        } else {
            BlockEntity blockEntity = level.getBlockEntity(bee.getBrain().getMemory(ModMemoryTypes.HIVE_POS).get().pos());
            return blockEntity instanceof BeehiveBlockEntity && ((BeehiveBlockEntity)blockEntity).isFireNearby();
        }
    }


    public static boolean isPollinating(Bee bee) {
        boolean pollinating = bee.getBrain().hasMemoryValue(ModMemoryTypes.SUCCESSFUL_POLLINATING_TICKS);

        if (pollinating && bee.getBrain().getMemory(ModMemoryTypes.SUCCESSFUL_POLLINATING_TICKS).get() == 0) {
            pollinating = false;
        }

        return pollinating;
    }

    public static List<ActivityData<Bee>> getActivities() {
        return List.of(initCoreActivity(), initIdleActivity(), initStingActivity(), initPollinateActivity());
    }

    public static void incrementMemory(Brain<?> brain, MemoryModuleType<Integer> type) {
        int newValue = brain.getMemory(type).orElse(0) + 1;
        brain.setMemory(type, newValue);
    }

    public static void updateActivity(Bee bee) {
        bee.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.FIGHT,  Activity.CELEBRATE, Activity.IDLE));
    }

}
