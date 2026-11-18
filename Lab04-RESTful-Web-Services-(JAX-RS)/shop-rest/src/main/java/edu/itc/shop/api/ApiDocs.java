package edu.itc.shop.api;

/** Shared texts for the OpenAPI annotations (annotation values must be constants). */
final class ApiDocs {

    private ApiDocs() { }

    static final String PROBLEM = "application/problem+json";

    static final String BAD_REQUEST = "Invalid parameter or body (problem+json, one entry per violation)";
    static final String UNAUTHORIZED = "Missing or invalid X-Api-Key";
    static final String NOT_ACCEPTABLE = "No representation for the Accept header";
    static final String UNSUPPORTED = "Body media type not supported";
}
