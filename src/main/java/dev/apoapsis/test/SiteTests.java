package dev.apoapsis.test;

import dev.apoapsis.client.map3d.MapSites;
import dev.apoapsis.client.map3d.MapSites.Site;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.orbit.Vec3d;
import dev.apoapsis.test.ApoapsisGameTest.ShipCleanup;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** Tests of {@link MapSites}, the 3D map's home and landing markers. No client needed. */
final class SiteTests {
    private SiteTests() {}

    /** Registers the tests. */
    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        event.registerTest(TestSupport.id("sites_conversions"),
                new ApoapsisGameTest("sites_conversions", environment, 40, SiteTests::conversions));
        event.registerTest(TestSupport.id("sites_record_landing"),
                new ApoapsisGameTest("sites_record_landing", environment, 40, SiteTests::recordLanding));
        event.registerTest(TestSupport.id("sites_roundtrip_text"),
                new ApoapsisGameTest("sites_roundtrip_text", environment, 40, SiteTests::roundtrip));
    }

    private static void conversions(GameTestHelper helper, ShipCleanup ships) {
        Site origin = MapSites.fromInertial(new Vec3d(10, 0, 0));
        helper.assertTrue(origin.lat() == 0 && origin.lon() == 0, "(10, 0, 0) is not lat 0 lon 0");
        helper.assertTrue(Math.abs(MapSites.fromInertial(new Vec3d(0, 0, 5)).lat() - Math.PI / 2) < 1e-9,
                "the north pole is not lat pi/2");
        Site s = MapSites.fromSurface(64000 * 0.5, -64000 * 0.25, 64000);
        helper.assertTrue(Math.abs(s.lon() - 0.5) < 1e-9 && Math.abs(s.lat() - 0.25) < 1e-9,
                "the surface convention is wrong: " + s);
        Vec3d r = new Vec3d(3, 4, 12);
        helper.assertTrue(MapSites.unit(MapSites.fromInertial(r)).sub(r.scale(1 / r.length())).length() < 1e-9,
                "unit(fromInertial(r)) is not r / |r|");
        Site a = new Site(0, 0);
        helper.assertTrue(MapSites.separation(a, a, 64000) == 0, "the separation of a site from itself is not 0");
        helper.assertTrue(Math.abs(MapSites.separation(a, new Site(0, Math.PI / 2), 64000) - Math.PI / 2 * 64000) < 1,
                "a quarter turn is not a quarter circumference");
        helper.succeed();
    }

    private static void recordLanding(GameTestHelper helper, ShipCleanup ships) {
        MapSites sites = new MapSites();
        Site first = MapSites.fromSurface(0, 0, 64000);
        helper.assertTrue(sites.recordLanding(BodyId.VERDANCE, first), "the first landing was not stored");
        helper.assertTrue(!sites.recordLanding(BodyId.VERDANCE, MapSites.fromSurface(50, 0, 64000)),
                "a landing 50 m away replaced the site");
        helper.assertTrue(sites.landing(BodyId.VERDANCE) == first, "the site moved on a landing 50 m away");
        helper.assertTrue(sites.recordLanding(BodyId.VERDANCE, MapSites.fromSurface(500, 0, 64000)),
                "a landing 500 m away was not stored");
        helper.assertTrue(sites.landing(BodyId.PALE) == null, "Pale has a landing it never had");
        helper.succeed();
    }

    private static void roundtrip(GameTestHelper helper, ShipCleanup ships) {
        MapSites sites = new MapSites();
        sites.setHome(new Site(0.1, 0.2));
        sites.recordLanding(BodyId.VERDANCE, new Site(0.3, 0.4));
        sites.recordLanding(BodyId.PALE, new Site(-0.5, 1.5));
        MapSites back = MapSites.fromLines(sites.toLines());
        helper.assertTrue(new Site(0.1, 0.2).equals(back.home()), "home did not survive the text form");
        helper.assertTrue(new Site(0.3, 0.4).equals(back.landing(BodyId.VERDANCE)), "Verdance's landing did not survive");
        helper.assertTrue(new Site(-0.5, 1.5).equals(back.landing(BodyId.PALE)), "Pale's landing did not survive");
        MapSites messy = MapSites.fromLines(List.of("", "# note", "landing nowhere 1 2", "landing verdance x y",
                "home 0.1 0.2"));
        helper.assertTrue(new Site(0.1, 0.2).equals(messy.home()) && messy.landing(BodyId.VERDANCE) == null
                && messy.landing(BodyId.PALE) == null, "malformed lines were not ignored");
        helper.succeed();
    }
}
