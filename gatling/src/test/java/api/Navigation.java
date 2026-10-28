package api;

import static config.BaseHelpers.ADMIN_AJAX;
import static io.gatling.javaapi.core.CoreDsl.*;  // scenario(), exec(), group(), exitBlockOnFail()
import static io.gatling.javaapi.http.HttpDsl.*;  // http(), flushHttpCache(), flushCookieJar()

import config.BaseHelpers;
import io.gatling.javaapi.core.ChainBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * NOTE: navigateToHomePage should be implemented in TASK1.
 * navigateToCategory should be implemented in TASK2.
 * navigateToProduct should be implemented in TASK3.
 * addProductToCart should be implemented in TASK3.
 * navigateToCart and navigateToCheckout should be implemented in TASK5.
 * chooseCountry and placeOrder should be implemented in TASK5.
 */
public class Navigation {
        /**
     * NOTE: This method should be implemented in TASK1.
     *
     * Navigate to the homepage and extract navigation links.
     * Should extract links for Tables, Chairs, Cart, and Checkout.
     */
    public static ChainBuilder navigateToHomePage(String group) {
        return group(group).on(
                exec(
                        http("GET /")
                                .get("/") // removed TODO comment
                                .check(regex("Performance testing Essentials").exists())
                                .check(regex("href=\"[^\"]*?(/tables[^\"]*)\"").findAll().saveAs("tables"))
                                .check(regex("href=\"[^\"]*?(/chairs[^\"]*)\"").findAll().saveAs("chairs"))
                                .check(regex("href=\"[^\"]*?(/cart[^\"]*)\"").findAll().saveAs("cart"))
                                .check(regex("href=\"[^\"]*?(/checkout[^\"]*)\"").findAll().saveAs("checkout"))
                )
        );
    }

    /*https://ebook.learn.epam.com/perf-copilot/docs/autocode-gatling/module-2/task-2#:~:text=public%20static%20ChainBuilder%20navigateToCategory,sessionVar))%0A%20%20%20%20%20%20%20%20)%0A%20%20%20%20)%3B%0A%7D
    /**
     * NOTE: This method should be implemented in TASK2.
     * <p>
     * Navigate to a category page (Tables or Chairs) and extract a random product
     * link.
     */
    public static ChainBuilder navigateToCategory(String group, String sessionVar) {
        return group(group).on(
                exec(
                        http("GET #{" + sessionVar + "(0)}")
                                .get("#{" + sessionVar + "(0)}")
                                .check(regex("href=\"[^\"]*?/products/([^\"]*)\"").findRandom().saveAs("rnd" + sessionVar))
                )
        );
    }

    /**
     * NOTE: This method should be implemented in TASK3.
     * <p>
     * Navigate to a product page and extract productId and cart_content.
     */
    public static ChainBuilder navigateToProduct(String group, String sessionVar) {
        return group(group).on(
                exec(
                        http("GET /products/#{rnd" + sessionVar + "}")
                                .get("/products/#{rnd" + sessionVar + "}")
                                .check(regex("<link rel='shortlink' href='.+\\?p=(\\d+)' />").saveAs("productId"))
                                .check(regex("<input type=\"hidden\" name=\"cart_content\" value='(.*?)'>")
                                        .optional().saveAs("cart_content"))
                )
        );
    }

    /**
     * NOTE: This method should be implemented in TASK3.
     * <p>
     * Add a product to cart via POST request to admin-ajax.php.
     */
    public static ChainBuilder addProductToCart(String prefix, int quantity) {

        return group(prefix + "_addToCart").on(
                exec(session -> session.contains("cart_content")
                        ? session
                        : session.set("cart_content", ""))
                        .exec(                                                  // ← було exec(, стало .exec(
                                http("POST " + ADMIN_AJAX)
                                        .post(ADMIN_AJAX)
                                        .formParamMap(Map.of(
                                                "action", "ic_add_to_cart",
                                                "add_cart_data",
                                                "current_product=#{productId}" +
                                                        "&cart_content=#{cart_content}" +
                                                        "&current_quantity=" + quantity,
                                                "cart_widget", BaseHelpers.CART_WIDGET_DISABLED,
                                                "cart_container", BaseHelpers.CART_CONTAINER_DISABLED
                                                 ))
                                        .check(regex("Added!").exists())
                        )
        );
    }

    public static ChainBuilder navigateToCart(String group, String sessionVar) {
        return exec(
                http("GET #{" + sessionVar + "(0)}")
                        .get("#{" + sessionVar + "(0)}")
                        .check(regex("name=\"cart_content\" value='(.*?)'").optional().saveAs("cart_content"))
        );
    }

    public static ChainBuilder navigateToCheckout(String group, String sessionVar) {
        return exec(
                http("GET #{" + sessionVar + "(0)}")
                        .get("#{" + sessionVar + "(0)}")
                        .check(regex("name=\"cart_content\" value='(.*?)'").saveAs("cart_content"))
                        .check(regex("name=\"(product_price_\\d+__)\" value=").findAll().saveAs("product_price_names"))
                        .check(regex("name=\"product_price_\\d+__\" value=\"(.*?)\"").findAll().saveAs("products_price_list"))
                        .check(regex("value=\"(\\d+)\" name=\"trans_id\"").saveAs("trans_id"))
                        .check(regex("name=\"total_net\" value=\"(.+?)\"").saveAs("total_net"))
                        .check(regex("<option value=\"([A-Z]{2})\"").findRandom().saveAs("country_code"))
        )
                .exec(session -> {
                    List<String> names = session.getList("product_price_names");
                    List<String> prices = session.getList("products_price_list");
                    List<String> pairs = new ArrayList<>();
                    for (int i = 0; i < names.size(); i++) {
                        pairs.add(names.get(i) + "=" + prices.get(i));
                    }
                    return session.set("products_price", String.join("&", pairs));
                });
    };


    public static ChainBuilder chooseCountry(String group) {
        return exec(
                http("POST " + ADMIN_AJAX)
                        .post(ADMIN_AJAX)
                        .formParam("action", "ic_state_dropdown")
                        .formParam("country_code", "#{country_code}")
                        .formParam("state_code", "")
                        .check(regex("\\\"value\\\":\\\"([A-Z0-9_]+)\\\"")
                                .findRandom()
                                .optional()
                                .saveAs("state_code"))
        ).exec(session -> {
            if (!session.contains("state_code")) {
                return session.set("state_code", "");
            }
            return session;
        });
    }

    public static ChainBuilder placeOrder(String group, String sessionVar) {
        return exec(
                http("POST #{" + sessionVar + "(0)}")
                        .post("#{" + sessionVar + "(0)}")
                        .asFormUrlEncoded()
                        .body(ElFileBody("bodies/submit.txt"))
                        .check(currentLocationRegex("thank-you"))
        );
    }
}

