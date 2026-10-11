package dev.apoapsis.test;

import dev.apoapsis.net.TerrainTilePayload;
import dev.apoapsis.planet.TerrainTiles;
import dev.apoapsis.ship.ShipEntity;
import dev.apoapsis.test.ApoapsisGameTest.ShipCleanup;
import java.lang.reflect.Proxy;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Who may ask the server for far-terrain tiles. */
final class TerrainGateTests {
    private TerrainGateTests() {}

    /** A payload context that knows only who sent the packet; the request handler asks for nothing else. */
    private static IPayloadContext from(ServerPlayer player) {
        return (IPayloadContext) Proxy.newProxyInstance(IPayloadContext.class.getClassLoader(),
                new Class<?>[] {IPayloadContext.class}, (proxy, method, args) -> {
                    if (method.getName().equals("player")) {
                        return player;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    /**
     * Only the pilot of a ship, or a player in the orbit dimension, gets a tile request through. A walker's request is
     * dropped without an error (the handler returns and the server keeps no state for them); the pilot's is accepted.
     */
    @SuppressWarnings("removal")
    static void requestsNeedAPilot(GameTestHelper helper, ShipCleanup ships) {
        ServerPlayer walker = helper.makeMockServerPlayerInLevel();
        ServerPlayer pilot = helper.makeMockServerPlayerInLevel();
        ShipEntity ship = ships.track(TestSupport.spawnAboveFloor(helper, TestSupport.threeBlockShip(helper, 600.0), 6));
        ship.seatPilot(pilot);
        helper.assertTrue(ship.getPilot() == pilot, "the pilot was not seated in the ship");

        helper.assertTrue(TerrainTiles.mayRequest(pilot), "the pilot of a ship may not ask for far terrain");
        helper.assertTrue(!TerrainTiles.mayRequest(walker), "a player on foot may ask for far terrain");
        helper.assertTrue(TerrainTiles.mayRequest(true, walker), "a player in the orbit dimension may not ask");
        helper.assertTrue(!TerrainTiles.mayRequest(false, walker), "a player outside orbit and a ship may ask");

        TerrainTilePayload.Request request = new TerrainTilePayload.Request("verdance", 0, 0, 0);
        TerrainTiles.handleRequest(request, from(walker));
        helper.assertTrue(!TerrainTiles.hasRequested(walker.getUUID()),
                "a request from a player on foot got past the gate");
        // The same walker, in the orbit regime, would be heard; the pure gate above covers that, because the orbit
        // dimension is not loaded in the GameTest world.
        TerrainTiles.handleRequest(request, from(pilot));
        helper.assertTrue(TerrainTiles.hasRequested(pilot.getUUID()),
                "the pilot's request was not accepted (is the server's terrain pool running?)");
        helper.succeed();
    }
}
