package com.dopadream.brainierbees.util;

import com.dopadream.brainierbees.BrainierBees;
import com.dopadream.brainierbees.registry.ModMemoryTypes;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.animal.bee.Bee;

import java.util.*;
import java.util.stream.Collectors;

public interface HiveAccessor {


    BlockPos brainier_bees$getMemorizedHome();

    void brainier_bees$setMemorizedHome(BlockPos pos);

    default void dropAndBlacklistHive(Bee bee) {
        if (bee.getBrain().getMemory(ModMemoryTypes.HIVE_POS).isPresent()) {
            ((HiveAccessor)bee).removeMemorizedHive(bee);
        }
        bee.getBrain().setMemory(ModMemoryTypes.COOLDOWN_LOCATE_HIVE, 200);
    }

    default void blacklistTarget(Bee bee, BlockPos blockPos) {
        if (!bee.getBrain().hasMemoryValue(ModMemoryTypes.HIVE_BLACKLIST)) {
            List<GlobalPos> list = new ArrayList<>();
            list.add(GlobalPos.of(bee.level().dimension(), blockPos));
            bee.getBrain().setMemory(ModMemoryTypes.HIVE_BLACKLIST, list.stream().limit(10).collect(Collectors.toList()));
        } else {
            if (!bee.getBrain().getMemory(ModMemoryTypes.HIVE_BLACKLIST).get().contains(GlobalPos.of(bee.level().dimension(), blockPos))) {
                bee.getBrain().getMemory(ModMemoryTypes.HIVE_BLACKLIST).get().add(GlobalPos.of(bee.level().dimension(), blockPos));
            }
            while(bee.getBrain().getMemory(ModMemoryTypes.HIVE_BLACKLIST).get().size() > 9) {
                bee.getBrain().getMemory(ModMemoryTypes.HIVE_BLACKLIST).get().remove(0);
            }
        }

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            if (bee.getBrain().hasMemoryValue(ModMemoryTypes.HIVE_BLACKLIST)) {
                BrainierBees.LOGGER.info((bee.getUUID() + " Hive Blacklist: " + bee.getBrain().getMemory(ModMemoryTypes.HIVE_BLACKLIST).get()));
                BrainierBees.LOGGER.info(bee.getUUID() + " Hive Blacklist Size: " + bee.getBrain().getMemory(ModMemoryTypes.HIVE_BLACKLIST).get().size());
            }
        }
    }

    default void removeMemorizedHive(Bee bee) {
        ((HiveAccessor)bee).blacklistTarget(bee, bee.getBrain().getMemory(ModMemoryTypes.HIVE_POS).get().pos());
        bee.getBrain().eraseMemory(ModMemoryTypes.HIVE_POS);
        bee.getBrain().setMemory(ModMemoryTypes.STUCK_TICKS, 0);
        bee.getBrain().setMemory(ModMemoryTypes.TRAVELLING_TICKS, 0);
        ((HiveAccessor)bee).brainier_bees$setMemorizedHome(null);
    }
}
