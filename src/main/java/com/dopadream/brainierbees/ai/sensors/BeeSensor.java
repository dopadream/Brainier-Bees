package com.dopadream.brainierbees.ai.sensors;

import com.dopadream.brainierbees.registry.ModMemoryTypes;
import com.google.common.collect.ImmutableSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import org.jspecify.annotations.NonNull;

import java.util.Set;

public class BeeSensor extends Sensor<LivingEntity> {
    @Override
    protected void doTick(ServerLevel level, LivingEntity body) {
    }

    @Override
    public @NonNull Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(
                ModMemoryTypes.FLOWER_POS,
                ModMemoryTypes.HIVE_POS,
                ModMemoryTypes.LAST_PATH,
                ModMemoryTypes.HIVE_BLACKLIST,
                ModMemoryTypes.POLLINATING_COOLDOWN,
                ModMemoryTypes.POLLINATING_TICKS,
                ModMemoryTypes.SUCCESSFUL_POLLINATING_TICKS,
                ModMemoryTypes.COOLDOWN_LOCATE_HIVE,
                ModMemoryTypes.TRAVELLING_TICKS,
                ModMemoryTypes.SEARCH_ATTEMPTS,
                ModMemoryTypes.STUCK_TICKS,
                ModMemoryTypes.WANTS_HIVE);
    }
}
