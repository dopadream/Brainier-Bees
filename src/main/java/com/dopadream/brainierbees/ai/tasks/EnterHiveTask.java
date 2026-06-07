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
        super(Map.of(ModMemoryTypes.HIVE_POS, MemoryStatus.VALUE_PRESENT));
    }


    @Override
    protected boolean checkExtraStartConditions(ServerLevel serverLevel, Bee bee) {
        var canEnter = false;
        if (bee.getBrain().getMemory(ModMemoryTypes.HIVE_POS).isPresent() && bee.getBrain().getMemory(ModMemoryTypes.WANTS_HIVE).isPresent() && bee.getBrain().getMemory(ModMemoryTypes.HIVE_POS).get().pos().closerToCenterThan(bee.position(), 2.0)) {
            BlockEntity blockEntity = serverLevel.getBlockEntity(bee.getBrain().getMemory(ModMemoryTypes.HIVE_POS).get().pos());
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
        if (bee.getBrain().getMemory(ModMemoryTypes.HIVE_POS).isPresent()) {
            BlockEntity blockEntity = serverLevel.getBlockEntity(bee.getBrain().getMemory(ModMemoryTypes.HIVE_POS).get().pos());
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
