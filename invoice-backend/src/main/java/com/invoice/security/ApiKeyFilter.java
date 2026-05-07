package com.invoice.security;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class ApiKeyFilter implements ContainerRequestFilter {

    private static final Logger log = Logger.getLogger(ApiKeyFilter.class);
    private static final String API_KEY_HEADER = "X-API-Key";

    @ConfigProperty(name = "invoice.api-key")
    String expectedApiKey;

    @Override
    public void filter(ContainerRequestContext ctx) {
        String provided = ctx.getHeaderString(API_KEY_HEADER);
        if (provided == null || !MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                expectedApiKey.getBytes(StandardCharsets.UTF_8))) {
            log.warnf("Rejected request to %s — missing or invalid API key", ctx.getUriInfo().getPath());
            ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Unauthorized")
                    .build());
        }
    }
}
