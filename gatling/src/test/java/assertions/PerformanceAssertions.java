package assertions;

import io.gatling.javaapi.core.Assertion;
import java.util.List;
import static io.gatling.javaapi.core.CoreDsl.*;

public class PerformanceAssertions {

    public static final String SMOKE_TEST = "smokeTest";
    public static final String ECOMMERCE_ORDER = "ecommerceOrder";

    private static final int P95_LIMIT_MS = 3000;

    public static List<Assertion> smokeTestAssertions = List.of(
            global().successfulRequests().percent().gte(99.0)
    );

    public static List<Assertion> ecommerceOrderAssertions = List.of(
            details("001_HomePage").responseTime().percentile(95.0).lte(P95_LIMIT_MS),
            details("002_Category").responseTime().percentile(95.0).lte(P95_LIMIT_MS),
            details("003_Tables").responseTime().percentile(95.0).lte(P95_LIMIT_MS),
            details("004_Tables_addToCart").responseTime().percentile(95.0).lte(P95_LIMIT_MS),
            details("005_Category").responseTime().percentile(95.0).lte(P95_LIMIT_MS),
            details("006_Chairs").responseTime().percentile(95.0).lte(P95_LIMIT_MS),
            details("007_Chairs_addToCart").responseTime().percentile(95.0).lte(P95_LIMIT_MS),
            details("008_Cart").responseTime().percentile(95.0).lte(P95_LIMIT_MS),
            details("009_Checkout").responseTime().percentile(95.0).lte(P95_LIMIT_MS),
            details("010_Choosing_Country").responseTime().percentile(95.0).lte(P95_LIMIT_MS),
            details("011_Place_Order").responseTime().percentile(95.0).lte(P95_LIMIT_MS)
    );

    private PerformanceAssertions() {}
}