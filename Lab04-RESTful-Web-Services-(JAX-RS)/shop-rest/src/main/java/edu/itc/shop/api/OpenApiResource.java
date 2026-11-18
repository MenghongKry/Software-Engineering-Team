package edu.itc.shop.api;

import java.io.InputStream;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Tag(name = "meta", description = "API description")
@Path("/openapi")
public class OpenApiResource {

    public static final String YAML = "application/yaml";
    private static final String DOCUMENT = "/META-INF/openapi.yaml";

    @GET
    @Produces(YAML)
    @Operation(summary = "This OpenAPI document (YAML)")
    @APIResponse(responseCode = "200", description = "OpenAPI 3.1 document")
    public InputStream document() {
        InputStream in = OpenApiResource.class.getResourceAsStream(DOCUMENT);
        if (in == null) throw new NotFoundException("OpenAPI document not generated, run mvn process-classes");
        return in;
    }
}
