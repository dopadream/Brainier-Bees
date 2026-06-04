package com.dopadream.brainierbees.mixin;


import com.dopadream.brainierbees.ai.BeeAi;
import com.dopadream.brainierbees.config.BrainierBeesConfig;
import com.dopadream.brainierbees.registry.ModMemoryTypes;
import com.dopadream.brainierbees.registry.ModSensorTypes;
import com.dopadream.brainierbees.util.HiveAccessor;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Bee.class)
public abstract class BeeMixin extends Animal implements HiveAccessor {

    @Unique
    private static final Brain.Provider<Bee> BRAIN_PROVIDER;

    static {
        BRAIN_PROVIDER = Brain.provider(List.of(
                        SensorType.NEAREST_LIVING_ENTITIES,
                        SensorType.NEAREST_PLAYERS,
                        SensorType.NEAREST_ADULT,
                        SensorType.HURT_BY,
                        ModSensorTypes.BEE_TEMPTATIONS,
                        ModSensorTypes.BEE_MEMORIES),
                (var0) -> BeeAi.getActivities());
    }

    @Unique
    public int brainier_bees$HoneyCooldown;
    @Shadow
    private @org.jspecify.annotations.Nullable EntityReference<LivingEntity> persistentAngerTarget;
    @Unique
    private BlockPos brainier_bees$memorizedHome;

    public BeeMixin(EntityType<? extends Bee> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public BlockPos brainier_bees$getMemorizedHome() {
        return this.brainier_bees$memorizedHome;
    }

    @Override
    public void brainier_bees$setMemorizedHome(BlockPos pos) {
        this.brainier_bees$memorizedHome = pos;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    public void Bee(EntityType<? extends Bee> entityType, Level level, CallbackInfo ci) {
        this.setPathfindingMalus(PathType.TRAPDOOR, -1.0F);
    }

    @Inject(method = "registerGoals", at = @At("RETURN"))
    public void killGoals(CallbackInfo ci) {
        this.removeAllGoals(goal -> true);
    }

    @Inject(method = "customServerAiStep", at = @At("RETURN"))
    public void aiStep(CallbackInfo ci) {
        Bee $this = (Bee) (Object) this;
        $this.setNoGravity(true);

        ProfilerFiller profilerFiller = Profiler.get();
        profilerFiller.push("beeBrain");
        this.getBrain().tick((ServerLevel) $this.level(), $this);
        profilerFiller.pop();
        profilerFiller.push("beeActivityUpdate");
        BeeAi.updateActivity($this);
        profilerFiller.pop();

        if (this.persistentAngerTarget != null) {
            getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, getTarget());
        }

        if (getBrain().getMemory(ModMemoryTypes.COOLDOWN_LOCATE_HIVE).isPresent()) {
            if (getBrain().getMemory(ModMemoryTypes.COOLDOWN_LOCATE_HIVE).get() > 0) {
                getBrain().setMemory(ModMemoryTypes.COOLDOWN_LOCATE_HIVE, getBrain().getMemory(ModMemoryTypes.COOLDOWN_LOCATE_HIVE).get() - 1);
            } else {
                getBrain().eraseMemory(ModMemoryTypes.COOLDOWN_LOCATE_HIVE);
            }
        }

        if ((this.brainier_bees$getMemorizedHome() == null) && (getBrain().getMemory(ModMemoryTypes.HIVE_POS).isPresent())) {
            this.brainier_bees$setMemorizedHome(getBrain().getMemory(ModMemoryTypes.HIVE_POS).get().pos());
        }

        if (this.brainier_bees$getMemorizedHome() != null) {
            getBrain().setMemory(ModMemoryTypes.HIVE_POS, new GlobalPos(level().dimension(), this.brainier_bees$getMemorizedHome()));
        }

        if (getBrain().getMemory(ModMemoryTypes.HIVE_POS).isPresent()) {
            if (!(level().getBlockEntity(getBrain().getMemory(ModMemoryTypes.HIVE_POS).get().pos()) instanceof BeehiveBlockEntity)) {
                if (getBrain().getMemory(ModMemoryTypes.HIVE_POS).isPresent()) {
                    this.removeMemorizedHive($this);
                }
            }
            if (brainier_bees$newHiveNearFire()) {
                this.removeMemorizedHive($this);
            }
        }


        if (brainier_bees$newWantsHive()) {
            $this.getBrain().setMemory(ModMemoryTypes.WANTS_HIVE, true);
        } else {
            $this.getBrain().eraseMemory(ModMemoryTypes.WANTS_HIVE);
        }
    }

    @Override
    protected boolean shouldStayCloseToLeashHolder() {
        return false;
    }

    @Unique
    public boolean brainier_bees$newWantsHive() {
        Bee bee = (Bee) (Object) this;
        if (((BeeAccessor) bee).getStayOutOfHiveCountdown() <= 0 && !bee.hasStung() && !this.brainier_bees$newIsPollinating() && bee.getTarget() == null) {
            boolean bl = this.level().environmentAttributes().getValue(EnvironmentAttributes.BEES_STAY_IN_HIVE, this.position()) || brainier_bees$isSickOfSearching() || bee.hasNectar();
            return bl && !this.brainier_bees$newHiveNearFire();
        } else {
            return false;
        }
    }

    @Unique
    public boolean brainier_bees$newIsPollinating() {
        Bee bee = (Bee) (Object) this;

        boolean pollinating = bee.getBrain().hasMemoryValue(ModMemoryTypes.SUCCESSFUL_POLLINATING_TICKS);

        if (pollinating && bee.getBrain().getMemory(ModMemoryTypes.SUCCESSFUL_POLLINATING_TICKS).get() == 0) {
            pollinating = false;
        }

        return pollinating;
    }

    @Unique
    private boolean brainier_bees$newHiveNearFire() {
        Bee bee = (Bee) (Object) this;
        if (!bee.getBrain().hasMemoryValue(ModMemoryTypes.HIVE_POS) || bee.getBrain().getMemory(ModMemoryTypes.HIVE_POS).isEmpty()) {
            return false;
        } else {
            BlockEntity blockEntity = level().getBlockEntity(bee.getBrain().getMemory(ModMemoryTypes.HIVE_POS).get().pos());
            return blockEntity instanceof BeehiveBlockEntity && ((BeehiveBlockEntity) blockEntity).isFireNearby();
        }
    }

    @Unique
    private boolean brainier_bees$isSickOfSearching() {
        Bee bee = (Bee) (Object) this;
        int searchAttempts = BrainierBeesConfig.SEARCH_ATTEMPTS;

        if (!bee.getBrain().hasMemoryValue(ModMemoryTypes.SEARCH_ATTEMPTS)) {
            return false;
        }

        return bee.getBrain().getMemory(ModMemoryTypes.SEARCH_ATTEMPTS).isPresent() && (bee.getBrain().getMemory(ModMemoryTypes.SEARCH_ATTEMPTS).get() >= searchAttempts);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    public void addAdditionalSaveData(ValueOutput valueOutput, CallbackInfo ci) {
        valueOutput.putInt("HoneyCooldown", this.brainier_bees$HoneyCooldown);
        if (this.brainier_bees$getMemorizedHome() != null) {
            valueOutput.storeNullable("MemorizedHome", BlockPos.CODEC, this.brainier_bees$getMemorizedHome());
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    public void readAdditionalSaveData(ValueInput valueInput, CallbackInfo ci) {
        if (valueInput.getInt("HoneyCooldown").isPresent()) {
            this.brainier_bees$HoneyCooldown = valueInput.getInt("HoneyCooldown").get();
        }
    }

    @Inject(method = "getBreedOffspring(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/AgeableMob;)Lnet/minecraft/world/entity/animal/bee/Bee;", at = @At("HEAD"))
    public void getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob, CallbackInfoReturnable<Bee> cir) {
        if (ageableMob != null) {
            BeeAi.initMemories((Bee) ageableMob, ageableMob.getRandom());
        }
    }

    @WrapOperation(method = "createNavigation", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/navigation/FlyingPathNavigation;setCanFloat(Z)V"))
    public void overrideNavigation(FlyingPathNavigation instance, boolean flag, Operation<Void> original) {
        instance.setCanFloat(true);
    }

    @Inject(method = "isFlapping", at = @At("RETURN"), cancellable = true)
    public void isFlapping(CallbackInfoReturnable<Boolean> cir) {
        Bee bee = (Bee) (Object) this;
        cir.setReturnValue(!bee.onGround());
    }

    @Override
    public float getWalkTargetValue(BlockPos blockPos, LevelReader levelReader) {
        return levelReader.getBlockState(blockPos).isAir() ? 10.0f : 0.0f;
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor serverLevelAccessor, DifficultyInstance difficultyInstance, EntitySpawnReason spawnReason, @Nullable SpawnGroupData spawnGroupData) {
        Bee $this = (Bee) (Object) this;
        RandomSource randomSource = serverLevelAccessor.getRandom();
        BeeAi.initMemories($this, randomSource);
        return super.finalizeSpawn(serverLevelAccessor, difficultyInstance, spawnReason, spawnGroupData);
    }

    @Override
    protected @NonNull Brain<Bee> makeBrain(Brain.Packed packedBrain) {
        Bee $this = (Bee) (Object) this;
        return BRAIN_PROVIDER.makeBrain($this, packedBrain);
    }

    @Override
    public @NonNull Brain<Bee> getBrain() {
        return (Brain<Bee>) super.getBrain();
    }

}
