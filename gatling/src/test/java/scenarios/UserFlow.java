package scenarios;

import static config.BaseHelpers.MAX_THINK_TIME;
import static config.BaseHelpers.MIN_THINK_TIME;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import api.Navigation;
import static api.Navigation.*;
import io.gatling.javaapi.core.ScenarioBuilder;

public class UserFlow {

    public static ScenarioBuilder userJourney(Double chairs, Double cart) {
        return scenario("E-commerce End-to-End Journey")
                .exec(flushHttpCache())
                .exec(flushCookieJar())
                .exitBlockOnFail().on(
                        exec(Navigation.navigateToHomePage("001_HomePage"))
                                .pause(MIN_THINK_TIME, MAX_THINK_TIME)
                                .exec(Navigation.navigateToCategory("002_Category", "tables"))
                                .pause(MIN_THINK_TIME, MAX_THINK_TIME)
                                .exec(Navigation.navigateToProduct("003_Tables", "tables"))
                                .pause(MIN_THINK_TIME, MAX_THINK_TIME)
                                .exec(Navigation.addProductToCart("004_Tables", 1))
                                .pause(MIN_THINK_TIME, MAX_THINK_TIME)
                                .randomSwitch().on(
                                        percent(chairs).then(
                                                exec(Navigation.navigateToCategory("005_Category", "chairs"))
                                                        .pause(MIN_THINK_TIME, MAX_THINK_TIME)
                                                        .exec(Navigation.navigateToProduct("006_Chairs", "chairs"))
                                                        .pause(MIN_THINK_TIME, MAX_THINK_TIME)
                                                        .exec(Navigation.addProductToCart("007_Chairs", 1))
                                        )
                                )
                                .randomSwitch().on(
                                        percent(cart).then(
                                                exec(group("008_Cart").on(Navigation.navigateToCart("008_Cart", "cart")))
                                                        .pause(MIN_THINK_TIME, MAX_THINK_TIME)
                                                        .exec(group("009_Checkout").on(Navigation.navigateToCheckout("009_Checkout", "checkout")))
                                                        .pause(MIN_THINK_TIME, MAX_THINK_TIME)
                                                        .exec(group("010_Choosing_Country").on(Navigation.chooseCountry("010_Choosing_Country")))
                                                        .pause(MIN_THINK_TIME, MAX_THINK_TIME)
                                                        .exec(group("011_Place_Order").on(Navigation.placeOrder("011_Place_Order", "checkout")))
                                        )
                                )
                );
    }
}


