package dev.apoapsis.test;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.apoapsis.Apoapsis;
import dev.apoapsis.ages.Ages;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.flight.FlightDimensions;
import dev.apoapsis.grid.GridBlocks;
import dev.apoapsis.ownership.ShipOwnership;
import dev.apoapsis.scenario.Blueprint;
import dev.apoapsis.scenario.Scenarios;
import dev.apoapsis.ship.ShipEntity;
import dev.apoapsis.ship.ShipStructure;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.joml.Quaternionf;

/**
 * Test scenarios for the screenshot driver, permission level 2.
 * <ul>
 *     <li>{@code /apoapsis scenario lander}: the Kestrel with four parachutes and four landing legs, with empty tanks,
 *     1,200 m above sea level where the player stands, with the player in the cockpit. Used by the landing check
 *     ({@code apoapsis.testDriver=land}).</li>
 *     <li>{@code /apoapsis scenario lander burn <altitude>}: the same lander with its tanks full and no parachutes,
 *     spawned {@code altitude} metres above sea level (200 to 5,000 m), with the player in the cockpit.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Apoapsis.MODID)
public final class LanderCommand {
    /** Height above sea level at which the lander starts, metres. */
    private static final int START_ALTITUDE_M = 1200;
    /** Parachute positions in the blueprint frame: on the shoulders, one layer above the cockpit ring. */
    private static final int[][] CHUTES = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}};
    /** Landing leg positions in the blueprint frame: outside the fins, at the lowest layer. */
    private static final int[][] LEGS = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}};
    private static final int CHUTE_Y = 1;
    private static final int LEG_Y = -4;

    private LanderCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("apoapsis")
                .then(Commands.literal("scenario")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("lander")
                                .executes(c -> lander(c, START_ALTITUDE_M, true, true))
                                .then(Commands.literal("burn")
                                        .then(Commands.argument("altitude", IntegerArgumentType.integer(200, 5000))
                                                .executes(c -> lander(c, IntegerArgumentType.getInteger(c, "altitude"),
                                                        false, false))))
                                .then(Commands.literal("pale")
                                        .executes(c -> paleLander(c, 100))
                                        .then(Commands.argument("fuelPercent", IntegerArgumentType.integer(0, 100))
                                                .executes(c -> paleLander(c, IntegerArgumentType.getInteger(c, "fuelPercent"))))))));
    }

    /**
     * Stage start for the return-trip run: the lander (four chutes, four legs) standing on Pale's surface near x 0, z 0,
     * with the tanks filled to {@code fuelPercent} per cent, and the player in its cockpit. The player is moved to Pale.
     *
     * @param ctx         command context; the source must be a player
     * @param fuelPercent tank fill, 0 to 100
     * @return 1 on success, 0 when the blueprint or Pale cannot be loaded
     */
    private static int paleLander(CommandContext<CommandSourceStack> ctx, int fuelPercent) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        MinecraftServer server = source.getServer();
        ServerLevel pale = server.getLevel(FlightDimensions.PALE);
        if (pale == null) {
            source.sendFailure(Component.literal("Pale is not loaded."));
            return 0;
        }
        ShipStructure structure;
        try {
            structure = landerStructure(server, true, false);
        } catch (IllegalStateException e) {
            source.sendFailure(Component.translatable("apoapsis.scenario.blueprint.failed"));
            return 0;
        }
        for (BlockPos tank : new ArrayList<>(structure.fuel().keySet())) {
            structure.setFuel(tank, structure.fuel().get(tank) * fuelPercent / 100.0);
        }
        // The highest ground under the 7 x 7 footprint decides the height, so no leg starts inside rock. The blocks are
        // scanned, not read from the heightmaps: they are what the ship collides with.
        int x = 0;
        int z = 0;
        // Pale has no player in it yet: load the chunks under the footprint before reading their blocks.
        for (int cx = -1; cx <= 0; cx++) {
            for (int cz = -1; cz <= 0; cz++) {
                pale.getChunk(cx, cz);
            }
        }
        int ground = Integer.MIN_VALUE;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                ground = Math.max(ground, topCollidingY(pale, x + dx, z + dz));
            }
        }
        double originY = ground - LEG_Y + 0.5;
        Vec3 com = structure.centerOfMassLocal();
        Vec3 comPos = new Vec3(x + com.x, originY + com.y, z + com.z);
        if (player.getVehicle() instanceof ShipEntity old) {
            player.stopRiding();
            old.discard();
        }
        ShipEntity ship = ShipEntity.spawn(pale, structure, comPos, new Quaternionf());
        ShipOwnership.claim(ship, player);
        player.teleportTo(pale, ship.getX(), ship.getY(), ship.getZ(), Set.of(), player.getYRot(), player.getXRot(), true);
        Scenarios.runNextTick(() -> {
            try {
                ship.seatPilot(player);
            } catch (RuntimeException e) {
                Apoapsis.LOG.warn("Could not seat the pilot in the Pale lander scenario", e);
            }
        });
        Ages.setAge(player, Math.max(Ages.getAge(player), 4), false);
        source.sendSuccess(() -> Component.literal("Lander on Pale, tanks " + fuelPercent + " per cent"), false);
        return 1;
    }

    /**
     * Spawns the lander {@code altitude} metres above sea level, above the player, and seats the player in its cockpit.
     *
     * @param ctx        command context; the source must be a player
     * @param altitude   spawn height above sea level, metres
     * @param chutes     true to fit the four parachutes
     * @param emptyTanks true to empty the tanks, false to keep their fuel
     * @return 1 on success, 0 when the blueprint cannot be loaded
     */
    private static int lander(CommandContext<CommandSourceStack> ctx, int altitude, boolean chutes, boolean emptyTanks)
            throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ShipStructure structure;
        try {
            structure = landerStructure(source.getServer(), chutes, emptyTanks);
        } catch (IllegalStateException e) {
            source.sendFailure(Component.translatable("apoapsis.scenario.blueprint.failed"));
            return 0;
        }

        double originY = FlightDimensions.seaLevel(BodyId.VERDANCE) + altitude;
        Vec3 com = structure.centerOfMassLocal();
        Vec3 comPos = new Vec3(Mth.floor(player.getX()) + com.x, originY + com.y, Mth.floor(player.getZ()) + com.z);
        if (player.getVehicle() instanceof ShipEntity old) {
            player.stopRiding();
            old.discard();
        }
        ShipEntity ship = ShipEntity.spawn(player.level(), structure, comPos, new Quaternionf());
        ShipOwnership.claim(ship, player);
        player.teleportTo(ship.getX(), ship.getY(), ship.getZ());
        Scenarios.runNextTick(() -> {
            try {
                ship.seatPilot(player);
            } catch (RuntimeException e) {
                Apoapsis.LOG.warn("Could not seat the pilot in the lander scenario", e);
            }
        });
        Ages.setAge(player, Math.max(Ages.getAge(player), 4), false);
        String text = String.format(Locale.ROOT, "%,d", altitude) + " m up, " + (chutes ? "chutes on P" : "no chutes")
                + (emptyTanks ? "" : ", tanks full");
        source.sendSuccess(() -> Component.literal("Lander ready: " + text), false);
        return 1;
    }

    /**
     * The y just above the highest block with a collision shape in a column, found by scanning the blocks the way a ship's
     * probes see them, so the answer does not depend on the chunk's heightmaps.
     */
    private static int topCollidingY(ServerLevel level, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = level.getMaxY(); y >= level.getMinY(); y--) {
            pos.set(x, y, z);
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return y + 1;
            }
        }
        return level.getMinY();
    }

    /**
     * Builds a fresh lander structure from the Kestrel blueprint.
     *
     * @param server     server that holds the blueprint
     * @param chutes     true to add the four parachutes
     * @param emptyTanks true to set every tank to zero fuel
     * @return the structure with the four landing legs, and the parachutes and empty tanks as asked
     * @throws IllegalStateException if the kestrel blueprint cannot be loaded
     */
    public static ShipStructure landerStructure(MinecraftServer server, boolean chutes, boolean emptyTanks) {
        ShipStructure structure = Blueprint.load(server, "kestrel").toStructure();
        if (emptyTanks) {
            for (BlockPos tank : new ArrayList<>(structure.fuel().keySet())) {
                structure.setFuel(tank, 0);
            }
        }
        if (chutes) {
            for (int[] c : CHUTES) {
                structure.put(new BlockPos(c[0], CHUTE_Y, c[1]), GridBlocks.PARACHUTE.get().defaultBlockState());
            }
        }
        for (int[] l : LEGS) {
            structure.put(new BlockPos(l[0], LEG_Y, l[1]), GridBlocks.LANDING_LEG.get().defaultBlockState());
        }
        return structure;
    }
}
