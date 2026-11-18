package edu.itc.shop.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import edu.itc.shop.api.dto.Problem;
import edu.itc.shop.api.dto.ProductDto;
import edu.itc.shop.api.dto.ProductV2;
import edu.itc.shop.api.dto.StockChange;
import edu.itc.shop.api.filter.Compress;
import edu.itc.shop.api.params.ProductFilter;
import edu.itc.shop.domain.Page;
import edu.itc.shop.domain.Product;
import edu.itc.shop.domain.ProductService;
import edu.itc.shop.domain.ReviewRepository;

@Tag(name = "products", description = "Product catalogue")
@Path("/products")
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class ProductResource {

    /** v1 is the default: plain application/json (qs=1.0) wins over v2 (qs=0.5) for Accept: * / *. */
    public static final String V1_JSON = "application/vnd.shop.v1+json";
    public static final String V2_JSON = "application/vnd.shop.v2+json";

    private final ProductService service;
    private final ReviewRepository reviews;

    @Inject
    public ProductResource(ProductService service, ReviewRepository reviews) {
        this.service = service;
        this.reviews = reviews;
    }

    // ---------- list with paging (Task 5) ----------

    @GET
    @Compress
    @Operation(summary = "List products",
            description = "Filtered, sorted and paged. Link header: first, prev, next, last. X-Total-Count: total after filtering.")
    @APIResponse(responseCode = "200", description = "One page of products",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ProductDto.class, type = SchemaType.ARRAY)))
    @APIResponse(responseCode = "400", description = "Bad filter, sort field, money value, page or size", content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "406", description = ApiDocs.NOT_ACCEPTABLE, content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    public Response list(@BeanParam @Valid ProductFilter f, @Context UriInfo uriInfo) {
        Page<Product> page = service.searchPage(f.toQuery(), f.page, f.size);
        List<ProductDto> body = page.items().stream().map(ProductDto::from).toList();

        var links = new ArrayList<Link>();
        links.add(pageLink(uriInfo, 0, "first"));
        if (page.hasPrevious()) links.add(pageLink(uriInfo, page.page() - 1, "prev"));
        if (page.hasNext()) links.add(pageLink(uriInfo, page.page() + 1, "next"));
        links.add(pageLink(uriInfo, page.lastPage(), "last"));

        return Response.ok(new GenericEntity<List<ProductDto>>(body) { })
                .links(links.toArray(Link[]::new))
                .header("X-Total-Count", page.total())
                .build();
    }

    /** Same request URI (keeps q, sort, size ...), only page changes. */
    private static Link pageLink(UriInfo uriInfo, int page, String rel) {
        return Link.fromUriBuilder(uriInfo.getRequestUriBuilder().replaceQueryParam("page", page))
                .rel(rel).build();
    }

    // ---------- one product: ETag + Cache-Control, v1 and v2 (Task 5) ----------

    @GET
    @Path("{id: \\d+}")
    @Produces({"application/json; qs=1.0", V1_JSON, MediaType.APPLICATION_XML + "; qs=0.9"})
    @Operation(summary = "Get one product (v1 or v2, chosen by Accept)",
            description = "application/json or application/vnd.shop.v1+json (default), XML, or "
                    + "application/vnd.shop.v2+json. Sends ETag and Cache-Control.")
    @APIResponse(responseCode = "200", description = "The product",
            content = {
                @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProductDto.class)),
                @Content(mediaType = V1_JSON, schema = @Schema(implementation = ProductDto.class)),
                @Content(mediaType = MediaType.APPLICATION_XML, schema = @Schema(implementation = ProductDto.class))})
    @APIResponse(responseCode = "304", description = "If-None-Match matches the current ETag")
    @APIResponse(responseCode = "404", description = "No product with this id", content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "406", description = ApiDocs.NOT_ACCEPTABLE, content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    public Response getById(@Parameter(description = "Product id") @PathParam("id") long id,
                            @Context Request request) {
        Product p = service.get(id);                                // 404 inside
        return cacheable(p, ProductDto.from(p), request);
    }

    @GET
    @Path("{id: \\d+}")
    @Produces(V2_JSON + "; qs=0.5")
    @Operation(summary = "Get one product (v2)",
            description = "Accept: application/vnd.shop.v2+json. Price as a Money object plus an available flag.")
    @APIResponse(responseCode = "200", description = "The product, v2 shape",
            content = @Content(mediaType = V2_JSON, schema = @Schema(implementation = ProductV2.class)))
    @APIResponse(responseCode = "304", description = "If-None-Match matches the current ETag")
    @APIResponse(responseCode = "404", description = "No product with this id", content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    public Response getByIdV2(@Parameter(description = "Product id") @PathParam("id") long id,
                              @Context Request request) {
        Product p = service.get(id);
        return cacheable(p, ProductV2.from(p), request);
    }

    /** 304 when If-None-Match matches the current version, otherwise 200 with ETag + Cache-Control. */
    private static Response cacheable(Product p, Object body, Request request) {
        EntityTag tag = ETags.of(p);
        Response.ResponseBuilder notModified = request.evaluatePreconditions(tag);
        if (notModified != null) return notModified.tag(tag).build();

        CacheControl cc = new CacheControl();
        cc.setPrivate(true);
        cc.setMaxAge(60);
        cc.setNoTransform(true);
        return Response.ok(body).tag(tag).cacheControl(cc).build();
    }

    // ---------- writes ----------

    @POST
    @Operation(summary = "Create a product", description = "Needs X-Api-Key. Answers 201 with Location and ETag.")
    @APIResponse(responseCode = "201", description = "Created, Location points to the new product",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProductDto.class)))
    @APIResponse(responseCode = "400", description = ApiDocs.BAD_REQUEST, content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "401", description = ApiDocs.UNAUTHORIZED, content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "409", description = "SKU already exists", content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "415", description = ApiDocs.UNSUPPORTED, content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    public Response create(@Valid @NotNull ProductDto dto, @Context UriInfo uriInfo) {
        Product saved = service.create(dto.toProduct(0, 0));
        URI location = uriInfo.getAbsolutePathBuilder().path(Long.toString(saved.id())).build();
        return Response.created(location).entity(ProductDto.from(saved)).tag(ETags.of(saved)).build();
    }

    /** Task 5: PUT needs If-Match (428 when missing, 412 when stale) and returns the new ETag. */
    @PUT
    @Path("{id: \\d+}")
    @Operation(summary = "Replace a product", description = "Needs X-Api-Key and If-Match with the current ETag. Returns the new ETag.")
    @APIResponse(responseCode = "200", description = "Replaced",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProductDto.class)))
    @APIResponse(responseCode = "400", description = ApiDocs.BAD_REQUEST, content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "401", description = ApiDocs.UNAUTHORIZED, content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "404", description = "No product with this id", content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "409", description = "SKU used by another product", content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "412", description = "If-Match does not match the current version", content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "415", description = ApiDocs.UNSUPPORTED, content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "428", description = "If-Match header missing", content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    public Response replace(@Parameter(description = "Product id") @PathParam("id") long id,
                            @Parameter(in = ParameterIn.HEADER, description = "ETag from the last GET, e.g. \"1\"")
                            @HeaderParam(HttpHeaders.IF_MATCH) String ifMatch,
                            @Valid @NotNull ProductDto dto) {
        Product saved = service.replace(id, dto.toProduct(id, 0), ETags.expectedVersion(ifMatch));
        return Response.ok(ProductDto.from(saved)).tag(ETags.of(saved)).build();
    }

    @DELETE
    @Path("{id: \\d+}")
    @Operation(summary = "Delete a product", description = "Needs X-Api-Key.")
    @APIResponse(responseCode = "204", description = "Deleted")
    @APIResponse(responseCode = "401", description = ApiDocs.UNAUTHORIZED, content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "404", description = "No product with this id", content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    public Response delete(@Parameter(description = "Product id") @PathParam("id") long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    @PATCH
    @Path("{id: \\d+}/stock")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Change the stock", description = "Needs X-Api-Key. Body {\"delta\": -5}.")
    @APIResponse(responseCode = "200", description = "Updated product",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProductDto.class)))
    @APIResponse(responseCode = "400", description = ApiDocs.BAD_REQUEST, content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "401", description = ApiDocs.UNAUTHORIZED, content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "404", description = "No product with this id", content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "409", description = "Stock would go negative", content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "415", description = ApiDocs.UNSUPPORTED, content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    public ProductDto changeStock(@Parameter(description = "Product id") @PathParam("id") long id,
                                  @Valid @NotNull StockChange change) {
        return ProductDto.from(service.changeStock(id, change.delta()));
    }



    /** TASK5-No HTTP method: Jersey continues matching inside ReviewResource. Unknown product = 404 here. */
    @Path("{id: \\d+}/reviews")
    public ReviewResource reviews(@Parameter(description = "Product id") @PathParam("id") long id) {
        return new ReviewResource(service.get(id), reviews);
    }
}
