package com.vroute.test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.vroute.models.Depot;
import com.vroute.models.Environment;
import com.vroute.solution.OrderStop;
import com.vroute.solution.Route;
import com.vroute.solution.RouteStop;
import com.vroute.solution.Solution;
import com.vroute.models.Position;
import com.vroute.taboo.RearrangeMove;

public class RearrangeMoveTest {
    public static TestFramework.TestSuite createSuite() {
        TestFramework.TestSuite suite = new TestFramework.TestSuite("RearrangeMove");
        suite.addTest(new RouteRearrangeTest());
        return suite;
    }

    private static class RouteRearrangeTest extends TestFramework.AbstractTest {
        @Override
        protected void runTest() throws Throwable {
            Environment env = TestUtilities.createSampleEnvironment();
            env.getMainDepot().refillGLP();
            for (Depot depot : env.getAuxDepots()) {
                depot.refillGLP();
            }

            LocalDateTime currentTime = env.getCurrentTime();
            // Create a route with 3 stops
            List<RouteStop> stops = new ArrayList<>();
            stops.add(new OrderStop("O1", new Position(0, 0), currentTime, 5));
            stops.add(new OrderStop("O2", new Position(5, 5), currentTime.plusMinutes(10), 10));
            stops.add(new OrderStop("O3", new Position(10, 10), currentTime.plusMinutes(20), 11));
            stops.add(new OrderStop("O4", new Position(15, 15), currentTime.plusMinutes(20), 8));
            stops.add(new OrderStop("O5", new Position(20, 20), currentTime.plusMinutes(20), 2));

            Route route1 = new Route(env.getVehicles().get(0), stops, currentTime);

            Solution solution = new Solution(null, Arrays.asList(route1));

            RearrangeMove rearrangeMove = new RearrangeMove();
            Solution newSolution = rearrangeMove.apply(env, solution);

            TestFramework.Assertions.assertEquals(newSolution.getRoutes().size(), 1,
                    "The solution should have 1 route");
            TestFramework.Assertions.assertTrue(newSolution.getRoutes().get(0).getStops().size() >= 5,
                    "The route should have at least 5 stops");

            List<RouteStop> newStops = newSolution.getRoutes().get(0).getStops();

            for (int i = 0; i < stops.size(); i++) {
                RouteStop originalStop = stops.get(i);

                boolean found = false;
                for (RouteStop newRouteStop : newStops) {
                    if (newRouteStop.getEntityID().equals(originalStop.getEntityID())) {
                        found = true;
                        break;
                    }
                }

                TestFramework.Assertions.assertTrue(found,
                        "The stop: " + originalStop.getEntityID() + " should be in the new solution");
            }

            // extract the order stops from the new solution
            List<OrderStop> newOrderStops = new ArrayList<>();
            for (RouteStop stop : newStops) {
                if (stop instanceof OrderStop) {
                    newOrderStops.add((OrderStop) stop);
                }
            }
        }
    }
}
