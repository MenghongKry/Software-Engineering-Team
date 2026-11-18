package edu.itc.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.Test;

/**
 * Runs the real application (Main.config()) on an in-memory Grizzly per test, so every
 * test starts with fresh singletons (33 products, no orders, no idempotency keys).
 * The test server listens on port 9998, so Location/Link values also prove that no URL
 * is hard-coded to localhost:8080.
 */
class ShopApiTest extends JerseyTest {

    private static final String KEY = "demo-123";
    private static final String PROBLEM = "application/problem+json";
    private static final String TEAPOT = """
            {"name":"Teapot","sku":"TP-01","price":31.00,"stock":5}""";
    private static final String KETTLE_XL = """
            {"name":"Kettle XL","sku":"KT-01","price":29.90,"stock":12}""";
    private static final String ORDER_TWO_KETTLES = """
            {"lines":[{"productId":1,"quantity":2}]}""";

    @Override
    protected Application configure() {
        return Main.config();                       // same application as production
    }

    // ---------- helpers ----------

    private Invocation.Builder withKey(String path) {
        return target(path).request().header("X-Api-Key", KEY);
    }

    private Response placeOrder(String idempotencyKey, String body) {
        return withKey("orders").header("Idempotency-Key", idempotencyKey).post(Entity.json(body));
    }

    private static void assertProblem(Response r, int status) {
        assertEquals(status, r.getStatus());
        assertEquals(PROBLEM, r.getMediaType().toString());
        Map<?, ?> body = r.readEntity(Map.class);
        assertEquals(status, ((Number) body.get("status")).intValue());
        assertNotNull(body.get("title"));
        assertNotNull(body.get("instance"));
    }

    private Map<String, Object> json(Response r) {
        return r.readEntity(new GenericType<Map<String, Object>>() { });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> links(Map<String, Object> order) {
        return (Map<String, String>) order.get("_links");
    }

    // ---------- one test per status code ----------

    @Test
    void status200_listIsPagedWithLinksAndTotal() {
        Response r = target("products").queryParam("page", 1).queryParam("size", 10).request().get();

        assertEquals(200, r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());
        assertEquals("33", r.getHeaderString("X-Total-Count"));
        assertEquals(0, page(r.getLink("first").getUri()));
        assertEquals(0, page(r.getLink("prev").getUri()));
        assertEquals(2, page(r.getLink("next").getUri()));
        assertEquals(3, page(r.getLink("last").getUri()));
        assertEquals(9998, r.getLink("next").getUri().getPort());   // built from UriInfo
        assertEquals(10, r.readEntity(List.class).size());
    }

    @Test
    void status200_firstPageHasNoPrevAndLastPageHasNoNext() {
        Response first = target("products").queryParam("size", 10).request().get();
        Response last = target("products").queryParam("page", 3).queryParam("size", 10).request().get();

        assertNull(first.getLink("prev"));
        assertNotNull(first.getLink("next"));
        assertNull(last.getLink("next"));
        assertEquals(3, last.readEntity(List.class).size());
    }

    @Test
    void status200_v2MediaTypeGivesMoneyAndAvailable_plainJsonStaysV1() {
        Response v1 = target("products/1").request(MediaType.APPLICATION_JSON).get();
        Response v2 = target("products/1").request("application/vnd.shop.v2+json").get();

        assertEquals(200, v1.getStatus());
        assertFalse(json(v1).get("price") instanceof Map, "v1 price is a number");
        assertEquals(200, v2.getStatus());
        assertEquals("application/vnd.shop.v2+json", v2.getMediaType().toString());
        Map<String, Object> body = json(v2);
        assertEquals(Map.of("amount", 24.9, "currency", "EUR"),
                Map.of("amount", ((Number) ((Map<?, ?>) body.get("price")).get("amount")).doubleValue(),
                       "currency", ((Map<?, ?>) body.get("price")).get("currency")));
        assertEquals(true, body.get("available"));
    }

    @Test
    void status200_putWithMatchingIfMatchReturnsNewETag() {
        EntityTag tag = target("products/1").request().get().getEntityTag();

        Response r = withKey("products/1").header("If-Match", tag.toString()).put(Entity.json(KETTLE_XL));

        assertEquals(200, r.getStatus());
        assertEquals("2", r.getEntityTag().getValue());
    }

    @Test
    void status201_createReturnsLocation() {
        Response r = withKey("products").post(Entity.json(TEAPOT));

        assertEquals(201, r.getStatus());
        assertEquals(URI.create(getBaseUri() + "products/34"), r.getLocation());
        assertEquals("Teapot", json(r).get("name"));
    }

    @Test
    void status201_reviewIsCreatedThroughTheSubResource() {
        Response r = withKey("products/1/reviews")
                .post(Entity.json("""
                        {"stars":5,"author":"Ana","text":"Boils fast."}"""));

        assertEquals(201, r.getStatus());
        assertEquals(URI.create(getBaseUri() + "products/1/reviews/1"), r.getLocation());
        assertEquals(1, target("products/1/reviews").request().get(List.class).size());
    }

    @Test
    void status201_placingTheSameOrderTwiceCreatesOneOrder() {
        Response first = placeOrder("k-1", ORDER_TWO_KETTLES);
        Response second = placeOrder("k-1", ORDER_TWO_KETTLES);

        assertEquals(201, first.getStatus());
        assertEquals("false", first.getHeaderString("Idempotency-Replayed"));
        assertEquals(201, second.getStatus());
        assertEquals("true", second.getHeaderString("Idempotency-Replayed"));
        assertEquals(first.getLocation(), second.getLocation());
        assertEquals(10, stockOf(1));                // 12 - 2, reserved only once
    }

    @Test
    void status204_deleteProduct() {
        Response r = withKey("products/2").delete();

        assertEquals(204, r.getStatus());
        assertFalse(r.hasEntity());
        assertEquals(404, target("products/2").request().get().getStatus());
    }

    @Test
    void status304_ifNoneMatchWithCurrentETag() {
        Response first = target("products/1").request().get();
        EntityTag tag = first.getEntityTag();
        assertEquals("\"1\"", tag.toString());
        assertTrue(first.getHeaderString("Cache-Control").contains("max-age=60"));

        Response r = target("products/1").request().header("If-None-Match", tag.toString()).get();

        assertEquals(304, r.getStatus());
        assertEquals(tag, r.getEntityTag());
        assertFalse(r.hasEntity());
    }

    @Test
    void status400_invalidBodyListsEveryViolation() {
        Response r = withKey("products").post(Entity.json("""
                {"name":"","sku":"kettle","price":0,"stock":-1}"""));

        assertEquals(400, r.getStatus());
        assertEquals(PROBLEM, r.getMediaType().toString());
        assertEquals(4, ((List<?>) json(r).get("errors")).size());
    }

    @Test
    void status400_pageSizeOver100() {
        assertProblem(target("products").queryParam("size", 500).request().get(), 400);
    }

    @Test
    void status401_writeWithoutApiKey() {
        Response r = target("products").request().post(Entity.json(TEAPOT));

        assertEquals("ApiKey realm=\"shop\"", r.getHeaderString("WWW-Authenticate"));
        assertProblem(r, 401);
    }

    @Test
    void status404_unknownProductAndReviewsOfUnknownProduct() {
        assertProblem(target("products/99").request().get(), 404);
        assertProblem(target("products/999/reviews").request().get(), 404);
    }

    @Test
    void status406_unsupportedAcceptHeader() {
        assertProblem(target("products/1").request("text/csv").get(), 406);
    }

    @Test
    void status409_duplicateSkuAndNotEnoughStock() {
        assertProblem(withKey("products").post(Entity.json("""
                {"name":"Kettle 2","sku":"KT-01","price":10.00,"stock":1}""")), 409);
        assertProblem(placeOrder("k-tea", """
                {"lines":[{"productId":3,"quantity":1}]}"""), 409);      // Tea has stock 0
    }

    @Test
    void status412_putWithStaleETag() {
        Response r = withKey("products/1").header("If-Match", "\"0\"").put(Entity.json(KETTLE_XL));

        assertProblem(r, 412);
        assertEquals("Kettle", json(target("products/1").request().get()).get("name"));
    }

    @Test
    void status415_textPlainBody() {
        assertProblem(withKey("products").post(Entity.text("Teapot")), 415);
    }

    @Test
    void status422_sameIdempotencyKeyWithDifferentBody() {
        assertEquals(201, placeOrder("k-2", ORDER_TWO_KETTLES).getStatus());

        assertProblem(placeOrder("k-2", """
                {"lines":[{"productId":2,"quantity":1}]}"""), 422);
    }

    @Test
    void status428_putWithoutIfMatchAndOrderWithoutIdempotencyKey() {
        assertProblem(withKey("products/1").put(Entity.json(KETTLE_XL)), 428);
        assertProblem(withKey("orders").post(Entity.json(ORDER_TWO_KETTLES)), 428);
    }

    // ---------- flows ----------

    /** A client that only follows rel links: place, then pay; cancel is gone afterwards. */
    @Test
    void hateoas_placeAndPayByFollowingLinksOnly() {
        Response placed = placeOrder("k-flow", ORDER_TWO_KETTLES);
        assertEquals(201, placed.getStatus());
        assertNotNull(placed.getLink("self"));
        assertNotNull(placed.getLink("pay"));
        assertNotNull(placed.getLink("cancel"));
        Map<String, Object> order = json(placed);
        assertEquals("NEW", order.get("status"));
        assertEquals(49.8, ((Number) order.get("total")).doubleValue(), 0.001);

        Response paid = client().target(links(order).get("pay")).request()
                .header("X-Api-Key", KEY).put(Entity.json(""));
        assertEquals(200, paid.getStatus());
        Map<String, Object> paidOrder = json(paid);
        assertEquals("PAID", paidOrder.get("status"));
        assertFalse(links(paidOrder).containsKey("pay"));
        assertFalse(links(paidOrder).containsKey("cancel"));

        // the product link of the line leads to the product
        Response product = client().target(links(paidOrder).get("product-1")).request().get();
        assertEquals(200, product.getStatus());

        // paying twice / cancelling a paid order: 409
        Response again = client().target(links(order).get("pay")).request()
                .header("X-Api-Key", KEY).put(Entity.json(""));
        assertProblem(again, 409);
        Response cancelPaid = client().target(links(order).get("cancel")).request()
                .header("X-Api-Key", KEY).delete();
        assertProblem(cancelPaid, 409);
    }

    @Test
    void hateoas_cancelGivesTheStockBack() {
        Map<String, Object> order = json(placeOrder("k-cancel", ORDER_TWO_KETTLES));
        assertEquals(10, stockOf(1));

        Response cancelled = client().target(links(order).get("cancel")).request()
                .header("X-Api-Key", KEY).delete();

        assertEquals(200, cancelled.getStatus());
        assertEquals("CANCELLED", json(cancelled).get("status"));
        assertEquals(12, stockOf(1));
    }

    /** Ten retries of the same POST at the same moment: one order, stock reserved once. */
    @Test
    void idempotency_concurrentRetriesCreateOneOrder() throws Exception {
        Callable<Response> post = () -> placeOrder("k-race", ORDER_TWO_KETTLES);
        ExecutorService pool = Executors.newFixedThreadPool(10);
        List<Future<Response>> results = pool.invokeAll(java.util.Collections.nCopies(10, post));
        pool.shutdown();

        long created = 0;
        URI location = null;
        for (Future<Response> f : results) {
            Response r = f.get();
            assertEquals(201, r.getStatus());
            if ("false".equals(r.getHeaderString("Idempotency-Replayed"))) created++;
            if (location == null) location = r.getLocation();
            assertEquals(location, r.getLocation());
        }
        assertEquals(1, created);
        assertEquals(10, stockOf(1));
    }

    @Test
    void openapi_isServedAsYaml() {
        Response r = target("openapi").request().get();

        assertEquals(200, r.getStatus());
        assertEquals("application/yaml", r.getMediaType().toString());
        String yaml = r.readEntity(String.class);
        assertTrue(yaml.contains("openapi: 3.1.0"));
        assertTrue(yaml.contains("/orders/{id}/payment"));
    }

    // ---------- small helpers ----------

    private int stockOf(long id) {
        return ((Number) json(target("products/" + id).request().get()).get("stock")).intValue();
    }

    private static int page(URI uri) {
        for (String part : uri.getQuery().split("&")) {
            if (part.startsWith("page=")) return Integer.parseInt(part.substring(5));
        }
        throw new AssertionError("no page in " + uri);
    }
}
