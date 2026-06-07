package com.dopadream.brainierbees.ai.tasks;

import com.dopadream.brainierbees.registry.ModMemoryTypes;
import com.dopadream.brainierbees.util.HiveAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;

public class EnterHiveTask extends Behavior<Bee> {

    public EnterHiveTask() {
        super(Map.of(ModMemoryTypes.WANTS_HIVE, MemoryStatus.VALUE_PRESENT));
    }


    @Override
    protected boolean checkExtraStartConditions(ServerLevel serverLevel, Bee bee) {
        var canEnter = false;
        if (((HiveAccessor) bee).brainier_bees$getMemorizedHome() != null && bee.getBrain().getMemory(ModMemoryTypes.WANTS_HIVE).isPresent() && ((HiveAccessor) bee).brainier_bees$getMemorizedHome().closerToCenterThan(bee.position(), 2.0)) {
            BlockEntity blockEntity = serverLevel.getBlockEntity(((HiveAccessor) bee).brainier_bees$getMemorizedHome());
            if (blockEntity instanceof BeehiveBlockEntity beehiveBlockEntity) {
                if (!beehiveBlockEntity.isFull()) {
                    canEnter = true;
                } else {
                    dropHive(bee);
                }
            }
        }
        return canEnter;
    }

    @Override
    protected void start(ServerLevel serverLevel, Bee bee, long l) {
        if (((HiveAccessor) bee).brainier_bees$getMemorizedHome() != null) {
            BlockEntity blockEntity = serverLevel.getBlockEntity(((HiveAccessor) bee).brainier_bees$getMemorizedHome());
            if (blockEntity instanceof BeehiveBlockEntity beehiveBlockEntity) {
                bee.getBrain().setMemory(ModMemoryTypes.STUCK_TICKS, 0);
                beehiveBlockEntity.addOccupant(bee);
            }
        }
        super.start(serverLevel, bee, l);
    }

    private void dropHive(Bee bee) {
        ((HiveAccessor) bee).dropAndBlacklistHive(bee);
    }
}
