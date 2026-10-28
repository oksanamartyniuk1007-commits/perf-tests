package config;

import io.gatling.javaapi.http.HttpProtocolBuilder;
import static io.gatling.javaapi.http.HttpDsl.*;

public class BaseHelpers {

    public static final String BASE_URL = System.getProperty("BASE_URL", "http://localhost");
    public static final String CART_WIDGET_DISABLED = "0";
    public static final String CART_CONTAINER_DISABLED = "0";

    public static final HttpProtocolBuilder HTTP_PROTOCOL = http
            .baseUrl(BASE_URL)
            .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .acceptEncodingHeader("gzip, deflate")
            .acceptLanguageHeader("en-US,en;q=0.5")
            .upgradeInsecureRequestsHeader("1")
            .userAgentHeader(
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            );

    public static final String ADMIN_AJAX = "/wp-admin/admin-ajax.php"; // Replace with actual implementation in TASK3

    public static Double CHAIRS_PERCENTAGE = Double.parseDouble(
            System.getProperty("CHAIRS_PERCENTAGE", "50"));
    public static int MIN_THINK_TIME = Integer.parseInt(
            System.getProperty("MIN_THINK_TIME", "2"));
    public static int MAX_THINK_TIME = Integer.parseInt(
            System.getProperty("MAX_THINK_TIME", "5"));
    public static int USER_COUNT = Integer.parseInt(
            System.getProperty("USER_COUNT", "100"));
    public static int RAMP_UP_PERIOD = Integer.parseInt(
            System.getProperty("RAMP_UP_PERIOD", "20"));

    public static Double CART_PERCENTAGE = Double.parseDouble(
            System.getProperty("CART_PERCENTAGE", "30"));

    public static String ASSERTION_TYPE = System.getProperty(
            "ASSERTION_TYPE", "smokeTest"
    );
}
