package com.dopadream.brainierbees;

import com.dopadream.brainierbees.config.BrainierBeesConfig;
import com.dopadream.brainierbees.registry.ModMemoryTypes;
import com.dopadream.brainierbees.registry.ModSensorTypes;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrainierBees implements ModInitializer {

    public static String MOD_ID = "brainierbees";
    public static final Logger LOGGER = LoggerFactory.getLogger(BrainierBees.MOD_ID);


    @Override
    public void onInitialize() {
        BrainierBeesConfig.init();
        ModMemoryTypes.init();
        ModSensorTypes.init();
    }
}
