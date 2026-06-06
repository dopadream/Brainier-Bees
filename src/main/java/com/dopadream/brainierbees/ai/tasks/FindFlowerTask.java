package com.dopadream.brainierbees.ai.tasks;

import com.dopadream.brainierbees.BrainierBees;
import com.dopadream.brainierbees.ai.BeeAi;
import com.dopadream.brainierbees.config.BrainierBeesConfig;
import com.dopadream.brainierbees.registry.ModMemoryTypes;
import com.google.common.collect.Lists;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.pathfinder.Path;

import java.util.List;
import java.util.Map;

public class FindFlowerTask extends Behavior<Bee> {
    private BlockPos flowerPosPublic = null;


    public FindFlowerTask() {
        super(Map.of(ModMemoryTypes.POLLINATING_COOLDOWN, MemoryStatus.VALUE_ABSENT));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, Bee bee) {
        return !bee.hasNectar() && (bee.getBrain().getMemory(ModMemoryTypes.FLOWER_POS).isEmpty() && bee.getBrain().getMemory(ModMemoryTypes.POLLINATING_COOLDOWN).isEmpty()) && !(bee.getBrain().getMemory(ModMemoryTypes.WANTS_HIVE).isPresent() && bee.getBrain().getMemory(ModMemoryTypes.WANTS_HIVE).get());
    }

    @Override
    protected boolean canStillUse(ServerLevel world, Bee bee, long l) {
        return !bee.hasNectar() && (bee.getBrain().getMemory(ModMemoryTypes.FLOWER_POS).isEmpty() && bee.getBrain().getMemory(ModMemoryTypes.POLLINATING_COOLDOWN).isEmpty()) && !(bee.getBrain().getMemory(ModMemoryTypes.WANTS_HIVE).isPresent() && bee.getBrain().getMemory(ModMemoryTypes.WANTS_HIVE).get());
    }

    public BlockPos getFlowerPos(Bee bee, ServerLevel level) {
        int radius = BrainierBeesConfig.FLOWER_LOCATE_RANGE;
        if (bee.isLeashed()) {
            radius = 5;
        }

        List<BlockPos> possibles = Lists.newArrayList();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -radius; y <= radius; y++) {
                    BlockPos pos;

                    if (bee.isLeashed() && bee.getLeashData() != null && bee.getLeashData().leashHolder != null) {
                        pos = new BlockPos(bee.getLeashData().leashHolder.blockPosition().getX() + x, bee.getLeashData().leashHolder.blockPosition().getY() + y, bee.getLeashData().leashHolder.blockPosition().getZ() + z);
                    } else {
                        pos = new BlockPos(bee.getBlockX() + x, bee.getBlockY() + y, bee.getBlockZ() + z);
                    }

                    if (Bee.attractsBees(level.getBlockState(pos))) {
                        possibles.add(pos);
                    }
                }
            }
        }
        if (possibles.isEmpty()) {
            bee.getBrain().setMemory(ModMemoryTypes.POLLINATING_COOLDOWN, UniformInt.of(120, 240).sample(level.getRandom()));
            BeeAi.incrementMemory(bee.getBrain(), ModMemoryTypes.SEARCH_ATTEMPTS);
            return null;
        } else {
            bee.getBrain().eraseMemory(ModMemoryTypes.SEARCH_ATTEMPTS);
            return possibles.get(bee.getRandom().nextInt(possibles.size()));
        }
    }

    @Override
    protected void start(ServerLevel level, Bee bee, long l) {
        BlockPos flowerPos = null;

        if (this.flowerPosPublic == null)
            flowerPos = this.getFlowerPos(bee, level);

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            if (flowerPos != null) {
                BrainierBees.LOGGER.info((bee.getUUID() + " Flower Selected: " + flowerPos));
            }
            if (bee.getBrain().getMemory(ModMemoryTypes.SEARCH_ATTEMPTS).isPresent())
                BrainierBees.LOGGER.info((bee.getUUID() + " Search Attempts: " + bee.getBrain().getMemory(ModMemoryTypes.SEARCH_ATTEMPTS).get()));
        }

        if (flowerPos != null && bee.getBrain().getMemory(ModMemoryTypes.FLOWER_POS).isEmpty()) {
            this.flowerPosPublic = flowerPos;
        }
    }

    @Override
    protected void tick(ServerLevel level, Bee bee, long l) {
        if (this.flowerPosPublic != null) {
            BlockPos flowerPos = this.flowerPosPublic;
            BehaviorUtils.setWalkAndLookTargetMemories(bee, flowerPos, 0.4F, 1);
            Path flower = bee.getNavigation().createPath(flowerPos, 1);
            if (flower != null && flower.canReach() && Bee.attractsBees(level.getBlockState(flowerPos))) {

                bee.getNavigation().moveTo(flower, 0.6);

                if (bee.blockPosition().closerThan(flowerPos, 2)) {
                    bee.getBrain().setMemory(ModMemoryTypes.FLOWER_POS, GlobalPos.of(level.dimension(), flowerPos));
                }

                if (bee.isLeashed() && bee.getLeashData() != null && bee.getLeashData().leashHolder != null) {
                    BlockPos leashOrigin = bee.getLeashData().leashHolder.blockPosition();
                    if (!leashOrigin.closerThan(flowerPos, 5.5) || !Bee.attractsBees(level.getBlockState(flowerPos))) {
                        bee.getBrain().eraseMemory(ModMemoryTypes.FLOWER_POS);
                        this.flowerPosPublic = null;
                        bee.getBrain().setMemory(ModMemoryTypes.POLLINATING_COOLDOWN, UniformInt.of(120, 240).sample(level.getRandom()));
                    }
                }

            } else {
                bee.getBrain().eraseMemory(ModMemoryTypes.FLOWER_POS);
                this.flowerPosPublic = null;
                bee.getBrain().setMemory(ModMemoryTypes.POLLINATING_COOLDOWN, UniformInt.of(120, 240).sample(level.getRandom()));
            }

        }
    }

    @Override
    protected void stop(ServerLevel serverLevel, Bee bee, long l) {
        super.stop(serverLevel, bee, l);
        if (bee.getBrain().getMemory(ModMemoryTypes.POLLINATING_COOLDOWN).isEmpty() && bee.hasNectar()) {
            bee.getBrain().setMemory(ModMemoryTypes.POLLINATING_COOLDOWN, 100);
        }
    }
}
