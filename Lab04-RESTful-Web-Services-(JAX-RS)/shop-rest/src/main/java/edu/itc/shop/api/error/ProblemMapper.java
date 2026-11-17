package edu.itc.shop.api.error;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.UUID;

import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import edu.itc.shop.api.dto.Problem;

@Provider
public class ProblemMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = System.getLogger("api");

    @Context UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable ex) {
        String path = uriInfo.getRequestUri().getPath();

        if (ex instanceof WebApplicationException wae) {
            Response original = wae.getResponse();
            int status = original.getStatus();
            var problem = new Problem("about:blank", original.getStatusInfo().getReasonPhrase(),
                    status, wae.getMessage(), path, null);
            return Response.fromResponse(original)
                    .type(Problem.MEDIA_TYPE)
                    .header(HttpHeaders.CONTENT_LENGTH, null)
                    .entity(problem)
                    .build();
        }

        if (hasCause(ex, JsonbException.class)) {
            var problem = new Problem("about:blank", "Bad Request", 400,
                    "Request body is not valid JSON for this resource", path, null);
            return Response.status(400).type(Problem.MEDIA_TYPE).entity(problem).build();
        }

        String reference = UUID.randomUUID().toString();
        LOG.log(Level.ERROR, "Unhandled error, reference " + reference, ex);
        var problem = new Problem("about:blank", "Internal Server Error", 500,
                "reference " + reference, path, null);
        return Response.status(500).type(Problem.MEDIA_TYPE).entity(problem).build();
    }

    private static boolean hasCause(Throwable ex, Class<? extends Throwable> type) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (type.isInstance(t)) return true;
        }
        return false;
    }
}