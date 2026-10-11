package dev.apoapsis.ship;

import dev.apoapsis.flight.Environment;
import dev.apoapsis.flight.FlightHooks;
import dev.apoapsis.grid.ShipParts;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * A moving ship: a {@link ShipStructure} with a pose (position of the centre of mass, orientation) and dynamics.
 * Units: metres, seconds, kilograms. Orientation rotates ship-local (body) axes into the current dimension's axes.
 * The server runs the physics in {@link #tick()} and writes the synced fields; clients read the synced fields.
 */
public class ShipEntity extends Entity implements IEntityWithComplexSpawn {
    /** Physics step, seconds: one server tick. */
    private static final double DT = 0.05;
    /** Commanded turn rate, rad/s, before the gyro limit. */
    private static final double MAX_RATE = 0.6;
    /** Time constant of the rate controller, seconds. */
    private static final double TORQUE_TIME = 0.25;
    /** Pilot inputs drop to zero after this many ticks without a packet. */
    private static final int INPUT_TIMEOUT_TICKS = 5;
    /** Downward speed, m/s, above which a landing is hard (sound and warning). */
    private static final double HARD_LANDING_MPS = 10.0;
    /** Vertical impact speed, m/s, above which the ship is destroyed. */
    private static final double CRASH_VERTICAL_MPS = 12.0;
    /** Total speed, m/s, above which touching terrain destroys the ship. */
    private static final double CRASH_CONTACT_MPS = 30.0;
    /** Kinetic friction coefficient between the ship and terrain. */
    private static final double MU_KINETIC = 0.6;
    /** Share of the angular velocity kept each tick in ground contact without thrust. */
    private static final double GROUND_SPIN_KEEP = 0.5;
    /** Guidance gain, 1/s: commanded rate per radian of attitude error for PROGRADE and RETROGRADE. */
    private static final double SAS_GAIN = 1.5;
    /** Pilot fall damage on a crash, hit points (four hearts). */
    private static final float CRASH_PILOT_DAMAGE = 8.0f;
    /** Longest move per substep, metres. */
    private static final double MAX_STEP_M = 0.9;
    private static final int MIN_SUBSTEPS = 4;
    private static final int MAX_SUBSTEPS = 16;
    /** Half-width of the collision probe around each block centre. */
    private static final double PROBE = 0.45;
    private static final double G0 = 9.81;
    private static final double THRUST_CONE_COS = Math.cos(Math.toRadians(30));
    /** The ship is discarded this far below the world's minimum Y (surface only). */
    private static final double BELOW_WORLD_MARGIN = 64;
    /** Ticks at rest on the ground before physics sleeps. */
    private static final int SLEEP_AFTER_TICKS = 40;
    /** Ticks between checks that a sleeping ship is still supported. */
    private static final int SUPPORT_CHECK_TICKS = 20;
    /** Squared spin, (rad/s)^2, below which a landed ship counts as still. */
    private static final double SLEEP_SPIN_SQ = 0.01 * 0.01;
    /** How far below the centre of mass the support check looks, metres. */
    private static final double SUPPORT_PROBE_M = 0.1;

    private static final EntityDataAccessor<Quaternionfc> ORIENTATION =
            SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.QUATERNION);
    private static final EntityDataAccessor<Vector3fc> VELOCITY =
            SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Float> THROTTLE =
            SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FUEL_KG =
            SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.FLOAT);
    /** SAS mode as its {@link SasMode} ordinal. */
    private static final EntityDataAccessor<Integer> SAS =
            SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> LANDED =
            SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.BOOLEAN);

    private ShipStructure structure = new ShipStructure();
    private final Quaternionf orientation = new Quaternionf();
    private Vec3 velocity = Vec3.ZERO;
    private Vec3 angularVelocity = Vec3.ZERO;
    private float throttle;
    private SasMode sasMode = SasMode.HOLD;
    private boolean transferring;
    private boolean landed;
    /** Centre of mass in local block coordinates; fixed at spawn so the blocks keep their pose as fuel burns. */
    private Vec3 comLocal = Vec3.ZERO;
    /** Half-size of the bounding cube, metres. */
    private double halfExtent = 0.5;
    private double pitchIn;
    private double yawIn;
    private double rollIn;
    private int inputAge = INPUT_TIMEOUT_TICKS + 1;
    private boolean terrainCollision;

    /** Solidity of cells looked up this tick. */
    private final CellCache cells = new CellCache();
    /** Hull probes at the current orientation, and a spare set for a candidate orientation (swapped on accept). */
    private Probes probes = new Probes();
    private Probes spare = new Probes();

    /** Values that change only with the structure; rebuilt by {@link #ensureStructureCache()}. */
    private ShipStructure cachedFor;
    private int cachedBlocks = -1;
    private Vec3 noseBody = new Vec3(0, 1, 0);
    private Vec3 forwardBody = new Vec3(0, 0, 1);
    private BlockPos cockpitLocal;
    /** Thrusters within the thrust cone of the nose. */
    private final List<ShipParts.ThrusterSpec> coneThrusters = new ArrayList<>();
    private final double[] dryInertia = new double[3];
    private double gyroTotal;
    private double dryMass;
    private double fuelCapacity;
    /** Drag area, m^2. */
    private double crossSection;
    /** Hull block centres relative to the centre of mass, body axes; the first {@code hullCount} entries are used. */
    private double[] hullX = new double[0];
    private double[] hullY = new double[0];
    private double[] hullZ = new double[0];
    private int hullCount;

    /** Scratch buffers reused every tick. */
    private final double[] props = new double[4];
    private final double[] cmd = new double[3];
    private final double[] spin = new double[3];
    private final double[] thrustOut = new double[2];
    private final double[] cornerX = new double[8];
    private final double[] cornerY = new double[8];
    private final double[] cornerZ = new double[8];
    private final Quaternionf rotationBackup = new Quaternionf();
    private final BlockPos.MutableBlockPos cellPos = new BlockPos.MutableBlockPos();

    /** Ticks the ship has been still on the ground, and sleep state. */
    private int restTicks;
    private boolean asleep;
    private int sleepTicks;
    private Player sleepPilot;

    public ShipEntity(EntityType<? extends ShipEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    /** Spawns a ship whose centre of mass is at {@code comPos}. */
    public static ShipEntity spawn(ServerLevel level, ShipStructure structure, Vec3 comPos, Quaternionfc orientation) {
        ShipEntity ship = new ShipEntity(ShipEntities.SHIP.get(), level);
        ship.setStructure(structure);
        ship.setOrientation(orientation);
        ship.setPos(comPos);
        ship.syncToClients();
        level.addFreshEntity(ship);
        return ship;
    }

    public ShipStructure getStructure() {
        return structure;
    }

    /** Replaces the structure and recomputes the centre of mass from it. */
    public void setStructure(ShipStructure structure) {
        this.structure = structure;
        this.comLocal = structure.centerOfMassLocal();
        invalidateStructureCache();
        updateBounds();
        wake();
    }

    /** Centre of mass in local block coordinates (a block's centre is pos + 0.5), fixed at spawn. */
    public Vec3 getCenterOfMassLocal() {
        return comLocal;
    }

    /** A copy of the orientation (body to dimension axes). */
    public Quaternionf getOrientation() {
        return new Quaternionf(remote() ? entityData.get(ORIENTATION) : orientation);
    }

    public void setOrientation(Quaternionfc q) {
        orientation.set(q);
        wake();
    }

    /** Linear velocity in m/s, dimension axes. Meaningful in the SURFACE regime. */
    public Vec3 getVelocity() {
        return remote() ? toVec3(entityData.get(VELOCITY)) : velocity;
    }

    public void setVelocity(Vec3 v) {
        velocity = v;
        wake();
    }

    /** Angular velocity in rad/s, body axes. Server only; clients see zero. */
    public Vec3 getAngularVelocity() {
        return angularVelocity;
    }

    public void setAngularVelocity(Vec3 w) {
        angularVelocity = w;
        wake();
    }

    /** 0..1. Synced to clients. */
    public float getThrottle() {
        return remote() ? entityData.get(THROTTLE) : throttle;
    }

    public void setThrottle(float t) {
        throttle = Math.max(0, Math.min(1, t));
    }

    /** SAS mode, synced to clients. G cycles OFF, HOLD, PROGRADE, RETROGRADE. */
    public SasMode getSasMode() {
        return remote() ? SasMode.byOrdinal(entityData.get(SAS)) : sasMode;
    }

    /** Sets the SAS mode and syncs it at once (also while the ship sleeps). */
    public void setSasMode(SasMode mode) {
        sasMode = mode;
        entityData.set(SAS, mode.ordinal());
    }

    /** Advances SAS to the next mode (G): OFF, HOLD, PROGRADE, RETROGRADE, OFF. */
    public void cycleSasMode() {
        setSasMode(getSasMode().next());
    }

    /** True unless SAS is OFF. */
    public boolean isSasOn() {
        return getSasMode() != SasMode.OFF;
    }

    /** Compatibility form: true sets HOLD, false sets OFF. */
    public void setSas(boolean on) {
        setSasMode(on ? SasMode.HOLD : SasMode.OFF);
    }

    /** True while FLT moves the ship between dimensions: the pilot may dismount and physics is paused. */
    public boolean isTransferring() {
        return transferring;
    }

    public void setTransferring(boolean transferring) {
        this.transferring = transferring;
    }

    /** Dry mass plus fuel, kg. */
    public double massKg() {
        ensureStructureCache();
        return dryMass + fuelKg();
    }

    /** Fuel on board, kg. Synced to clients. */
    public double fuelKg() {
        return remote() ? entityData.get(FUEL_KG) : structure.fuelKg();
    }

    public double fuelCapacityKg() {
        ensureStructureCache();
        return fuelCapacity;
    }

    /** Removes up to {@code kg} of fuel spread across tanks; returns the amount removed. */
    public double drainFuel(double kg) {
        double total = structure.fuelKg();
        if (kg <= 0 || total <= 0) {
            return 0;
        }
        double take = Math.min(kg, total);
        double keep = 1.0 - take / total;
        for (BlockPos p : new ArrayList<>(structure.fuel().keySet())) {
            structure.setFuel(p, structure.getFuel(p) * keep);
        }
        return take;
    }

    /** Adds fuel up to capacity, shared across tanks in proportion to their free space; returns the amount added. */
    public double refuel(double kg) {
        double free = fuelCapacityKg() - structure.fuelKg();
        if (kg <= 0 || free <= 0) {
            return 0;
        }
        double add = Math.min(kg, free);
        for (Map.Entry<BlockPos, BlockState> e : structure.blocks().entrySet()) {
            double cap = ShipParts.tankCapacityKg(e.getValue());
            BlockPos p = e.getKey();
            double tankFree = cap - structure.getFuel(p);
            if (cap <= 0 || tankFree <= 0) {
                continue;
            }
            structure.setFuel(p, structure.getFuel(p) + add * tankFree / free);
        }
        return add;
    }

    /** Thrust direction (unit vector, body axes): the sum of the thrusters' reversed exhaust. Default +Y. */
    public Vec3 thrustDirectionBody() {
        ensureStructureCache();
        return noseBody;
    }

    /** Total thrust in N at full throttle along {@link #thrustDirectionBody()}, at pressure fraction p/p0. */
    public double maxThrustN(double pressureFraction) {
        return thrustTotals(pressureFraction)[0];
    }

    /** Effective Isp of the engines, seconds (thrust-weighted). */
    public double ispSeconds(double pressureFraction) {
        return thrustTotals(pressureFraction)[1];
    }

    /** Remaining delta-v in m/s at pressure fraction p/p0 (rocket equation over the fuel on board). */
    public double deltaV(double pressureFraction) {
        double isp = ispSeconds(pressureFraction);
        double fuel = fuelKg();
        double dry = massKg() - fuel;
        if (isp <= 0 || fuel <= 0 || dry <= 0) {
            return 0;
        }
        return isp * G0 * Math.log((dry + fuel) / dry);
    }

    /** True when resting on terrain (SURFACE regime only). Synced to clients. */
    public boolean isLanded() {
        return remote() ? entityData.get(LANDED) : landed;
    }

    /** The pilot (first passenger that is a player), or null. */
    public Player getPilot() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    /** Seats a player in the cockpit (server side). */
    public void seatPilot(Player player) {
        player.startRiding(this, true, false);
    }

    /** Stores the pilot's stick and pedal values (each -1..1) and restarts the input timeout. */
    public void setPilotInput(double pitch, double yaw, double roll) {
        pitchIn = Mth.clamp(pitch, -1.0, 1.0);
        yawIn = Mth.clamp(yaw, -1.0, 1.0);
        rollIn = Mth.clamp(roll, -1.0, 1.0);
        inputAge = 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ORIENTATION, new Quaternionf());
        builder.define(VELOCITY, new Vector3f());
        builder.define(THROTTLE, 0.0f);
        builder.define(FUEL_KG, 0.0f);
        builder.define(SAS, SasMode.HOLD.ordinal());
        builder.define(LANDED, false);
    }

    /** The cube around the centre of mass that holds every block, so picking, culling and tracking see the whole ship. */
    @Override
    protected AABB makeBoundingBox(Vec3 position) {
        double h = halfExtent;
        return new AABB(position.x - h, position.y - h, position.z - h, position.x + h, position.y + h, position.z + h);
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    /** Right-click on a ship without a pilot seats the player in the cockpit. */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (getPilot() != null) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide()) {
            seatPilot(player);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Keeps the pilot's eye fixed in the ship frame: the eye is the cockpit centre plus the body-frame offset
     * (0, eyeHeight - 0.5, 0) turned into the world, so a ship on its side does not put the eye outside the cockpit.
     * The feet are the eye minus the eye height along world Y, where the first-person camera stands. At identity
     * orientation the eye is the cockpit centre plus (0, eyeHeight - 0.5, 0), so the feet are the cockpit centre plus
     * (0, -0.5, 0), as before.
     */
    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        double eyeHeight = passenger.getEyeHeight();
        Vec3 eye = cockpitCentre().add(rotate(new Vec3(0, eyeHeight - 0.5, 0)));
        Vec3 feet = eye.add(0, -eyeHeight, 0);
        moveFunction.accept(passenger, feet.x, feet.y, feet.z);
    }

    /** A free spot beside the cockpit, at the cockpit's floor level (or one block up). */
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 centre = cockpitCentre();
        double floorY = centre.y - 0.5;
        for (int up = 0; up <= 1; up++) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                Vec3 target = new Vec3(centre.x + d.getStepX(), floorY + up, centre.z + d.getStepZ());
                for (Pose pose : passenger.getDismountPoses()) {
                    if (DismountHelper.canDismountTo(level(), target, passenger, pose)) {
                        passenger.setPose(pose);
                        return target;
                    }
                }
            }
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        structure = ShipStructure.load(input.childOrEmpty("structure"));
        comLocal = new Vec3(input.getDoubleOr("comX", 0), input.getDoubleOr("comY", 0), input.getDoubleOr("comZ", 0));
        invalidateStructureCache();
        orientation.set(input.getFloatOr("qx", 0f), input.getFloatOr("qy", 0f), input.getFloatOr("qz", 0f),
                input.getFloatOr("qw", 1f));
        if (orientation.lengthSquared() < 1e-6f) {
            orientation.identity();
        }
        orientation.normalize();
        velocity = new Vec3(input.getDoubleOr("vx", 0), input.getDoubleOr("vy", 0), input.getDoubleOr("vz", 0));
        angularVelocity = new Vec3(input.getDoubleOr("wx", 0), input.getDoubleOr("wy", 0), input.getDoubleOr("wz", 0));
        throttle = Math.max(0f, Math.min(1f, input.getFloatOr("throttle", 0f)));
        int mode = input.getIntOr("sasMode", -1);
        sasMode = mode >= 0 ? SasMode.byOrdinal(mode) : (input.getBooleanOr("sas", true) ? SasMode.HOLD : SasMode.OFF);
        landed = input.getBooleanOr("landed", false);
        updateBounds();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        structure.save(output.child("structure"));
        output.putDouble("comX", comLocal.x);
        output.putDouble("comY", comLocal.y);
        output.putDouble("comZ", comLocal.z);
        output.putFloat("qx", orientation.x);
        output.putFloat("qy", orientation.y);
        output.putFloat("qz", orientation.z);
        output.putFloat("qw", orientation.w);
        output.putDouble("vx", velocity.x);
        output.putDouble("vy", velocity.y);
        output.putDouble("vz", velocity.z);
        output.putDouble("wx", angularVelocity.x);
        output.putDouble("wy", angularVelocity.y);
        output.putDouble("wz", angularVelocity.z);
        output.putFloat("throttle", throttle);
        output.putInt("sasMode", sasMode.ordinal());
        output.putBoolean("landed", landed);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        ShipStructure.STREAM_CODEC.encode(buffer, structure);
        buffer.writeDouble(comLocal.x);
        buffer.writeDouble(comLocal.y);
        buffer.writeDouble(comLocal.z);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buffer) {
        structure = ShipStructure.STREAM_CODEC.decode(buffer);
        comLocal = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        invalidateStructureCache();
        updateBounds();
    }

    /**
     * Server tick. Order: environment, pilot input, rotation, orbital or surface translation, fuel, then the
     * FLT hook. Physics is skipped while {@link #isTransferring()} and while the ship sleeps on the ground.
     */
    @Override
    public void tick() {
        if (level().isClientSide()) {
            return;
        }
        cells.clear();
        // Step 2 input: the pilot's values time out after five ticks without a packet, or when nobody flies.
        inputAge = Math.min(inputAge + 1, INPUT_TIMEOUT_TICKS + 1);
        if (inputAge > INPUT_TIMEOUT_TICKS || getPilot() == null) {
            pitchIn = 0;
            yawIn = 0;
            rollIn = 0;
        }
        if (transferring) {
            syncToClients();
            return;
        }
        // Step 1: environment.
        Environment env = FlightHooks.environment(this);
        terrainCollision = env.terrainCollision();
        if (asleep) {
            if (shouldWake(env)) {
                wake();
            } else if (++sleepTicks % SUPPORT_CHECK_TICKS == 0 && !supportedAtRest()) {
                wake();
            }
            if (asleep) {
                FlightHooks.afterTick(this);
                return;
            }
        }
        if (!env.orbital()) {
            ensureProbes();
        }
        // Step 3: rotation.
        stepRotation(env);
        if (env.orbital()) {
            // Step 4: the orbit model owns translation.
            double mass = Math.max(massKg(), 1.0);
            Vec3 accel = rotate(thrustDirectionBody()).scale(effectiveThrustN(env.pressureFraction()) / mass);
            FlightHooks.orbitalTick(this, accel, DT);
            landed = false;
        } else {
            // Step 5: surface physics with terrain collision.
            double impactMps = stepSurface(env);
            if (impactMps >= 0) {
                destroyOnImpact(impactMps);
                return;
            }
        }
        // Step 6: fuel.
        burnFuel(env);
        if (!env.orbital() && getY() < level().getMinY() - BELOW_WORLD_MARGIN) {
            remove(RemovalReason.DISCARDED);
            return;
        }
        updateRest(env);
        syncToClients();
        // Step 7: flight hook (transitions, milestones, client sync).
        FlightHooks.afterTick(this);
    }

    // ---- rest and sleep ------------------------------------------------------------------------------------

    /** Counts the ticks the ship is still on the ground and puts it to sleep after {@link #SLEEP_AFTER_TICKS}. */
    private void updateRest(Environment env) {
        boolean still = !env.orbital() && landed && throttle <= 0f && !hasPilotInput()
                && angularVelocity.lengthSqr() < SLEEP_SPIN_SQ;
        if (!still) {
            restTicks = 0;
            return;
        }
        if (++restTicks < SLEEP_AFTER_TICKS) {
            return;
        }
        restTicks = 0;
        asleep = true;
        sleepTicks = 0;
        sleepPilot = getPilot();
        velocity = Vec3.ZERO;
        angularVelocity = Vec3.ZERO;
    }

    /** A sleeping ship wakes when it leaves the surface, a pilot boards or leaves, or the pilot touches the controls. */
    private boolean shouldWake(Environment env) {
        return env.orbital() || getPilot() != sleepPilot || throttle > 0f || hasPilotInput();
    }

    /** The ship is still held up if its probes would hit terrain a little lower. */
    private boolean supportedAtRest() {
        ensureProbes();
        return collidesWith(probes, getX(), getY() - SUPPORT_PROBE_M, getZ());
    }

    private void wake() {
        asleep = false;
        restTicks = 0;
        sleepTicks = 0;
        sleepPilot = null;
    }

    private boolean hasPilotInput() {
        return pitchIn != 0 || yawIn != 0 || rollIn != 0;
    }

    // ---- rotation ------------------------------------------------------------------------------------------

    /**
     * Integrates the body rate (control or SAS torque, through the gyro limit). A step that puts the hull into terrain is
     * reverted and the angular velocity is zeroed for this tick, so no torque is carried into the next tick's contact.
     */
    private void stepRotation(Environment env) {
        ensureStructureCache();
        inertiaAndGyro(props);
        double gyro = props[3];
        double imax = Math.max(props[0], Math.max(props[1], props[2]));
        double rate = gyro > 0 ? Math.min(MAX_RATE, gyro / imax) : 0.0;
        boolean hasInput = hasPilotInput();
        boolean guided = sasMode == SasMode.PROGRADE || sasMode == SasMode.RETROGRADE;
        boolean torqued = hasInput || sasMode != SasMode.OFF;

        if (hasInput) {
            Vec3 nose = noseBody;
            Vec3 right = forwardBody.cross(nose).normalize();
            Vec3 c = right.scale(pitchIn * rate).add(forwardBody.scale(yawIn * rate)).subtract(nose.scale(rollIn * rate));
            cmd[0] = c.x;
            cmd[1] = c.y;
            cmd[2] = c.z;
        } else if (guided) {
            guidanceRate(rate);
        } else {
            cmd[0] = 0;
            cmd[1] = 0;
            cmd[2] = 0;
        }

        double[] w = spin;
        w[0] = angularVelocity.x;
        w[1] = angularVelocity.y;
        w[2] = angularVelocity.z;
        if (torqued) {
            for (int i = 0; i < 3; i++) {
                double torque = Mth.clamp((cmd[i] - w[i]) * props[i] / TORQUE_TIME, -gyro, gyro);
                w[i] += torque / props[i] * DT;
            }
        }
        if (w[0] == 0 && w[1] == 0 && w[2] == 0) {
            angularVelocity = Vec3.ZERO;
            return;
        }

        // JOML's integrate() applies a world-axis rate, so turn the body rate into dimension axes first.
        Vec3 worldRate = ShipGeometry.apply(ShipGeometry.matrix(orientation), new Vec3(w[0], w[1], w[2]));
        rotationBackup.set(orientation);
        orientation.integrate((float) DT, (float) worldRate.x, (float) worldRate.y, (float) worldRate.z);
        orientation.normalize();

        if (!env.orbital()) {
            buildProbes(orientation, spare);
            if (collidesWith(spare, getX(), getY(), getZ())) {
                orientation.set(rotationBackup);
                angularVelocity = Vec3.ZERO;
                return;
            }
            Probes t = probes;
            probes = spare;
            spare = t;
        }
        angularVelocity = new Vec3(w[0], w[1], w[2]);
    }

    /**
     * Body-axis rates (into {@code cmd}) that turn the nose toward the velocity (PROGRADE) or away from it
     * (RETROGRADE). Zero when the ship is at rest or the nose already points there.
     */
    private void guidanceRate(double rateLimit) {
        cmd[0] = 0;
        cmd[1] = 0;
        cmd[2] = 0;
        Vec3 v = FlightHooks.velocityDirection(this);
        if (v.lengthSqr() < 1e-9 || rateLimit <= 0) {
            return;
        }
        Vec3 d = sasMode == SasMode.RETROGRADE ? v.reverse() : v;
        double[] m = ShipGeometry.matrix(orientation);
        Vec3 nose = ShipGeometry.apply(m, noseBody);
        Vec3 axis = nose.cross(d);
        double s = axis.length();
        double angle;
        if (s >= 1e-9) {
            angle = Math.atan2(s, nose.dot(d));
        } else if (nose.dot(d) >= 0) {
            return;
        } else {
            // Exactly opposite: any axis perpendicular to the nose turns it round.
            axis = nose.cross(Math.abs(nose.x) < 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0));
            s = axis.length();
            angle = Math.PI;
        }
        double rate = Mth.clamp(angle * SAS_GAIN, 0.0, rateLimit);
        Vec3 body = applyTransposed(m, axis.scale(rate / s));
        cmd[0] = body.x;
        cmd[1] = body.y;
        cmd[2] = body.z;
    }

    /** Diagonal inertia about the centre of mass (kg m^2, into out[0..2]) and the total gyro torque (N m, out[3]). */
    private void inertiaAndGyro(double[] out) {
        out[0] = dryInertia[0];
        out[1] = dryInertia[1];
        out[2] = dryInertia[2];
        out[3] = gyroTotal;
        for (Map.Entry<BlockPos, Double> e : structure.fuel().entrySet()) {
            double f = e.getValue();
            if (f <= 0) {
                continue;
            }
            BlockPos p = e.getKey();
            double dx = p.getX() + 0.5 - comLocal.x;
            double dy = p.getY() + 0.5 - comLocal.y;
            double dz = p.getZ() + 0.5 - comLocal.z;
            out[0] += f * (dy * dy + dz * dz) + f / 6.0;
            out[1] += f * (dx * dx + dz * dz) + f / 6.0;
            out[2] += f * (dx * dx + dy * dy) + f / 6.0;
        }
        for (int i = 0; i < 3; i++) {
            out[i] = Math.max(out[i], 1.0);
        }
    }

    // ---- surface translation -------------------------------------------------------------------------------

    /**
     * Surface translation with terrain contact. Returns the impact speed (m/s) if the ship was destroyed this tick,
     * otherwise -1.
     */
    private double stepSurface(Environment env) {
        ensureStructureCache();
        double mass = Math.max(massKg(), 1.0);
        double thrustN = effectiveThrustN(env.pressureFraction());
        Vec3 thrustAccel = rotate(thrustDirectionBody()).scale(thrustN / mass);
        double rho = env.airDensity();
        Vec3 g = env.gravity();
        double gMag = g.length();

        double x = getX(), y = getY(), z = getZ();
        double vx = velocity.x, vy = velocity.y, vz = velocity.z;
        double speed0 = velocity.length();
        int steps = (int) Mth.clamp(Math.ceil(speed0 * DT / MAX_STEP_M), MIN_SUBSTEPS, MAX_SUBSTEPS);
        double h = DT / steps;
        boolean blockedBelow = false;

        for (int i = 0; i < steps; i++) {
            // Speed at the start of the substep: a collision response may never leave the ship faster than this.
            double vStart = Math.sqrt(vx * vx + vy * vy + vz * vz);
            double drag = 0.5 * rho * vStart * crossSection / mass;
            vx += (g.x + thrustAccel.x - drag * vx) * h;
            vy += (g.y + thrustAccel.y - drag * vy) * h;
            vz += (g.z + thrustAccel.z - drag * vz) * h;
            boolean downContact = false;

            // Y first, then X, then Z; each axis is reverted on contact.
            double ny = y + vy * h;
            if (collidesWith(probes, x, ny, z)) {
                double vContact = Math.sqrt(vx * vx + vy * vy + vz * vz);
                if (vContact > CRASH_CONTACT_MPS || Math.abs(vy) > CRASH_VERTICAL_MPS) {
                    setPos(x, y, z);
                    return vContact;
                }
                if (vy <= 0) {
                    downContact = true;
                    blockedBelow = true;
                    if (-vy > HARD_LANDING_MPS) {
                        onHardLanding();
                    }
                }
                vy = 0;
                double k = clampFactor(vx, vy, vz, vStart);
                vx *= k;
                vy *= k;
                vz *= k;
            } else {
                y = ny;
            }
            double nx = x + vx * h;
            if (collidesWith(probes, nx, y, z)) {
                double vContact = Math.sqrt(vx * vx + vy * vy + vz * vz);
                if (vContact > CRASH_CONTACT_MPS) {
                    setPos(x, y, z);
                    return vContact;
                }
                vx = 0;
                double k = clampFactor(vx, vy, vz, vStart);
                vy *= k;
                vz *= k;
            } else {
                x = nx;
            }
            double nz = z + vz * h;
            if (collidesWith(probes, x, y, nz)) {
                double vContact = Math.sqrt(vx * vx + vy * vy + vz * vz);
                if (vContact > CRASH_CONTACT_MPS) {
                    setPos(x, y, z);
                    return vContact;
                }
                vz = 0;
                double k = clampFactor(vx, vy, vz, vStart);
                vx *= k;
                vy *= k;
            } else {
                z = nz;
            }

            if (downContact) {
                // Kinetic friction on the horizontal velocity: mu g per second, never reversing it.
                double sh = Math.sqrt(vx * vx + vz * vz);
                if (sh > 0) {
                    double k = Math.max(0.0, sh - MU_KINETIC * gMag * h) / sh;
                    vx *= k;
                    vz *= k;
                }
            }
        }

        double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
        landed = blockedBelow && speed < 1.0;
        if (blockedBelow && thrustN <= 0) {
            angularVelocity = angularVelocity.scale(GROUND_SPIN_KEEP);
        }
        if (landed && thrustN < mass * gMag) {
            vx *= 0.8;
            vz *= 0.8;
        }
        setPos(x, y, z);
        velocity = new Vec3(vx, vy, vz);
        return -1;
    }

    /** Scale factor that brings the speed of (vx, vy, vz) down to {@code limit}; 1 if it is not faster. */
    private static double clampFactor(double vx, double vy, double vz, double limit) {
        double s = Math.sqrt(vx * vx + vy * vy + vz * vz);
        return s > limit ? limit / s : 1.0;
    }

    private void onHardLanding() {
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 1.0f, 0.6f);
        Player pilot = getPilot();
        if (pilot != null) {
            pilot.sendOverlayMessage(Component.translatable("message.apoapsis.ship.hard_landing"));
        }
    }

    /**
     * The ship is lost on impact: a cosmetic blast (no block damage, no drops), the pilot put down beside the impact point
     * with fall damage unless creative, and the entity discarded.
     */
    private void destroyOnImpact(double speedMps) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 at = position();
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        serverLevel.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 8, 1.5, 1.0, 1.5, 0.05);
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 24, 2.5, 1.5, 2.5, 0.05);
        serverLevel.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, 4.0f, 0.8f);

        Player pilot = getPilot();
        if (pilot != null) {
            pilot.stopRiding();
            Vec3 spot = safeSpotNear(serverLevel, pilot, at);
            pilot.teleportTo(spot.x, spot.y, spot.z);
            if (!pilot.isCreative()) {
                pilot.hurtServer(serverLevel, serverLevel.damageSources().fall(), CRASH_PILOT_DAMAGE);
            }
            pilot.sendSystemMessage(Component.translatable("message.apoapsis.ship.lost_on_impact", Math.round(speedMps)));
        }
        remove(RemovalReason.DISCARDED);
    }

    /** A standing spot on the ground beside the impact point, or just above the ground there if none is clear. */
    private static Vec3 safeSpotNear(ServerLevel level, Player pilot, Vec3 at) {
        int bx = Mth.floor(at.x);
        int bz = Mth.floor(at.z);
        int[][] offsets = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}, {4, 0}, {-4, 0}, {0, 4}, {0, -4}};
        for (int[] o : offsets) {
            int cx = bx + o[0];
            int cz = bz + o[1];
            int feetY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
            Vec3 target = new Vec3(cx + 0.5, feetY, cz + 0.5);
            if (DismountHelper.canDismountTo(level, target, pilot, Pose.STANDING)) {
                return target;
            }
        }
        int feetY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
        return new Vec3(at.x, Math.max(at.y, feetY) + 0.5, at.z);
    }

    // ---- probes and collision ------------------------------------------------------------------------------

    /**
     * Rebuilds the values that change only with the structure: nose, cone thrusters, cockpit, mass, inertia, gyro torque
     * and the hull-shell blocks (a block with at least one empty neighbour). Cached until the structure object or its
     * block count changes.
     */
    private void ensureStructureCache() {
        if (cachedFor == structure && cachedBlocks == structure.size()) {
            return;
        }
        cachedFor = structure;
        cachedBlocks = structure.size();
        Map<BlockPos, BlockState> blocks = structure.blocks();

        double nx = 0, ny = 0, nz = 0;
        for (BlockState state : blocks.values()) {
            ShipParts.ThrusterSpec spec = ShipParts.thruster(state);
            if (spec != null) {
                Vec3i u = spec.exhaust().getUnitVec3i();
                nx -= u.getX();
                ny -= u.getY();
                nz -= u.getZ();
            }
        }
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        noseBody = len < 1e-9 ? new Vec3(0, 1, 0) : new Vec3(nx / len, ny / len, nz / len);

        coneThrusters.clear();
        for (BlockState state : blocks.values()) {
            ShipParts.ThrusterSpec spec = ShipParts.thruster(state);
            if (spec != null) {
                Vec3i u = spec.exhaust().getUnitVec3i();
                Vec3 thrustDir = new Vec3(-u.getX(), -u.getY(), -u.getZ());
                if (thrustDir.dot(noseBody) >= THRUST_CONE_COS) {
                    coneThrusters.add(spec);
                }
            }
        }

        cockpitLocal = structure.cockpit();
        Direction facing = cockpitLocal == null ? null : ShipGeometry.facingOf(structure.get(cockpitLocal));
        forwardBody = cockpitForward(facing, noseBody);

        dryMass = 0;
        fuelCapacity = 0;
        gyroTotal = 0;
        dryInertia[0] = 0;
        dryInertia[1] = 0;
        dryInertia[2] = 0;
        for (Map.Entry<BlockPos, BlockState> e : blocks.entrySet()) {
            BlockPos p = e.getKey();
            BlockState state = e.getValue();
            double m = ShipParts.massKg(state);
            dryMass += m;
            fuelCapacity += ShipParts.tankCapacityKg(state);
            gyroTotal += ShipParts.gyroTorqueNm(state);
            double dx = p.getX() + 0.5 - comLocal.x;
            double dy = p.getY() + 0.5 - comLocal.y;
            double dz = p.getZ() + 0.5 - comLocal.z;
            dryInertia[0] += m * (dy * dy + dz * dz) + m / 6.0;
            dryInertia[1] += m * (dx * dx + dz * dz) + m / 6.0;
            dryInertia[2] += m * (dx * dx + dy * dy) + m / 6.0;
        }

        int n = blocks.size();
        if (hullX.length < n) {
            hullX = new double[n];
            hullY = new double[n];
            hullZ = new double[n];
        }
        hullCount = 0;
        for (BlockPos p : blocks.keySet()) {
            if (!isShell(p, blocks)) {
                continue;
            }
            hullX[hullCount] = p.getX() + 0.5 - comLocal.x;
            hullY[hullCount] = p.getY() + 0.5 - comLocal.y;
            hullZ[hullCount] = p.getZ() + 0.5 - comLocal.z;
            hullCount++;
        }
        crossSection = 0.5 * Math.pow(cachedBlocks, 2.0 / 3.0);
        probes.valid = false;
        spare.valid = false;
    }

    private void invalidateStructureCache() {
        cachedFor = null;
        probes.valid = false;
        spare.valid = false;
    }

    /** True if some face of the block touches empty space in the structure. */
    private static boolean isShell(BlockPos p, Map<BlockPos, BlockState> blocks) {
        for (Direction d : Direction.values()) {
            if (!blocks.containsKey(p.relative(d))) {
                return true;
            }
        }
        return false;
    }

    /** Forward in body axes: the cockpit facing with the nose component removed (fallback +Z). */
    private static Vec3 cockpitForward(Direction facing, Vec3 nose) {
        Vec3 f = facing == null ? new Vec3(0, 0, 1) : Vec3.atLowerCornerOf(facing.getUnitVec3i());
        Vec3 p = f.subtract(nose.scale(f.dot(nose)));
        if (p.lengthSqr() < 1e-6) {
            p = new Vec3(0, 0, 1).subtract(nose.scale(nose.z));
            if (p.lengthSqr() < 1e-6) {
                p = new Vec3(1, 0, 0).subtract(nose.scale(nose.x));
            }
        }
        return p.normalize();
    }

    /** Makes the probes match the current orientation, rebuilding them only when it changed. */
    private void ensureProbes() {
        ensureStructureCache();
        if (!probes.valid || !sameOrientation(probes.orientation, orientation)) {
            buildProbes(orientation, probes);
        }
    }

    /** Fills {@code out} with the probe points of the hull at orientation {@code q}, reusing its arrays. */
    private void buildProbes(Quaternionfc q, Probes out) {
        ensureStructureCache();
        double[] m = ShipGeometry.matrix(q);
        int n = hullCount * 9;
        if (out.x.length < n) {
            out.x = new double[n];
            out.y = new double[n];
            out.z = new double[n];
        }
        int ci = 0;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    Vec3 corner = ShipGeometry.apply(m, new Vec3(sx * PROBE, sy * PROBE, sz * PROBE));
                    cornerX[ci] = corner.x;
                    cornerY[ci] = corner.y;
                    cornerZ[ci] = corner.z;
                    ci++;
                }
            }
        }
        int k = 0;
        for (int i = 0; i < hullCount; i++) {
            double lx = hullX[i], ly = hullY[i], lz = hullZ[i];
            double cx = m[0] * lx + m[1] * ly + m[2] * lz;
            double cy = m[3] * lx + m[4] * ly + m[5] * lz;
            double cz = m[6] * lx + m[7] * ly + m[8] * lz;
            out.x[k] = cx;
            out.y[k] = cy;
            out.z[k] = cz;
            k++;
            for (int c = 0; c < 8; c++) {
                out.x[k] = cx + cornerX[c];
                out.y[k] = cy + cornerY[c];
                out.z[k] = cz + cornerZ[c];
                k++;
            }
        }
        out.count = k;
        out.orientation.set(q);
        out.valid = true;
    }

    private static boolean sameOrientation(Quaternionfc a, Quaternionfc b) {
        return a.x() == b.x() && a.y() == b.y() && a.z() == b.z() && a.w() == b.w();
    }

    /** True if any probe of the probe set, with its centre of mass at (x, y, z), lies in a solid cell. */
    private boolean collidesWith(Probes s, double x, double y, double z) {
        if (!terrainCollision) {
            return false;
        }
        for (int k = 0; k < s.count; k++) {
            if (cellSolid(Mth.floor(x + s.x[k]), Mth.floor(y + s.y[k]), Mth.floor(z + s.z[k]))) {
                return true;
            }
        }
        return false;
    }

    private boolean cellSolid(int x, int y, int z) {
        long key = BlockPos.asLong(x, y, z);
        int cached = cells.get(key);
        if (cached >= 0) {
            return cached == 1;
        }
        cellPos.set(x, y, z);
        boolean solid = level().hasChunkAt(cellPos)
                && !level().getBlockState(cellPos).getCollisionShape(level(), cellPos).isEmpty();
        cells.put(key, solid);
        return solid;
    }

    // ---- thrust and fuel -----------------------------------------------------------------------------------

    /** Thrust force now: throttle times full thrust, zero without fuel. */
    private double effectiveThrustN(double pressureFraction) {
        return fuelKg() > 0 ? throttle * maxThrustN(pressureFraction) : 0.0;
    }

    /** Burns throttle * thrust / (Isp g0) for one tick, spread across the tanks. */
    private void burnFuel(Environment env) {
        double pf = env.pressureFraction();
        double thrustFull = maxThrustN(pf);
        double isp = ispSeconds(pf);
        if (thrustFull <= 0 || isp <= 0 || structure.fuelKg() <= 0) {
            return;
        }
        drainFuel(throttle * thrustFull / (isp * G0) * DT);
    }

    /** {total thrust at full throttle (N), thrust-weighted Isp (s)} over the thrusters within 30 degrees of the nose. */
    private double[] thrustTotals(double pressureFraction) {
        ensureStructureCache();
        double total = 0;
        double ispWeighted = 0;
        for (int i = 0; i < coneThrusters.size(); i++) {
            ShipParts.ThrusterSpec spec = coneThrusters.get(i);
            double n = spec.thrustN(pressureFraction);
            total += n;
            ispWeighted += n * spec.isp(pressureFraction);
        }
        thrustOut[0] = total;
        thrustOut[1] = total > 0 ? ispWeighted / total : 0.0;
        return thrustOut;
    }

    // ---- helpers -------------------------------------------------------------------------------------------

    private void updateBounds() {
        double maxSq = 0;
        for (BlockPos p : structure.blocks().keySet()) {
            for (int dx = 0; dx <= 1; dx++) {
                for (int dy = 0; dy <= 1; dy++) {
                    for (int dz = 0; dz <= 1; dz++) {
                        double x = p.getX() + dx - comLocal.x;
                        double y = p.getY() + dy - comLocal.y;
                        double z = p.getZ() + dz - comLocal.z;
                        maxSq = Math.max(maxSq, x * x + y * y + z * z);
                    }
                }
            }
        }
        halfExtent = Math.sqrt(maxSq) + 0.5;
        setBoundingBox(makeBoundingBox(position()));
    }

    /** World centre of the cockpit block, or the centre of mass when the ship has no cockpit. */
    private Vec3 cockpitCentre() {
        ensureStructureCache();
        BlockPos c = cockpitLocal;
        if (c == null) {
            return position();
        }
        Vec3 local = new Vec3(c.getX() + 0.5 - comLocal.x, c.getY() + 0.5 - comLocal.y, c.getZ() + 0.5 - comLocal.z);
        return position().add(ShipGeometry.apply(ShipGeometry.matrix(getOrientation()), local));
    }

    /** Body vector to dimension axes, using the server-side orientation. */
    private Vec3 rotate(Vec3 bodyVector) {
        return ShipGeometry.apply(ShipGeometry.matrix(orientation), bodyVector);
    }

    /** Dimension-axis vector to body axes (the transpose of the body-to-dimension matrix). */
    private static Vec3 applyTransposed(double[] m, Vec3 v) {
        return new Vec3(
                m[0] * v.x + m[3] * v.y + m[6] * v.z,
                m[1] * v.x + m[4] * v.y + m[7] * v.z,
                m[2] * v.x + m[5] * v.y + m[8] * v.z);
    }

    private boolean remote() {
        return level().isClientSide();
    }

    private void syncToClients() {
        entityData.set(ORIENTATION, new Quaternionf(orientation));
        entityData.set(VELOCITY, new Vector3f((float) velocity.x, (float) velocity.y, (float) velocity.z));
        entityData.set(THROTTLE, throttle);
        entityData.set(FUEL_KG, (float) structure.fuelKg());
        entityData.set(SAS, sasMode.ordinal());
        entityData.set(LANDED, landed);
    }

    private static Vec3 toVec3(Vector3fc v) {
        return new Vec3(v.x(), v.y(), v.z());
    }

    /** Probe points of the hull for one orientation, relative to the centre of mass. The arrays are reused. */
    private static final class Probes {
        double[] x = new double[0];
        double[] y = new double[0];
        double[] z = new double[0];
        int count;
        boolean valid;
        final Quaternionf orientation = new Quaternionf();
    }

    /**
     * Open-addressing map from a packed block position to "solid". Entries carry a generation stamp, so clearing per
     * tick is a counter increment, and no objects are allocated.
     */
    private static final class CellCache {
        private long[] keys = new long[1024];
        private byte[] solid = new byte[1024];
        private int[] stamp = new int[1024];
        private int generation = 1;
        private int size;

        void clear() {
            size = 0;
            if (++generation == Integer.MAX_VALUE) {
                Arrays.fill(stamp, 0);
                generation = 1;
            }
        }

        /** 1 if the cell is solid, 0 if it is not, -1 if it has not been looked up this tick. */
        int get(long key) {
            int i = slot(key);
            return stamp[i] == generation ? solid[i] : -1;
        }

        void put(long key, boolean isSolid) {
            if ((size + 1) * 2 > keys.length) {
                grow();
            }
            int i = slot(key);
            if (stamp[i] != generation) {
                stamp[i] = generation;
                keys[i] = key;
                size++;
            }
            solid[i] = (byte) (isSolid ? 1 : 0);
        }

        /** The slot holding {@code key} this generation, or the empty slot where it belongs. */
        private int slot(long key) {
            int mask = keys.length - 1;
            int i = (int) ((key * 0x9E3779B97F4A7C15L) >>> 32) & mask;
            while (stamp[i] == generation && keys[i] != key) {
                i = (i + 1) & mask;
            }
            return i;
        }

        private void grow() {
            long[] oldKeys = keys;
            byte[] oldSolid = solid;
            int[] oldStamp = stamp;
            int oldGeneration = generation;
            keys = new long[oldKeys.length * 2];
            solid = new byte[oldKeys.length * 2];
            stamp = new int[oldKeys.length * 2];
            size = 0;
            for (int i = 0; i < oldKeys.length; i++) {
                if (oldStamp[i] == oldGeneration) {
                    int j = slot(oldKeys[i]);
                    stamp[j] = generation;
                    keys[j] = oldKeys[i];
                    solid[j] = oldSolid[i];
                    size++;
                }
            }
        }
    }
}
