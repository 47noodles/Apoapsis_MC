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
 * Test commands for the seamless-descent screenshots: {@code /apoapsis seamless orbit <verdance|pale> <altitude m>
 * <phase deg> <yaw> <pitch>} puts a Kestrel in a circular equatorial orbit of that body at that phase (degrees from
 * +X, so longitude) and seats the caller in it, looking along the given vanilla yaw and pitch. Permission level 2.
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
                                .then(Commands.literal("pale").then(args(BodyId.PALE))))));
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
}
