package dev.apoapsis.ship;

import dev.apoapsis.flight.Environment;
import dev.apoapsis.flight.FlightHooks;
import dev.apoapsis.grid.ShipParts;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
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
    /** Downward speed, m/s, above which a landing is hard. */
    private static final double HARD_LANDING_MPS = 10.0;
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

    private static final EntityDataAccessor<Quaternionfc> ORIENTATION =
            SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.QUATERNION);
    private static final EntityDataAccessor<Vector3fc> VELOCITY =
            SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Float> THROTTLE =
            SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FUEL_KG =
            SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> SAS =
            SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LANDED =
            SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.BOOLEAN);

    private ShipStructure structure = new ShipStructure();
    private final Quaternionf orientation = new Quaternionf();
    private Vec3 velocity = Vec3.ZERO;
    private Vec3 angularVelocity = Vec3.ZERO;
    private float throttle;
    private boolean sas = true;
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
    /** Collision lookups cached for the current tick; keyed by block position. */
    private final Map<Long, Boolean> cellCache = new HashMap<>();

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
        updateBounds();
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
    }

    /** Linear velocity in m/s, dimension axes. Meaningful in the SURFACE regime. */
    public Vec3 getVelocity() {
        return remote() ? toVec3(entityData.get(VELOCITY)) : velocity;
    }

    public void setVelocity(Vec3 v) {
        velocity = v;
    }

    /** Angular velocity in rad/s, body axes. Server only; clients see zero. */
    public Vec3 getAngularVelocity() {
        return angularVelocity;
    }

    public void setAngularVelocity(Vec3 w) {
        angularVelocity = w;
    }

    /** 0..1. Synced to clients. */
    public float getThrottle() {
        return remote() ? entityData.get(THROTTLE) : throttle;
    }

    public void setThrottle(float t) {
        throttle = Math.max(0, Math.min(1, t));
    }

    /** SAS mode. STUB from integration: node FIX-A1 stores and syncs it; HOLD when SAS is on, else OFF. */
    public SasMode getSasMode() {
        return isSasOn() ? SasMode.HOLD : SasMode.OFF;
    }

    /** Advances SAS to the next mode (G). STUB from integration: node FIX-A1 implements it. */
    public void cycleSasMode() {
        setSas(!isSasOn());
    }

    public boolean isSasOn() {
        return remote() ? entityData.get(SAS) : sas;
    }

    public void setSas(boolean on) {
        sas = on;
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
        return structure.dryMassKg() + fuelKg();
    }

    /** Fuel on board, kg. Synced to clients. */
    public double fuelKg() {
        return remote() ? entityData.get(FUEL_KG) : structure.fuelKg();
    }

    public double fuelCapacityKg() {
        return structure.fuelCapacityKg();
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
        double x = 0, y = 0, z = 0;
        for (BlockState state : structure.blocks().values()) {
            ShipParts.ThrusterSpec spec = ShipParts.thruster(state);
            if (spec != null) {
                Vec3i u = spec.exhaust().getUnitVec3i();
                x -= u.getX();
                y -= u.getY();
                z -= u.getZ();
            }
        }
        double len = Math.sqrt(x * x + y * y + z * z);
        return len < 1e-9 ? new Vec3(0, 1, 0) : new Vec3(x / len, y / len, z / len);
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
        builder.define(SAS, true);
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

    /** Puts the pilot's feet at the bottom of the cockpit block. */
    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        Vec3 feet = cockpitCentre().add(0, -0.5, 0);
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
        orientation.set(input.getFloatOr("qx", 0f), input.getFloatOr("qy", 0f), input.getFloatOr("qz", 0f),
                input.getFloatOr("qw", 1f));
        if (orientation.lengthSquared() < 1e-6f) {
            orientation.identity();
        }
        orientation.normalize();
        velocity = new Vec3(input.getDoubleOr("vx", 0), input.getDoubleOr("vy", 0), input.getDoubleOr("vz", 0));
        angularVelocity = new Vec3(input.getDoubleOr("wx", 0), input.getDoubleOr("wy", 0), input.getDoubleOr("wz", 0));
        throttle = Math.max(0f, Math.min(1f, input.getFloatOr("throttle", 0f)));
        sas = input.getBooleanOr("sas", true);
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
        output.putBoolean("sas", sas);
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
        updateBounds();
    }

    /**
     * Server tick. Order: environment, pilot input, rotation, orbital or surface translation, fuel, then the
     * FLT hook. Physics is skipped while {@link #isTransferring()}.
     */
    @Override
    public void tick() {
        if (level().isClientSide()) {
            return;
        }
        cellCache.clear();
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
            stepSurface(env);
        }
        // Step 6: fuel.
        burnFuel(env);
        if (!env.orbital() && getY() < level().getMinY() - BELOW_WORLD_MARGIN) {
            remove(RemovalReason.DISCARDED);
            return;
        }
        syncToClients();
        // Step 7: flight hook (transitions, milestones, client sync).
        FlightHooks.afterTick(this);
    }

    // ---- rotation ------------------------------------------------------------------------------------------

    private void stepRotation(Environment env) {
        double[] props = inertiaAndGyro();
        double[] inertia = {props[0], props[1], props[2]};
        double gyro = props[3];

        Vec3 nose = thrustDirectionBody();
        Vec3 forward = cockpitForward(nose);
        Vec3 right = forward.cross(nose).normalize();
        double imax = Math.max(inertia[0], Math.max(inertia[1], inertia[2]));
        double rate = gyro > 0 ? Math.min(MAX_RATE, gyro / imax) : 0.0;
        Vec3 cmd = right.scale(pitchIn * rate)
                .add(forward.scale(yawIn * rate))
                .subtract(nose.scale(rollIn * rate));
        double[] c = {cmd.x, cmd.y, cmd.z};

        double[] w = {angularVelocity.x, angularVelocity.y, angularVelocity.z};
        boolean hasInput = pitchIn != 0 || yawIn != 0 || rollIn != 0;
        if (hasInput || sas) {
            for (int i = 0; i < 3; i++) {
                double torque = Mth.clamp((c[i] - w[i]) * inertia[i] / TORQUE_TIME, -gyro, gyro);
                w[i] += torque / inertia[i] * DT;
            }
        }

        // JOML's integrate() applies a world-axis rate, so turn the body rate into dimension axes first.
        Vec3 worldRate = ShipGeometry.apply(ShipGeometry.matrix(orientation), new Vec3(w[0], w[1], w[2]));
        Quaternionf previous = new Quaternionf(orientation);
        orientation.integrate((float) DT, (float) worldRate.x, (float) worldRate.y, (float) worldRate.z);
        orientation.normalize();

        if (!env.orbital() && collidesWith(buildSamples(orientation), getX(), getY(), getZ())) {
            orientation.set(previous);
            w = new double[3];
        }
        angularVelocity = new Vec3(w[0], w[1], w[2]);
    }

    /** Diagonal inertia about the centre of mass (kg m^2) and the total gyro torque (N m). */
    private double[] inertiaAndGyro() {
        double ix = 0, iy = 0, iz = 0, gyro = 0;
        for (Map.Entry<BlockPos, BlockState> e : structure.blocks().entrySet()) {
            BlockPos p = e.getKey();
            double m = ShipParts.massKg(e.getValue()) + structure.getFuel(p);
            double dx = p.getX() + 0.5 - comLocal.x;
            double dy = p.getY() + 0.5 - comLocal.y;
            double dz = p.getZ() + 0.5 - comLocal.z;
            ix += m * (dy * dy + dz * dz) + m / 6.0;
            iy += m * (dx * dx + dz * dz) + m / 6.0;
            iz += m * (dx * dx + dy * dy) + m / 6.0;
            gyro += ShipParts.gyroTorqueNm(e.getValue());
        }
        return new double[] {Math.max(ix, 1.0), Math.max(iy, 1.0), Math.max(iz, 1.0), gyro};
    }

    /** Forward in body axes: the cockpit facing with the nose component removed (fallback +Z). */
    private Vec3 cockpitForward(Vec3 nose) {
        BlockPos c = structure.cockpit();
        Direction facing = c == null ? null : ShipGeometry.facingOf(structure.get(c));
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

    // ---- surface translation -------------------------------------------------------------------------------

    private void stepSurface(Environment env) {
        double mass = Math.max(massKg(), 1.0);
        double thrustN = effectiveThrustN(env.pressureFraction());
        Vec3 thrustAccel = rotate(thrustDirectionBody()).scale(thrustN / mass);
        double cdA = 0.5 * Math.pow(structure.size(), 2.0 / 3.0);
        double rho = env.airDensity();
        Vec3 g = env.gravity();
        Samples samples = buildSamples(orientation);

        double x = getX(), y = getY(), z = getZ();
        double vx = velocity.x, vy = velocity.y, vz = velocity.z;
        double speed0 = velocity.length();
        int steps = (int) Mth.clamp(Math.ceil(speed0 * DT / MAX_STEP_M), MIN_SUBSTEPS, MAX_SUBSTEPS);
        double h = DT / steps;
        boolean blockedBelow = false;

        for (int i = 0; i < steps; i++) {
            double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
            double drag = 0.5 * rho * speed * cdA / mass;
            vx += (g.x + thrustAccel.x - drag * vx) * h;
            vy += (g.y + thrustAccel.y - drag * vy) * h;
            vz += (g.z + thrustAccel.z - drag * vz) * h;

            // Y first, then X, then Z; each axis is reverted on contact.
            double ny = y + vy * h;
            if (collidesWith(samples, x, ny, z)) {
                if (vy < 0) {
                    blockedBelow = true;
                    if (vy < -HARD_LANDING_MPS) {
                        onHardLanding();
                    }
                }
                vy = 0;
            } else {
                y = ny;
            }
            double nx = x + vx * h;
            if (collidesWith(samples, nx, y, z)) {
                vx = 0;
            } else {
                x = nx;
            }
            double nz = z + vz * h;
            if (collidesWith(samples, x, y, nz)) {
                vz = 0;
            } else {
                z = nz;
            }
        }

        double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
        landed = blockedBelow && speed < 1.0;
        if (landed && thrustN < mass * g.length()) {
            vx *= 0.8;
            vz *= 0.8;
        }
        setPos(x, y, z);
        velocity = new Vec3(vx, vy, vz);
    }

    private void onHardLanding() {
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 1.0f, 0.6f);
        Player pilot = getPilot();
        if (pilot != null) {
            pilot.sendOverlayMessage(Component.translatable("message.apoapsis.ship.hard_landing"));
        }
    }

    /** Probe points of every block for one orientation, relative to the centre of mass. */
    private record Samples(double[] x, double[] y, double[] z) {
    }

    private Samples buildSamples(Quaternionfc q) {
        double[] m = ShipGeometry.matrix(q);
        int n = structure.size();
        double[] xs = new double[n * 9];
        double[] ys = new double[n * 9];
        double[] zs = new double[n * 9];
        Vec3[] corners = new Vec3[8];
        int ci = 0;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    corners[ci++] = ShipGeometry.apply(m, new Vec3(sx * PROBE, sy * PROBE, sz * PROBE));
                }
            }
        }
        int k = 0;
        for (BlockPos p : structure.blocks().keySet()) {
            Vec3 c = ShipGeometry.apply(m, new Vec3(p.getX() + 0.5 - comLocal.x, p.getY() + 0.5 - comLocal.y,
                    p.getZ() + 0.5 - comLocal.z));
            xs[k] = c.x;
            ys[k] = c.y;
            zs[k] = c.z;
            k++;
            for (Vec3 cr : corners) {
                xs[k] = c.x + cr.x;
                ys[k] = c.y + cr.y;
                zs[k] = c.z + cr.z;
                k++;
            }
        }
        return new Samples(xs, ys, zs);
    }

    /** True if any probe of the ship, with its centre of mass at (x, y, z), lies in a solid cell. */
    private boolean collidesWith(Samples s, double x, double y, double z) {
        if (!terrainCollision) {
            return false;
        }
        for (int k = 0; k < s.x().length; k++) {
            if (cellSolid(Mth.floor(x + s.x()[k]), Mth.floor(y + s.y()[k]), Mth.floor(z + s.z()[k]))) {
                return true;
            }
        }
        return false;
    }

    private boolean cellSolid(int x, int y, int z) {
        long key = BlockPos.asLong(x, y, z);
        Boolean cached = cellCache.get(key);
        if (cached != null) {
            return cached;
        }
        BlockPos pos = new BlockPos(x, y, z);
        boolean solid = false;
        if (level().hasChunkAt(pos)) {
            solid = !level().getBlockState(pos).getCollisionShape(level(), pos).isEmpty();
        }
        cellCache.put(key, solid);
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
        Vec3 nose = thrustDirectionBody();
        double total = 0;
        double ispWeighted = 0;
        for (BlockState state : structure.blocks().values()) {
            ShipParts.ThrusterSpec spec = ShipParts.thruster(state);
            if (spec == null) {
                continue;
            }
            Vec3i u = spec.exhaust().getUnitVec3i();
            Vec3 thrustDir = new Vec3(-u.getX(), -u.getY(), -u.getZ());
            if (thrustDir.dot(nose) < THRUST_CONE_COS) {
                continue;
            }
            double n = spec.thrustN(pressureFraction);
            total += n;
            ispWeighted += n * spec.isp(pressureFraction);
        }
        return new double[] {total, total > 0 ? ispWeighted / total : 0.0};
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
        BlockPos c = structure.cockpit();
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

    private boolean remote() {
        return level().isClientSide();
    }

    private void syncToClients() {
        entityData.set(ORIENTATION, new Quaternionf(orientation));
        entityData.set(VELOCITY, new Vector3f((float) velocity.x, (float) velocity.y, (float) velocity.z));
        entityData.set(THROTTLE, throttle);
        entityData.set(FUEL_KG, (float) structure.fuelKg());
        entityData.set(SAS, sas);
        entityData.set(LANDED, landed);
    }

    private static Vec3 toVec3(Vector3fc v) {
        return new Vec3(v.x(), v.y(), v.z());
    }
}
