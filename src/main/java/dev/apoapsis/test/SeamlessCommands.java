package dev.apoapsis.test;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.apoapsis.Apoapsis;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.flight.Flight;
import dev.apoapsis.scenario.Blueprint;
import dev.apoapsis.scenario.Scenarios;
import dev.apoapsis.ship.ShipEntity;
import dev.apoapsis.ship.ShipStructure;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Test commands for the seamless-descent screenshots. {@code /apoapsis seamless orbit <verdance|pale> <altitude m>
 * <phase deg> <yaw> <pitch>} puts a Kestrel in a circular equatorial orbit of that body at that phase (degrees from
 * +X, so longitude) and seats the caller in it, looking along the given vanilla yaw and pitch.
 * {@code /apoapsis seamless dive <altitude m> <speed m/s> <path deg> <plain|chutes>} puts a Kestrel above Verdance
 * moving at that speed, with its velocity {@code path deg} below prograde (toward the body) and its nose retrograde,
 * and seats the caller in it looking along yaw 0, pitch 0. {@code plain} keeps the Kestrel's fuel; {@code chutes} is
 * the lander, with empty tanks, four parachutes and four landing legs. Permission level 2.
 */
@EventBusSubscriber(modid = Apoapsis.MODID)
public final class SeamlessCommands {
    private SeamlessCommands() {}

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("apoapsis")
                .then(Commands.literal("seamless")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("orbit")
                                .then(Commands.literal("verdance").then(args(BodyId.VERDANCE)))
                                .then(Commands.literal("pale").then(args(BodyId.PALE))))
                        .then(Commands.literal("dive").then(diveArgs()))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, Double> diveArgs() {
        return Commands.argument("altitude", DoubleArgumentType.doubleArg(100, 100_000))
                .then(Commands.argument("speed", DoubleArgumentType.doubleArg(1, 5000))
                        .then(Commands.argument("pathDeg", DoubleArgumentType.doubleArg(0, 89))
                                .then(Commands.literal("plain").executes(c -> dive(c, false)))
                                .then(Commands.literal("chutes").executes(c -> dive(c, true)))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, Double> args(BodyId body) {
        return Commands.argument("altitude", DoubleArgumentType.doubleArg(100, 100_000))
                .then(Commands.argument("phase", DoubleArgumentType.doubleArg(-360, 360))
                        .then(Commands.argument("yaw", DoubleArgumentType.doubleArg(-360, 360))
                                .then(Commands.argument("pitch", DoubleArgumentType.doubleArg(-90, 90))
                                        .executes(c -> orbit(c, body)))));
    }

    private static int orbit(CommandContext<CommandSourceStack> c, BodyId body) throws CommandSyntaxException {
        CommandSourceStack source = c.getSource();
        ServerPlayer player = source.getPlayerOrException();
        MinecraftServer server = source.getServer();
        Blueprint blueprint = Blueprint.load(server, "kestrel");
        double altitude = DoubleArgumentType.getDouble(c, "altitude");
        double phase = DoubleArgumentType.getDouble(c, "phase");
        float yaw = (float) DoubleArgumentType.getDouble(c, "yaw");
        float pitch = (float) DoubleArgumentType.getDouble(c, "pitch");
        ShipEntity ship = Flight.spawnInOrbit(server, blueprint.toStructure(), body, altitude, phase);
        if (ship == null) {
            source.sendFailure(Component.literal("Could not spawn the ship in orbit."));
            return 0;
        }
        ServerLevel orbit = (ServerLevel) ship.level();
        player.stopRiding();
        player.teleportTo(orbit, ship.getX(), ship.getY(), ship.getZ(), Set.of(), yaw, pitch, true);
        Scenarios.runNextTick(() -> {
            try {
                ship.seatPilot(player);
            } catch (RuntimeException e) {
                Apoapsis.LOG.warn("Could not seat the pilot", e);
            }
        });
        source.sendSuccess(() -> Component.literal("In orbit of " + body.id + " at " + altitude + " m."), false);
        return 1;
    }

    private static int dive(CommandContext<CommandSourceStack> c, boolean chutes) throws CommandSyntaxException {
        CommandSourceStack source = c.getSource();
        ServerPlayer player = source.getPlayerOrException();
        MinecraftServer server = source.getServer();
        double altitude = DoubleArgumentType.getDouble(c, "altitude");
        double speed = DoubleArgumentType.getDouble(c, "speed");
        double pathDeg = DoubleArgumentType.getDouble(c, "pathDeg");
        ShipStructure structure = chutes
                ? LanderCommand.landerStructure(server, true, true)
                : Blueprint.load(server, "kestrel").toStructure();
        ShipEntity ship = Flight.spawnInOrbit(server, structure, BodyId.VERDANCE, altitude, 0, speed, pathDeg, true);
        if (ship == null) {
            source.sendFailure(Component.literal("Could not spawn the ship in orbit."));
            return 0;
        }
        ServerLevel orbit = (ServerLevel) ship.level();
        player.stopRiding();
        player.teleportTo(orbit, ship.getX(), ship.getY(), ship.getZ(), Set.of(), 0f, 0f, true);
        Scenarios.runNextTick(() -> {
            try {
                ship.seatPilot(player);
            } catch (RuntimeException e) {
                Apoapsis.LOG.warn("Could not seat the pilot", e);
            }
        });
        String kind = chutes ? "chutes" : "plain";
        source.sendSuccess(() -> Component.literal("Dive from " + altitude + " m at " + speed + " m/s, " + pathDeg
                + " deg down, " + kind + "."), false);
        return 1;
    }
}
