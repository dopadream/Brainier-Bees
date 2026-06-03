package com.dopadream.brainierbees.registry;

import com.dopadream.brainierbees.BrainierBees;
import com.dopadream.brainierbees.ai.BeeAi;
import com.dopadream.brainierbees.ai.sensors.BeeSensor;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.ai.sensing.TemptingSensor;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ModSensorTypes {

    public static final SensorType<TemptingSensor> BEE_TEMPTATIONS = registerSensorType("bee_temptations", () -> new TemptingSensor(BeeAi.getTemptations()));

    public static final SensorType<BeeSensor> BEE_MEMORIES = registerSensorType("bee_memories", BeeSensor::new);

    @NotNull
    private static <U extends Sensor<?>> SensorType<U> registerSensorType(String name, Supplier<U> supplier) {
        return Registry.register(BuiltInRegistries.SENSOR_TYPE, Identifier.fromNamespaceAndPath(BrainierBees.MOD_ID, name), new SensorType<>(supplier));
    }

    public static void init() {
    }
}
