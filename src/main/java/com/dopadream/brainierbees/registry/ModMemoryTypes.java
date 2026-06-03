package com.dopadream.brainierbees.registry;

import com.mojang.serialization.Codec;
import com.dopadream.brainierbees.BrainierBees;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.pathfinder.Path;

import java.util.List;
import java.util.Optional;

public class ModMemoryTypes {

    public static final MemoryModuleType<GlobalPos> FLOWER_POS = register("flower_pos", GlobalPos.CODEC);
    public static final MemoryModuleType<GlobalPos> HIVE_POS = register("hive_pos", GlobalPos.CODEC);
    public static final MemoryModuleType<Path> LAST_PATH = register("last_path");

    public static final MemoryModuleType<List<GlobalPos>> HIVE_BLACKLIST = register("hive_blacklist");

    public static final MemoryModuleType<Integer> POLLINATING_COOLDOWN = register("pollinating_cooldown", Codec.INT);
    public static final MemoryModuleType<Integer> POLLINATING_TICKS = register("pollinating_ticks", Codec.INT);
    public static final MemoryModuleType<Integer> SUCCESSFUL_POLLINATING_TICKS = register("successful_pollinating_ticks", Codec.INT);
    public static final MemoryModuleType<Integer> COOLDOWN_LOCATE_HIVE = register("cooldown_locate_hive", Codec.INT);
    public static final MemoryModuleType<Integer> TRAVELLING_TICKS = register("travelling_ticks", Codec.INT);
    public static final MemoryModuleType<Integer> SEARCH_ATTEMPTS = register("search_attempts", Codec.INT);
    public static final MemoryModuleType<Integer> STUCK_TICKS = register("stuck_ticks", Codec.INT);

    public static final MemoryModuleType<Boolean> WANTS_HIVE = register("wants_hive", Codec.BOOL);


    public static <U> MemoryModuleType<U> register(String id, Codec<U> codec) {
        return Registry.register(BuiltInRegistries.MEMORY_MODULE_TYPE, Identifier.fromNamespaceAndPath(BrainierBees.MOD_ID, id), new MemoryModuleType<>(Optional.of(codec)));
    }

    public static <U> MemoryModuleType<U> register(String id) {
        return Registry.register(BuiltInRegistries.MEMORY_MODULE_TYPE, Identifier.fromNamespaceAndPath(BrainierBees.MOD_ID, id), new MemoryModuleType<>(Optional.empty()));
    }

    public static void init() {
    }
}
