package simulations;

import static config.BaseHelpers.*;
import static io.gatling.javaapi.core.CoreDsl.*;
import static scenarios.UserFlow.userJourney;

import io.gatling.javaapi.core.Simulation;
import java.time.Duration;

/*
 * Run (Linux / Jenkins agent):
 * ./mvnw gatling:test -Dgatling.simulationClass=simulations.PerfTestSimulation \
 *   -DUSER_COUNT=3 -DRAMP_UP_PERIOD=60 -DTEST_DURATION=600 -DBASE_URL=http://localhost
 *
 * TEST_DURATION - total test time in seconds (ramp-up included).
 */
public class PerfTestSimulation extends Simulation {

    private static final int TEST_DURATION =
            Integer.parseInt(System.getProperty("TEST_DURATION", "600"));

    private static final int STEADY_STATE =
            Math.max(1, TEST_DURATION - RAMP_UP_PERIOD);

    {
        setUp(
                userJourney(CHAIRS_PERCENTAGE, CART_PERCENTAGE).injectClosed(
                        rampConcurrentUsers(1).to(USER_COUNT).during(Duration.ofSeconds(RAMP_UP_PERIOD)),
                        constantConcurrentUsers(USER_COUNT).during(Duration.ofSeconds(STEADY_STATE))
                )
        ).protocols(HTTP_PROTOCOL)
                .maxDuration(Duration.ofSeconds(TEST_DURATION + 120));
    }
}
