package org.openmrs.uitestframework.test;

import java.io.IOException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.client.filter.HttpBasicAuthFilter;
import org.openmrs.uitestframework.page.TestProperties;
import org.openmrs.uitestframework.test.TestData.JsonTestClass;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RestClient {

	private static final String REST_ROOT = "/ws/rest/v1/";

	public static JsonNode get(String restPath) {
		return get(restPath, null);
    }

	// columns is a comma separated list (or null)
	public static JsonNode get(String restPath, String columns) {
		WebTarget target = newClient().target(getWebAppUrl()).path(REST_ROOT + restPath);
		if (columns != null) {
			target = target.queryParam("v", "custom:(" + columns + ")");
		}
		String jsonString = target.request().get(String.class);
        try {
	        return new ObjectMapper().readValue(jsonString, JsonNode.class);
        }
        catch (JsonParseException e) {
	        log("error during REST get", e);
	        return null;
        }
        catch (JsonMappingException e) {
	        log("error during REST get", e);
	        return null;
        }
        catch (IOException e) {
	        log("error during REST get", e);
	        return null;
        }
	}

	public static JsonNode delete(String restPath) {
		return delete(restPath, null);
	}

	// columns is a comma separated list (or null)
	public static JsonNode delete(String restPath, String columns) {
		WebTarget target = newClient().target(getWebAppUrl()).path(REST_ROOT + restPath);
		if (columns != null) {
			target = target.queryParam("v", "custom:(" + columns + ")");
		}
		String jsonString = target.request().delete(String.class);
		try {
			return new ObjectMapper().readValue(jsonString, JsonNode.class);
		}
		catch (JsonParseException e) {
			log("error during REST delete", e);
			return null;
		}
		catch (JsonMappingException e) {
			log("error during REST delete", e);
			return null;
		}
		catch (IOException e) {
			log("error during REST delete", e);
			return null;
		}
	}

	public static JsonNode post(String restPath, JsonTestClass object) {
		WebTarget target = newClient().target(getWebAppUrl()).path(REST_ROOT + restPath);
        try {
        	String objectAsJson = object.asJson();
//System.out.println("post " + restPath + " " + objectAsJson);
			Entity<String> entity = Entity.entity(objectAsJson, MediaType.APPLICATION_JSON_TYPE);
        	String json = target.request(MediaType.APPLICATION_JSON_TYPE).post(entity, String.class);
//System.out.println("\t=> " + json);
	        return new ObjectMapper().readValue(json, JsonNode.class);
        }
        catch (JsonParseException e) {
	        log("error during REST post", e);
	        return null;
        }
        catch (JsonMappingException e) {
	        log("error during REST post", e);
	        return null;
        }
        catch (IOException e) {
	        log("error during REST post", e);
	        return null;
        }
	}
	
	private static Client newClient() {
		return ClientBuilder.newClient().register(new HttpBasicAuthFilter(getUsername(), getPassword()));
	}
	
	static String getUsername() {
		return TestProperties.instance().getUserName();
	}
	
	static String getPassword() {
		return TestProperties.instance().getPassword();
	}
	
	static String getWebAppUrl() {
		return TestProperties.instance().getWebAppUrl();
	}
	
	static void log(Object o) {
		System.out.println(o);
	}
	
	static void log(Object o, Exception e) {
		System.out.println(o);
		e.printStackTrace();
	}

	// Note this is not REST. 
	public static String generatePatientIdentifier(String source) {
		Client client = newClient();
		WebTarget target = client.target(getWebAppUrl())
				.path("/module/idgen/generateIdentifier.form")
				.queryParam("source", source)
				.queryParam("username", getUsername())
				.queryParam("password", getPassword());
		String jsonString = target.request(MediaType.APPLICATION_JSON_TYPE).get(String.class);
        JsonNode json;
        try {
	        json = new ObjectMapper().readValue(jsonString, JsonNode.class);
        }
        catch (JsonParseException e) {
	        log("error during generatePatientIdentifier", e);
	        return null;
        }
        catch (JsonMappingException e) {
	        log("error during generatePatientIdentifier", e);
	        return null;
        }
        catch (IOException e) {
	        log("error during generatePatientIdentifier", e);
	        return null;
        }
		return json.get("identifiers").get(0).asText();
    }

}
