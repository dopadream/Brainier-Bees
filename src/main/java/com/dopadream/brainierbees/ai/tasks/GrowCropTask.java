package com.dopadream.brainierbees.ai.tasks;

import com.dopadream.brainierbees.mixin.BeeAccessor;
import com.dopadream.brainierbees.util.BeePollenHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public class GrowCropTask extends Behavior<Bee> {

    public GrowCropTask() {
        super(Map.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT));
    }

    public boolean canBeeUse(ServerLevel level, Bee bee) {
        if (((BeeAccessor) bee).invokeGetCropsGrownSincePollination() >= 10) {
            ((BeeAccessor)bee).invokeSetHasNectar(false);
            ((BeeAccessor)bee).invokeResetNumCropsGrownSincePollination();
            return false;
        } else if (level.getRandom().nextFloat() < 0.3F) {
            return false;
        } else {
            return bee.hasNectar();
        }
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel serverLevel, Bee bee) {
        return canBeeUse(serverLevel, bee);
    }

    @Override
    protected boolean canStillUse(ServerLevel serverLevel, Bee bee, long l) {
        return canBeeUse(serverLevel, bee);
    }

    @Override
    protected void tick(ServerLevel level, Bee bee, long l) {
        if (bee.getRandom().nextInt(1, 31) == 1) {
            for (int i = 1; i <= 2; ++i) {
                BlockPos blockPos = bee.blockPosition().below(i);
                BlockState blockState = bee.level().getBlockState(blockPos);

                if (blockState.is(BlockTags.BEE_GROWABLES)) {
                    BlockState newState = BeePollenHelper.tryGrow(level, blockPos, blockState, bee.getRandom());
                    if (newState != null) {
                        bee.level().levelEvent(2011, blockPos, 15);
                        bee.level().setBlockAndUpdate(blockPos, newState);
                        ((BeeAccessor) bee).invokeIncrementNumCropsGrownSincePollination();
                    }
                }
            }
        }
    }
}
