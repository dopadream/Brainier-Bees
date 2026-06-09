package com.dopadream.brainierbees.ai.tasks;

import com.dopadream.brainierbees.ai.BeeAi;
import com.dopadream.brainierbees.config.BrainierBeesConfig;
import com.dopadream.brainierbees.registry.ModMemoryTypes;
import com.dopadream.brainierbees.util.HiveAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.util.AirRandomPos;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class GoToHiveTask extends Behavior<Bee> {

    public GoToHiveTask() {
        super(Map.of(ModMemoryTypes.WANTS_HIVE, MemoryStatus.VALUE_PRESENT, ModMemoryTypes.COOLDOWN_LOCATE_HIVE, MemoryStatus.VALUE_ABSENT));
    }


    @Override
    protected boolean canStillUse(ServerLevel level, Bee bee, long l) {
        return ((HiveAccessor) bee).brainier_bees$getMemorizedHome() != null
                && (bee.getBrain().getMemory(ModMemoryTypes.WANTS_HIVE).isPresent()
                && bee.getBrain().getMemory(ModMemoryTypes.WANTS_HIVE).get())
                && !BeeAi.isHiveNearFire(level, bee)
                && checkLeadDist(bee);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Bee bee) {
        return ((HiveAccessor) bee).brainier_bees$getMemorizedHome() != null
                && (bee.getBrain().getMemory(ModMemoryTypes.WANTS_HIVE).isPresent()
                && bee.getBrain().getMemory(ModMemoryTypes.WANTS_HIVE).get())
                && !BeeAi.isHiveNearFire(level, bee)
                && checkLeadDist(bee);
    }

    @Override
    protected void start(ServerLevel serverLevel, Bee bee, long l) {
        super.start(serverLevel, bee, l);
        bee.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
        bee.resetLove();
    }


    @Override
    protected void stop(ServerLevel serverLevel, Bee bee, long l) {
        super.stop(serverLevel, bee, l);
    }

    @Override
    protected void tick(ServerLevel level, Bee bee, long l) {
        var brain = bee.getBrain();

        BlockPos hivePos = ((HiveAccessor) bee).brainier_bees$getMemorizedHome();
        Optional<Integer> travellingTicksOpt = brain.getMemory(ModMemoryTypes.TRAVELLING_TICKS);
        Optional<Integer> stuckTicksOpt = brain.getMemory(ModMemoryTypes.STUCK_TICKS);
        Optional<Path> lastPathOpt = brain.getMemory(ModMemoryTypes.LAST_PATH);

        // Handle the homeless
        if (hivePos == null) {
            brain.setMemory(ModMemoryTypes.TRAVELLING_TICKS, 1);
            dropHive(bee);
            return;
        }

        // Travelling ticks handling
        int travellingTicks = travellingTicksOpt.orElse(0) + 1;
        brain.setMemory(ModMemoryTypes.TRAVELLING_TICKS, travellingTicks);

        int maxTravelTicks = 80 * BrainierBeesConfig.MAX_WANDER_RADIUS;
        if (travellingTicks > maxTravelTicks) {
            dropHive(bee);
            return;
        }

        // Pathfinding
        if (!bee.getNavigation().isInProgress()) {
            if (!pathfindRandomlyTowards(hivePos, bee, 0.6F)) {
                dropHive(bee);
                return;
            }
        }

        // Stuck ticks
        if (lastPathOpt.isPresent() && Objects.requireNonNull(bee.getNavigation().getPath()).sameAs(lastPathOpt.get())) {

            int stuckTicks = stuckTicksOpt.orElse(0) + 1;
            brain.setMemory(ModMemoryTypes.STUCK_TICKS, stuckTicks);

            if (stuckTicks > 600) {
                dropHive(bee);
            }

        } else {
            brain.setMemory(ModMemoryTypes.LAST_PATH, bee.getNavigation().getPath());
        }
    }

    private void dropHive(Bee bee) {
        ((HiveAccessor) bee).dropAndBlacklistHive(bee);
    }

    private boolean checkLeadDist(Bee bee) {
        var canReach = true;
        if (bee.isLeashed() && bee.getLeashData() != null && bee.getLeashData().leashHolder != null) {
            BlockPos leashOrigin = bee.getLeashData().leashHolder.blockPosition();
            if (!leashOrigin.closerThan(((HiveAccessor) bee).brainier_bees$getMemorizedHome(), 5.5)) {
                canReach = false;
            }
        }
        return canReach;
    }


    private boolean pathfindRandomlyTowards(final BlockPos targetPos, Bee bee, float speed) {
        Vec3 targetVec = Vec3.atBottomCenterOf(targetPos);
        int yAdjust = 0;
        BlockPos beePos = bee.blockPosition();
        int yDelta = (int)targetVec.y - beePos.getY();
        if (yDelta > 2) {
            yAdjust = 4;
        } else if (yDelta < -2) {
            yAdjust = -4;
        }

        int xzDist = 6;
        int yDist = 8;
        int dist = beePos.distManhattan(targetPos);
        if (dist < 15) {
            xzDist = dist / 2;
            yDist = dist / 2;
        }

        Vec3 nextPosTowards = AirRandomPos.getPosTowards(bee, xzDist, yDist, yAdjust, targetVec, (double)((float)Math.PI / 10F));
        if (nextPosTowards != null) {
            bee.getNavigation().setMaxVisitedNodesMultiplier(0.5F);
            bee.getNavigation().moveTo(nextPosTowards.x, nextPosTowards.y, nextPosTowards.z, speed);
        }

        return bee.getNavigation().getPath() != null && bee.getNavigation().getPath().canReach();
    }
}
