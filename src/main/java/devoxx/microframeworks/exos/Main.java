package devoxx.microframeworks.exos;

import devoxx.microframeworks.exos.routes.*;
import devoxx.microframeworks.exos.services.AuthenticationService;
import devoxx.microframeworks.exos.services.ReferenceService;
import feign.FeignException;
import spark.ResponseTransformer;

import java.util.NoSuchElementException;

import static spark.Spark.*;

public class Main {
    public static void main(String... args) {
        Configuration configuration = Configuration.INSTANCE;
        ResponseTransformer encoder = object -> configuration.getGson().toJson(object);

        // Port
        int port = args.length > 0 ? Integer.parseInt(args[0]) : configuration.getPort();
        port(port);

        // Frontend
        staticFileLocation("/public");

        // XXX to configure frontend
        get("api/config.js", (request, response) -> {
            String loginUrl = configuration.getUrl(AuthenticationService.class);
            String imgUrl = configuration.getUrl(ReferenceService.class);
            return String.format("angular.module('webFrontend')" +
                    ".constant('config', {loginUrl: \"%s\", imgUrl: \"%s\"})", loginUrl, imgUrl);
        });

        // Wine
        WineRoute wineRoute = new WineRoute();
        get("api/wine/:wid", wineRoute::handleFindById, encoder);
        get("api/wine", wineRoute::handleSearch, encoder);

        // Cellar
        CellarRoute cellarRoute = new CellarRoute();
        get("api/cellar", cellarRoute::handleMyCellar, encoder);
        post("api/cellar/drink/:wid", cellarRoute::handleDrink, encoder);
        post("api/cellar/favorite/:wid", cellarRoute::handleFavorite, encoder);

        // Comment
        CommentRoute commentRoute = new CommentRoute();
        post("api/wine/:wid/comments", commentRoute::handleAddComment, encoder);

        // Order
        OrderRoute orderRoute = new OrderRoute();
        post("api/cart/order", orderRoute::handleOrder, encoder);

        // Errors
        ErrorRoute errorRoute = new ErrorRoute();
        exception(NoSuchElementException.class, errorRoute::handleNotFound);
        exception(IllegalArgumentException.class, errorRoute::handleBadRequest);
        exception(SecurityException.class, errorRoute::handleForbidden);
        exception(FeignException.class, errorRoute::handleFeignException);

        // CORS
        options("/*", (request, response) -> "");
        after((request, response) -> {
            response.header("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,OPTIONS");
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Headers", "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin,");
            response.header("Access-Control-Allow-Credentials", "true");
        });
    }

}
