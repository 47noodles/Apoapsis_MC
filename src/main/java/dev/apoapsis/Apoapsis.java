package dev.apoapsis;

import com.mojang.logging.LogUtils;
import dev.apoapsis.ages.Ages;
import dev.apoapsis.flight.Flight;
import dev.apoapsis.grid.GridBlocks;
import dev.apoapsis.net.ApNetwork;
import dev.apoapsis.scenario.Scenarios;
import dev.apoapsis.ship.ShipEntities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/** Mod entry point. Each module registers itself; see docs/dev/prototype-contract.md. */
@Mod(Apoapsis.MODID)
public final class Apoapsis {
    public static final String MODID = "apoapsis";
    public static final Logger LOG = LogUtils.getLogger();

    public Apoapsis(IEventBus modBus, ModContainer container) {
        GridBlocks.register(modBus);
        ShipEntities.register(modBus);
        Ages.register(modBus);
        Flight.register(modBus);
        Scenarios.register(modBus);
        ApNetwork.register(modBus);
    }
}
