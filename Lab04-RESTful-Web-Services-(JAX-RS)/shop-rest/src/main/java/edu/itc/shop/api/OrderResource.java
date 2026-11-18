package edu.itc.shop.api;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import edu.itc.shop.api.IdempotencyStore.Stored;
import edu.itc.shop.api.dto.Problem;
import edu.itc.shop.api.dto.OrderDto;
import edu.itc.shop.api.dto.PlaceOrder;
import edu.itc.shop.domain.Order;
import edu.itc.shop.domain.OrderLine;
import edu.itc.shop.domain.OrderService;
import edu.itc.shop.domain.OrderStatus;

@Tag(name = "orders", description = "Placing, paying and cancelling orders (HATEOAS)")
@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {

    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    public static final String REPLAYED = "Idempotency-Replayed";

    private final OrderService orders;
    private final IdempotencyStore idempotency;

    @Inject
    public OrderResource(OrderService orders, IdempotencyStore idempotency) {
        this.orders = orders;
        this.idempotency = idempotency;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Place an order",
            description = "Needs X-Api-Key and Idempotency-Key. Reserves stock. A retry with the same key and body "
                    + "replays the first 201 with Idempotency-Replayed: true.")
    @APIResponse(responseCode = "201", description = "Created (or replayed), Location + Link headers", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OrderDto.class)),
            headers = @Header(name = REPLAYED, description = "true when this is a stored response", schema = @Schema(implementation = Boolean.class)))
    @APIResponse(responseCode = "400", description = ApiDocs.BAD_REQUEST, content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "401", description = ApiDocs.UNAUTHORIZED, content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "404", description = "Unknown product in a line", content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "409", description = "Not enough stock", content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "415", description = ApiDocs.UNSUPPORTED, content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "422", description = "Idempotency-Key reused with a different body", content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "428", description = "Idempotency-Key header missing", content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    public Response place(@Parameter(in = ParameterIn.HEADER, required = true,
                                     description = "Unique key per order attempt, reuse it when retrying")
                          @HeaderParam(IDEMPOTENCY_KEY) String key,
                          @Valid @NotNull PlaceOrder cmd,
                          @Context UriInfo uriInfo) {
        if (key == null || key.isBlank()) {
            throw HttpErrors.preconditionRequired(IDEMPOTENCY_KEY + " header required");
        }
        String hash = Hashes.sha256(cmd);
        var result = idempotency.once(key, () -> {
            Order order = orders.place(cmd.toLines());                  // 404 / 409 inside, nothing stored
            URI location = orderUri(uriInfo, order.id());
            return new Stored(hash, Response.Status.CREATED.getStatusCode(), location, represent(order, uriInfo));
        });
        Stored s = result.stored();
        if (!s.bodyHash().equals(hash)) {
            throw HttpErrors.unprocessable(IDEMPOTENCY_KEY + " " + key + " was already used with a different body");
        }
        return withLinks(Response.status(s.status()).location(s.location()), s.body())
                .header(REPLAYED, result.replayed())
                .build();
    }

    @GET
    @Path("{id}")
    @Operation(summary = "Get an order")
    @APIResponse(responseCode = "200", description = "The order with its links", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OrderDto.class)))
    @APIResponse(responseCode = "404", description = "No order with this id", content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    public Response get(@Parameter(description = "Order id, e.g. o-1001") @PathParam("id") String id,
                        @Context UriInfo uriInfo) {
        return ok(orders.get(id), uriInfo);
    }

    /** rel "pay": NEW -> PAID, 409 otherwise. */
    @PUT
    @Path("{id}/payment")
    @Operation(summary = "Pay an order (rel pay)", description = "Needs X-Api-Key.")
    @APIResponse(responseCode = "200", description = "Order is PAID", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OrderDto.class)))
    @APIResponse(responseCode = "401", description = ApiDocs.UNAUTHORIZED, content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "404", description = "No order with this id", content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "409", description = "Order is not NEW", content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    public Response pay(@Parameter(description = "Order id") @PathParam("id") String id,
                        @Context UriInfo uriInfo) {
        return ok(orders.pay(id), uriInfo);
    }

    /** rel "cancel": NEW -> CANCELLED and the stock goes back, 409 otherwise. */
    @DELETE
    @Path("{id}")
    @Operation(summary = "Cancel an order (rel cancel)", description = "Needs X-Api-Key. Gives the stock back.")
    @APIResponse(responseCode = "200", description = "Order is CANCELLED", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OrderDto.class)))
    @APIResponse(responseCode = "401", description = ApiDocs.UNAUTHORIZED, content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "404", description = "No order with this id", content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "409", description = "Order is PAID or already CANCELLED", content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    public Response cancel(@Parameter(description = "Order id") @PathParam("id") String id,
                           @Context UriInfo uriInfo) {
        return ok(orders.cancel(id), uriInfo);
    }

    // ---------- HATEOAS ----------

    private static Response ok(Order order, UriInfo uriInfo) {
        return withLinks(Response.ok(), represent(order, uriInfo)).build();
    }

    /** The same links go into the body (_links) and into Link headers. */
    private static Response.ResponseBuilder withLinks(Response.ResponseBuilder rb, OrderDto dto) {
        dto.links().forEach((rel, uri) -> rb.links(Link.fromUri(uri).rel(rel).build()));
        return rb.entity(dto);
    }

    /** All URIs come from UriInfo, so they follow the host/port the client used. */
    static OrderDto represent(Order order, UriInfo uriInfo) {
        URI self = orderUri(uriInfo, order.id());
        Map<String, String> links = new LinkedHashMap<>();
        links.put("self", self.toString());
        if (order.status() == OrderStatus.NEW) {
            links.put("pay", uriInfo.getBaseUriBuilder().path(OrderResource.class)
                    .path(order.id()).path("payment").build().toString());
            links.put("cancel", self.toString());
        }
        for (OrderLine line : order.lines()) {
            links.put("product-" + line.productId(), uriInfo.getBaseUriBuilder()
                    .path(ProductResource.class).path(Long.toString(line.productId())).build().toString());
        }
        return OrderDto.from(order, links);
    }

    private static URI orderUri(UriInfo uriInfo, String id) {
        return uriInfo.getBaseUriBuilder().path(OrderResource.class).path(id).build();
    }
}
