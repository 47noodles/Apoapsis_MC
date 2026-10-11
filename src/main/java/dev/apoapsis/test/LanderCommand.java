package dev.apoapsis.test;

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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.joml.Quaternionf;

/**
 * Test scenario {@code /apoapsis scenario lander}: the Kestrel with four parachutes and four landing legs, with empty
 * tanks, 1,200 m above where the player stands, with the player in the cockpit. Used by the landing check
 * ({@code apoapsis.testDriver=land}). Permission level 2.
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
                        .then(Commands.literal("lander").executes(LanderCommand::lander))));
    }

    private static int lander(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        Blueprint blueprint;
        try {
            blueprint = Blueprint.load(source.getServer(), "kestrel");
        } catch (IllegalStateException e) {
            source.sendFailure(Component.translatable("apoapsis.scenario.blueprint.failed"));
            return 0;
        }
        ShipStructure structure = blueprint.toStructure();
        for (BlockPos tank : new ArrayList<>(structure.fuel().keySet())) {
            structure.setFuel(tank, 0);
        }
        for (int[] c : CHUTES) {
            structure.put(new BlockPos(c[0], CHUTE_Y, c[1]), GridBlocks.PARACHUTE.get().defaultBlockState());
        }
        for (int[] l : LEGS) {
            structure.put(new BlockPos(l[0], LEG_Y, l[1]), GridBlocks.LANDING_LEG.get().defaultBlockState());
        }

        double originY = FlightDimensions.seaLevel(BodyId.VERDANCE) + START_ALTITUDE_M;
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
        source.sendSuccess(() -> Component.literal("Lander ready: 1,200 m up, chutes on P"), false);
        return 1;
    }
}
