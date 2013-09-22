package org.openmrs.uitestframework.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestData {
	
	private static TestData SINGLETON;
	
	public static TestData instance() {
		if (SINGLETON == null) {
			SINGLETON = new TestData();
		}
		return SINGLETON;
	}
	
	private static final String OPENMRS_PATIENT_IDENTIFIER_TYPE = "OpenMRS ID";
	
	/*
	 * Note that all these TestXXX classes are intended to be used with REST
	 * which means the field names must match the expected REST API params.
	 */
	
	public static class JsonTestClass {
		
		public String asJson() throws JsonProcessingException {
			return new ObjectMapper().writeValueAsString(this);
		}
		
		@Override
		public String toString() {
			try {
				return super.toString() + " " + asJson();
			}
			catch (JsonProcessingException e) {
				return super.toString();
			}
		}
	}
	
	public static class TestPerson extends JsonTestClass {
		
		public Set<TestPersonName> names = new HashSet<TestPersonName>();
		
		public Set<TestPersonAddress> addresses = new HashSet<TestPersonAddress>();
		
		public String gender;
		
		public String birthdate;
		
		public TestPerson(Set<TestPersonName> names, Set<TestPersonAddress> addresses, String gender, String birthdate) {
			this.names = names;
			this.addresses = addresses;
			this.gender = gender;
			this.birthdate = birthdate;
		}
		
		public TestPerson(String givenName, String middleName, String familyName, String gender, String birthdate) {
			this.gender = gender;
			this.birthdate = birthdate;
			this.addName(new TestPersonName(givenName, middleName, familyName));
		}
		
		public void addName(TestPersonName testPersonName) {
			this.names.add(testPersonName);
		}
		
		public void addAddress(TestPersonAddress testPersonAddress) {
			this.addresses.add(testPersonAddress);
		}
		
	}
	
	public static class TestPersonName extends JsonTestClass {
		
		public String givenName;
		
		public String familyName;
		
		public String middleName;
		
		public TestPersonName(String givenName, String familyName) {
			this.givenName = givenName;
			this.familyName = familyName;
		}
		
		public TestPersonName(String givenName, String middleName, String familyName) {
			this(givenName, familyName);
			this.middleName = middleName;
		}
		
	}
	
	public static class TestPersonAddress extends JsonTestClass {
		
		public String address1;
		
		public String address2;
		
		public String cityVillage;
		
		public String stateProvince;
		
		public String postalCode;
		
		public String country;
		
		public TestPersonAddress(String address1, String address2, String city_village,
		    String state_province, String postal_code, String country) {
			this.address1 = address1;
			this.address2 = address2;
			this.cityVillage = city_village;
			this.stateProvince = state_province;
			this.postalCode = postal_code;
			this.country = country;
		}
		
	}
	
	public static class TestPatient extends JsonTestClass {
		
		public String person; // uuid
		
		public List<PatientIdentifier> identifiers = new ArrayList<PatientIdentifier>();
		
		public TestPatient(String uuid, String identifier) {
			this.person = uuid;
			this.identifiers.add(new PatientIdentifier(identifier));
		}
	}
	
	public static class PatientIdentifier extends JsonTestClass {
		
		public String identifier;
		
		public String identifierType = getIdentifierType(OPENMRS_PATIENT_IDENTIFIER_TYPE);
		
		public String location = getALocation();
		
		public boolean preferred = true;
		
		public PatientIdentifier(String identifier) {
			this.identifier = identifier;
		}
	}
	
	public static String getALocation() {
		JsonNode locations = RestClient.get("location");
		return locations.get("results").get(0).get("uuid").asText(); // arbitrarily choose the first location
	}
	
	public static String getIdentifierType(String name) {
		JsonNode json = RestClient.get("patientidentifiertype");
		JsonNode results = json.get("results");
		for (int i = 0; i < results.size(); i++) {
			JsonNode each = results.get(i);
			JsonNode display = each.get("display");
			if (display.asText().equals(name)) {
				return each.get("uuid").asText();
			}
		}
		return null;
	}
	
	public static class PatientInfo {
		public String givenName;
		public String middleName;
		public String familyName;
		public String birthDay;
		public String birthMonth;
		public int birthMonthIndex;
		public String birthYear;
		public String gender;
		public String address1;
		public String address2;
		public String city;
		public String state;
		public String country;
		public String phone;
		public String postalCode;
	}

	public static PatientInfo generateRandomPatient() {
		PatientInfo pi = new PatientInfo();
		String suffix = randomSuffix();
		pi.givenName = "User" + suffix;
		pi.middleName = "Interface" + suffix;
		pi.familyName = "Tester" + suffix;
		pi.gender = randomArrayEntry(GENDERS);
		pi.birthDay = randomArrayEntry(DAYS);
		pi.birthMonthIndex = randomArrayIndex(MONTHS);
		pi.birthMonth = MONTHS[pi.birthMonthIndex];
		pi.birthYear = randomArrayEntry(YEARS);
		pi.address1 = "Address1" + suffix;
		pi.address2 = "Address2" + suffix;
		pi.city = "City" + suffix;
		pi.state = "State" + suffix; // TODO shorter string for State perhaps?
		pi.country = "Country" + suffix; // TODO shorter string for Country perhaps?
		pi.phone = randomSuffix(9);
		pi.postalCode = randomSuffix(5);
	    return pi;
    }

	private static final String[] GENDERS = { "M", "F" };
	
	private static final String[] MONTHS = { "January", "February", "March", "April", "May", "June", "July",
        "August", "September", "October", "November", "December" };

	private static final String[] DAYS = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13",
        "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28" };

	private static final String[] YEARS = { "1980", "1981", "1982", "1983", "1990", "1991", "1995" };

	static int randomArrayIndex(String[] array) {
		return (int) (Math.random() * array.length);
	}
	static String randomArrayEntry(String[] array) {
		return array[randomArrayIndex(array)];
	}

	static String randomSuffix() {
		return randomSuffix(6);
	}
	
	static String randomSuffix(int digits) {
		// First n digits of the current time.
		return String.valueOf(System.currentTimeMillis()).substring(0, digits);
	}

}
