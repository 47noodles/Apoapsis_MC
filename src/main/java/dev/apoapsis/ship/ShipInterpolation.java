package dev.apoapsis.ship;

import net.minecraft.core.PositionAndRotation;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.PositionPath;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

/**
 * Client-side smoothing of a ship's pose, wired into the entity as its {@link InterpolationHandler}. Common code, but
 * only a client level creates one; the server moves the ship itself.
 *
 * <p>Why it exists: the server sends one position and one data packet per server tick, and the client applies packets
 * once per frame, before its own ticks. A plain entity therefore draws the newest packet's position, a staircase whose
 * steps are as large as one tick of flight (6 m at 120 m/s) and whose timing follows the packet arrivals. The cure
 * (see {@link ShipPlayback}) is to take the pose out of the arrival timing altogether: every server tick the ship syncs a
 * tick stamp, which with the position of the move packet before it, the velocity and the orientation makes one exact
 * sample of the flight, and each client tick the ship is put on a smooth curve through those samples at a playhead that
 * advances one server tick per client tick. The entity's ordinary partial-tick lerp of its old and new position then
 * draws the frames in between, and the rider, who is placed from this pose, follows it.
 *
 * <p>This class only connects the two: move packets arrive at {@link #interpolateTo}, the stamped data at
 * {@link #accept}, and {@link #interpolate} runs once per client tick, after the entity's old pose was set. A position
 * set by anything other than this class (a snap for a long jump, a teleport) restarts the playback from it.
 * The server still decides everything: nothing here is sent back, and the samples are exactly the server's states.
 */
public final class ShipInterpolation implements InterpolationHandler {
    private static final double EPS_SQ = 1.0e-10;

    private final ShipEntity ship;
    private final ShipPlayback playback = new ShipPlayback();
    /** The position of the latest move packet (or the spawn position); a sample takes it with its stamp. */
    private @Nullable Vec3 authority;
    /** The position this class last gave the entity, to notice anyone else moving it. */
    private @Nullable Vec3 shown;

    ShipInterpolation(ShipEntity ship) {
        this.ship = ship;
    }

    /** True once a sample has arrived: the orientation is the smoothed one. */
    public boolean hasOrientation() {
        return playback.hasOrientation();
    }

    /** The smoothed orientation as of the latest client tick. */
    public Quaternionf orientationNow(Quaternionf dest) {
        return playback.orientationNow(dest);
    }

    /** The smoothed orientation {@code partial} of the way from the previous client tick to the latest. */
    public Quaternionf orientationAt(float partial, Quaternionf dest) {
        return playback.orientationAt(partial, dest);
    }

    /** Lag of the drawn pose behind the newest sample, in server ticks; for tests and logs. */
    public double lagTicks() {
        return playback.lagTicks();
    }

    /** Server tick of the newest sample, or -1 before the first; for tests and logs. */
    public int newestStamp() {
        return playback.newestStamp();
    }

    // ---- InterpolationHandler -------------------------------------------------------------------------------

    @Override
    public @Nullable PositionAndRotation target() {
        return null;
    }

    /** A move packet: remember its position. The sample is made when the tick stamp of the same tick follows. */
    @Override
    public boolean interpolateTo(@Nullable PositionPath position, float yRot, float xRot, boolean hasRotation) {
        if (position != null) {
            authority = position.endPosition();
        }
        return true;
    }

    @Override
    public void applyPredictedMovement(Vec3 delta) {
    }

    @Override
    public boolean hasActiveInterpolation() {
        return playback.isRunning();
    }

    @Override
    public void cancel() {
    }

    /** One client tick, after the entity's old pose was set: move the playhead and put the ship on the curve. */
    @Override
    public void interpolate() {
        if (!playback.isRunning()) {
            return;
        }
        Vec3 here = ship.position();
        if (shown != null && here.distanceToSqr(shown) > EPS_SQ) {
            // Something else placed the entity: its position is the truth now, and the next stamp starts again.
            authority = here;
            shown = here;
            playback.reset();
            return;
        }
        playback.tick();
        place();
    }

    /**
     * The tick stamp arrived with its data: the velocity and orientation of server tick {@code serverTick}, and the
     * position of the move packet that came before them.
     */
    void accept(int serverTick, Vec3 velocity, Quaternionfc orientation) {
        Vec3 pos = authority != null ? authority : ship.position();
        authority = pos;
        if (playback.accept(serverTick, pos, velocity, orientation)) {
            place();
            ship.setOldPosAndRot();
        }
    }

    private void place() {
        ship.setPos(playback.x(), playback.y(), playback.z());
        shown = ship.position();
    }
}
