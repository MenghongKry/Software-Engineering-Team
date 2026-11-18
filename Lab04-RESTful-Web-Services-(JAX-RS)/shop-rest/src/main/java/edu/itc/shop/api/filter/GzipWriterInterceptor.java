package edu.itc.shop.api.filter;

import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;

/** Gzips @Compress responses when the client accepts gzip. */
@Provider
@Compress
public class GzipWriterInterceptor implements WriterInterceptor {

    @Context HttpHeaders requestHeaders;

    @Override
    public void aroundWriteTo(WriterInterceptorContext ctx) throws IOException {
        String enc = requestHeaders.getHeaderString(HttpHeaders.ACCEPT_ENCODING);
        if (enc == null || !enc.contains("gzip")) {
            ctx.proceed();
            return;
        }

        ctx.getHeaders().putSingle(HttpHeaders.CONTENT_ENCODING, "gzip");
        ctx.getHeaders().add(HttpHeaders.VARY, HttpHeaders.ACCEPT_ENCODING);
        ctx.getHeaders().remove(HttpHeaders.CONTENT_LENGTH);   // old length is wrong after gzip

        GZIPOutputStream gzip = new GZIPOutputStream(ctx.getOutputStream());
        ctx.setOutputStream(gzip);
        try {
            ctx.proceed();
        } finally {
            gzip.finish();                                    // write gzip trailer
        }
    }
}
