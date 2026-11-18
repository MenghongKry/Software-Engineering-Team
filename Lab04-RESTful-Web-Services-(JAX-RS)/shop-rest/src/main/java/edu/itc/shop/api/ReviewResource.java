package edu.itc.shop.api;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import edu.itc.shop.api.dto.Problem;
import edu.itc.shop.api.dto.ReviewDto;
import edu.itc.shop.domain.Product;
import edu.itc.shop.domain.Review;
import edu.itc.shop.domain.ReviewRepository;

@Tag(name = "reviews", description = "Reviews of one product")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReviewResource {

    private final Product product;
    private final ReviewRepository reviews;

    public ReviewResource(Product product, ReviewRepository reviews) {
        this.product = product;
        this.reviews = reviews;
    }

    @GET
    @Operation(summary = "List the reviews of a product")
    @APIResponse(responseCode = "200", description = "Reviews", content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = ReviewDto.class, type = SchemaType.ARRAY)))
    @APIResponse(responseCode = "404", description = "No product with this id", content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    public List<ReviewDto> list() {
        return reviews.findByProduct(product.id()).stream().map(ReviewDto::from).toList();
    }

    @POST
    @Operation(summary = "Add a review", description = "Needs X-Api-Key. stars 1..5, author required, text up to 500 chars.")
    @APIResponse(responseCode = "201", description = "Created, Location points to the review",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ReviewDto.class)))
    @APIResponse(responseCode = "400", description = ApiDocs.BAD_REQUEST, content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "401", description = ApiDocs.UNAUTHORIZED, content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "404", description = "No product with this id", content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "415", description = ApiDocs.UNSUPPORTED, content = @Content(mediaType = ApiDocs.PROBLEM, schema = @Schema(implementation = Problem.class)))
    public Response add(@Valid @NotNull ReviewDto dto, @Context UriInfo uriInfo) {
        Review saved = reviews.add(product.id(), dto.stars(), dto.author(), dto.text());
        URI location = uriInfo.getAbsolutePathBuilder().path(Long.toString(saved.id())).build();
        return Response.created(location).entity(ReviewDto.from(saved)).build();
    }
}
