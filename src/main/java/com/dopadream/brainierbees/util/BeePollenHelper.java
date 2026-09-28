package com.dopadream.brainierbees.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

public class BeePollenHelper {

    private static final Map<Block, GrowthHandler> growthHandlers = new HashMap<>();

    private static BlockState growBonemealable(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        BonemealableBlock block = (BonemealableBlock) state.getBlock();
        if (block.isValidBonemealTarget(level, pos, state, BonemealSource.INTERACTION)) {
            block.performBonemeal(level, random, pos, state, BonemealSource.INTERACTION);
            return level.getBlockState(pos);
        }
        if (block.isValidBonemealTarget(level, pos, state, BonemealSource.MOB)) {
            block.performBonemeal(level, random, pos, state, BonemealSource.MOB);
            return level.getBlockState(pos);
        }
        return null;
    }

    public static BlockState tryGrow(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {

        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(state) ? null : crop.getStateForAge(crop.getAge(state) + 1);
        } else if (state.getBlock() instanceof StemBlock) {
            int j = state.getValue(StemBlock.AGE);
            return (j < 7) ? state.setValue(StemBlock.AGE, j + 1) : null;
        }

        GrowthHandler handler = growthHandlers.get(state.getBlock());
        return handler != null ? handler.apply(level, pos, state, random) : null;
    }

    @FunctionalInterface
    interface GrowthHandler {
        BlockState apply(ServerLevel level, BlockPos pos, BlockState state, RandomSource random);
    }


    static {
        growthHandlers.put(Blocks.SWEET_BERRY_BUSH, (level, pos, state, random) -> {
            int age = state.getValue(SweetBerryBushBlock.AGE);
            return age < 3 ? state.setValue(SweetBerryBushBlock.AGE, age + 1) : null;
        });

        growthHandlers.put(Blocks.CAVE_VINES, BeePollenHelper::growBonemealable);
        growthHandlers.put(Blocks.CAVE_VINES_PLANT, BeePollenHelper::growBonemealable);

    }
}
