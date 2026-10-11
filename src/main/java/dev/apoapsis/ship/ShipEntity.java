package dev.apoapsis.ship;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

/**
 * A moving ship: a {@link ShipStructure} with a pose (position of the centre of mass, orientation) and dynamics.
 * STUB from S0; node SHIP implements it. Public signatures are fixed by docs/dev/prototype-contract.md.
 * Units: metres, seconds, kilograms. Orientation rotates ship-local (body) axes into the current dimension's axes.
 */
public class ShipEntity extends Entity implements IEntityWithComplexSpawn {
    private ShipStructure structure = new ShipStructure();
    private final Quaternionf orientation = new Quaternionf();
    private Vec3 velocity = Vec3.ZERO;
    private Vec3 angularVelocity = Vec3.ZERO;
    private float throttle;
    private boolean sas = true;

    public ShipEntity(EntityType<? extends ShipEntity> type, Level level) {
        super(type, level);
    }

    /** Spawns a ship whose centre of mass is at {@code comPos}. */
    public static ShipEntity spawn(ServerLevel level, ShipStructure structure, Vec3 comPos, Quaternionfc orientation) {
        ShipEntity ship = new ShipEntity(ShipEntities.SHIP.get(), level);
        ship.setStructure(structure);
        ship.setOrientation(orientation);
        ship.setPos(comPos);
        level.addFreshEntity(ship);
        return ship;
    }

    public ShipStructure getStructure() {
        return structure;
    }

    public void setStructure(ShipStructure structure) {
        this.structure = structure;
    }

    /** A copy of the orientation (body to dimension axes). */
    public Quaternionf getOrientation() {
        return new Quaternionf(orientation);
    }

    public void setOrientation(Quaternionfc q) {
        orientation.set(q);
    }

    /** Linear velocity in m/s, dimension axes. Meaningful in the SURFACE regime. */
    public Vec3 getVelocity() {
        return velocity;
    }

    public void setVelocity(Vec3 v) {
        velocity = v;
    }

    /** Angular velocity in rad/s, body axes. */
    public Vec3 getAngularVelocity() {
        return angularVelocity;
    }

    public void setAngularVelocity(Vec3 w) {
        angularVelocity = w;
    }

    /** 0..1. Synced to clients. */
    public float getThrottle() {
        return throttle;
    }

    public void setThrottle(float t) {
        throttle = Math.max(0, Math.min(1, t));
    }

    public boolean isSasOn() {
        return sas;
    }

    public void setSas(boolean on) {
        sas = on;
    }

    /** Dry mass plus fuel, kg. */
    public double massKg() {
        return structure.dryMassKg() + structure.fuelKg();
    }

    /** Fuel on board, kg. Synced to clients. */
    public double fuelKg() {
        return structure.fuelKg();
    }

    public double fuelCapacityKg() {
        return structure.fuelCapacityKg();
    }

    /** Removes up to {@code kg} of fuel spread across tanks; returns the amount removed. */
    public double drainFuel(double kg) {
        return 0;
    }

    /** Adds fuel up to capacity; returns the amount added. */
    public double refuel(double kg) {
        return 0;
    }

    /** Thrust direction (unit vector, body axes): opposite the exhaust of the engines. Default +Y. */
    public Vec3 thrustDirectionBody() {
        return new Vec3(0, 1, 0);
    }

    /** Total thrust in N at full throttle along {@link #thrustDirectionBody()}, at pressure fraction p/p0. */
    public double maxThrustN(double pressureFraction) {
        return 0;
    }

    /** Effective Isp of the engines, seconds. */
    public double ispSeconds(double pressureFraction) {
        return 0;
    }

    /** Remaining delta-v in m/s at pressure fraction p/p0 (rocket equation over the fuel on board). */
    public double deltaV(double pressureFraction) {
        return 0;
    }

    /** True when resting on terrain (SURFACE regime only). */
    public boolean isLanded() {
        return false;
    }

    /** The pilot (first passenger that is a player), or null. */
    public Player getPilot() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    /** Seats a player in the cockpit (server side). */
    public void seatPilot(Player player) {
        player.startRiding(this, true, false);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        ShipStructure.STREAM_CODEC.encode(buffer, structure);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buffer) {
        structure = ShipStructure.STREAM_CODEC.decode(buffer);
    }
}
