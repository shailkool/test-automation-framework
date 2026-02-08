package com.automation.core.api;

import com.automation.core.config.ConfigurationManager;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.extern.log4j.Log4j2;

import java.util.Map;

/**
 * REST Assured client for API testing
 */
@Log4j2
public class RestAssuredClient {
    
    private RequestSpecification requestSpec;
    private static ConfigurationManager config = ConfigurationManager.getInstance();
    
    public RestAssuredClient() {
        initializeRequestSpec();
    }
    
    public RestAssuredClient(String baseUrl) {
        initializeRequestSpec(baseUrl);
    }
    
    private void initializeRequestSpec() {
        String baseUrl = config.getApiBaseUrl();
        initializeRequestSpec(baseUrl);
    }
    
    private void initializeRequestSpec(String baseUrl) {
        requestSpec = new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .log(LogDetail.ALL)
                .build();
        
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        log.info("REST Assured client initialized with base URL: {}", baseUrl);
    }
    
    /**
     * Set base URI
     */
    public RestAssuredClient setBaseUri(String baseUri) {
        requestSpec.baseUri(baseUri);
        return this;
    }
    
    /**
     * Set base path
     */
    public RestAssuredClient setBasePath(String basePath) {
        requestSpec.basePath(basePath);
        return this;
    }
    
    /**
     * Set header
     */
    public RestAssuredClient setHeader(String key, String value) {
        requestSpec.header(key, value);
        return this;
    }
    
    /**
     * Set headers
     */
    public RestAssuredClient setHeaders(Map<String, String> headers) {
        requestSpec.headers(headers);
        return this;
    }
    
    /**
     * Set query parameter
     */
    public RestAssuredClient setQueryParam(String key, Object value) {
        requestSpec.queryParam(key, value);
        return this;
    }
    
    /**
     * Set query parameters
     */
    public RestAssuredClient setQueryParams(Map<String, ?> params) {
        requestSpec.queryParams(params);
        return this;
    }
    
    /**
     * Set path parameter
     */
    public RestAssuredClient setPathParam(String key, Object value) {
        requestSpec.pathParam(key, value);
        return this;
    }
    
    /**
     * Set content type
     */
    public RestAssuredClient setContentType(ContentType contentType) {
        requestSpec.contentType(contentType);
        return this;
    }
    
    /**
     * Set authentication token
     */
    public RestAssuredClient setAuthToken(String token) {
        requestSpec.header("Authorization", "Bearer " + token);
        return this;
    }
    
    /**
     * Set basic authentication
     */
    public RestAssuredClient setBasicAuth(String username, String password) {
        requestSpec.auth().basic(username, password);
        return this;
    }
    
    /**
     * GET request
     */
    public Response get(String endpoint) {
        log.info("Sending GET request to: {}", endpoint);
        Response response = RestAssured.given()
                .spec(requestSpec)
                .when()
                .get(endpoint)
                .then()
                .extract()
                .response();
        
        logResponse(response);
        return response;
    }
    
    /**
     * POST request
     */
    public Response post(String endpoint, Object body) {
        log.info("Sending POST request to: {}", endpoint);
        Response response = RestAssured.given()
                .spec(requestSpec)
                .body(body)
                .when()
                .post(endpoint)
                .then()
                .extract()
                .response();
        
        logResponse(response);
        return response;
    }
    
    /**
     * POST request without body
     */
    public Response post(String endpoint) {
        log.info("Sending POST request to: {}", endpoint);
        Response response = RestAssured.given()
                .spec(requestSpec)
                .when()
                .post(endpoint)
                .then()
                .extract()
                .response();
        
        logResponse(response);
        return response;
    }
    
    /**
     * PUT request
     */
    public Response put(String endpoint, Object body) {
        log.info("Sending PUT request to: {}", endpoint);
        Response response = RestAssured.given()
                .spec(requestSpec)
                .body(body)
                .when()
                .put(endpoint)
                .then()
                .extract()
                .response();
        
        logResponse(response);
        return response;
    }
    
    /**
     * PATCH request
     */
    public Response patch(String endpoint, Object body) {
        log.info("Sending PATCH request to: {}", endpoint);
        Response response = RestAssured.given()
                .spec(requestSpec)
                .body(body)
                .when()
                .patch(endpoint)
                .then()
                .extract()
                .response();
        
        logResponse(response);
        return response;
    }
    
    /**
     * DELETE request
     */
    public Response delete(String endpoint) {
        log.info("Sending DELETE request to: {}", endpoint);
        Response response = RestAssured.given()
                .spec(requestSpec)
                .when()
                .delete(endpoint)
                .then()
                .extract()
                .response();
        
        logResponse(response);
        return response;
    }
    
    /**
     * HEAD request
     */
    public Response head(String endpoint) {
        log.info("Sending HEAD request to: {}", endpoint);
        Response response = RestAssured.given()
                .spec(requestSpec)
                .when()
                .head(endpoint)
                .then()
                .extract()
                .response();
        
        logResponse(response);
        return response;
    }
    
    /**
     * OPTIONS request
     */
    public Response options(String endpoint) {
        log.info("Sending OPTIONS request to: {}", endpoint);
        Response response = RestAssured.given()
                .spec(requestSpec)
                .when()
                .options(endpoint)
                .then()
                .extract()
                .response();
        
        logResponse(response);
        return response;
    }
    
    /**
     * Upload file
     */
    public Response uploadFile(String endpoint, String fileParamName, String filePath) {
        log.info("Uploading file to: {}", endpoint);
        Response response = RestAssured.given()
                .spec(requestSpec)
                .multiPart(fileParamName, new java.io.File(filePath))
                .when()
                .post(endpoint)
                .then()
                .extract()
                .response();
        
        logResponse(response);
        return response;
    }
    
    private void logResponse(Response response) {
        log.info("Response Status Code: {}", response.getStatusCode());
        log.debug("Response Body: {}", response.getBody().asString());
    }
    
    /**
     * Get request specification for advanced usage
     */
    public RequestSpecification getRequestSpec() {
        return requestSpec;
    }
}
