package org.openmrs.uitestframework.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestData {

	private static final String DEFAULT_PWD = "Admin123";

	private static TestData SINGLETON;

	public static TestData instance() {
		if (SINGLETON == null) {
			SINGLETON = new TestData();
		}
		return SINGLETON;
	}

	public static final String OPENMRS_PATIENT_IDENTIFIER_TYPE = "OpenMRS ID";

	/*
	 * Note that all these TestXXX classes are intended to be used with REST
	 * which means the field names must match the expected REST API params.
	 */

	public abstract static class JsonTestClass {

		public String asJson() throws JsonProcessingException {
			return new ObjectMapper().writeValueAsString(this);
		}

		@Override
		public String toString() {
			try {
				return super.toString() + " " + asJson();
			} catch (JsonProcessingException e) {
				return super.toString();
			}
		}

		/**
		 * Return the REST name of the object/class.
		 * 
		 * @return the REST name of the object/class.
		 */
		public abstract String name();

		/**
		 * Create the object in the database using REST. Return the uuid.
		 * 
		 * @return uuid of the new object.
		 */
		public String create() {
			return create(name());
		}

		String create(String clazz) {
			JsonNode json = RestClient.post(clazz, this);
			JsonNode uuid = json.get("uuid");
			return uuid == null ? null : uuid.asText();
		}
	}

	public static class TestPerson extends JsonTestClass {
		public Set<TestPersonName> names = new HashSet<TestPersonName>();
		public Set<TestPersonAddress> addresses = new HashSet<TestPersonAddress>();
		public String gender;
		public String birthdate;

		public TestPerson(Set<TestPersonName> names,
				Set<TestPersonAddress> addresses, String gender,
				String birthdate) {
			this.names = names;
			this.addresses = addresses;
			this.gender = gender;
			this.birthdate = birthdate;
		}

		public TestPerson(String givenName, String middleName,
				String familyName, String gender, String birthdate) {
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

		@Override
		public String name() {
			return "person";
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

		public TestPersonName(String givenName, String middleName,
				String familyName) {
			this(givenName, familyName);
			this.middleName = middleName;
		}

		@Override
		public String name() {
			return null;	// not directly created
		}

	}

	public static class TestPersonAddress extends JsonTestClass {
		public String address1;
		public String address2;
		public String cityVillage;
		public String stateProvince;
		public String postalCode;
		public String country;

		public TestPersonAddress(String address1, String address2,
				String city_village, String state_province, String postal_code,
				String country) {
			this.address1 = address1;
			this.address2 = address2;
			this.cityVillage = city_village;
			this.stateProvince = state_province;
			this.postalCode = postal_code;
			this.country = country;
		}

		@Override
		public String name() {
			return null;	// not directly created
		}

	}

	public static class TestPatient extends JsonTestClass {
		public String person; // uuid
		public List<PatientIdentifier> identifiers = new ArrayList<PatientIdentifier>();

		public TestPatient(String uuid, String identifier,
				String identifierTypeName) {
			this.person = uuid;
			this.identifiers.add(new PatientIdentifier(identifier,
					identifierTypeName));
		}

		@Override
		public String name() {
			return "patient";
		}
	}

	public static class PatientIdentifier extends JsonTestClass {
		public String identifier;
		public String identifierType;
		public String location = getALocation();
		public boolean preferred = true;

		public PatientIdentifier(String identifier, String identifierTypeName) {
			this.identifierType = getIdentifierTypeUuid(identifierTypeName);
			this.identifier = identifier;
		}

		@Override
		public String name() {
			return null;	// not directly created
		}
	}

	public static class TestUser extends JsonTestClass {
		public String username;
		public String password = DEFAULT_PWD;
		public String person; // uuid
		public String[] roles;	// uuid's

		public TestUser(String usernameArg, String uuid, String[] roleUuids) {
			username = usernameArg;
			person = uuid;
			roles = roleUuids;
		}

		@Override
		public String name() {
			return "user";
		}
	}

	public static class TestRole extends JsonTestClass {
		public String name;
		public String description;

		public TestRole(String nameArg, String descriptionArg) {
			name = nameArg;
			description = descriptionArg;
		}

		@Override
		public String name() {
			return "role";
		}
	}

	public static String getALocation() {
		JsonNode locations = RestClient.get("location");
		return locations.get("results").get(0).get("uuid").asText(); // arbitrarily choose the first location
	}

	public static String getIdentifierTypeUuid(String name) {
		JsonNode json = RestClient.get("patientidentifiertype");
		return getFromJsonResults(json, "display", name, "uuid");
	}
	
	private static String getFromJsonResults(JsonNode json, String columnToMatch, String valueToMatch, String columnToGet) {
		JsonNode results = json.get("results");
		for (int i = 0; i < results.size(); i++) {
			JsonNode each = results.get(i);
			JsonNode display = each.get(columnToMatch);
			if (display.asText().equals(valueToMatch)) {
				return each.get(columnToGet).asText();
			}
		}
		return null;
	}

	public static String getPersonId(String uuid) {
		return getId("person", uuid);
	}

	public static String getUserId(String uuid) {
		return getId("user", uuid);
	}

	private static String getId(String object, String uuid) {
		JsonNode json = RestClient.get(object + "/" + uuid, "id");
		return json.get("id").asText();
	}

	public static class PersonInfo {
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
		// The rest are filled in from database.
		public String uuid;
		public String id;

		public String getName() {
			return givenName + " " + middleName + " " + familyName;
		}
	}

	public static class PatientInfo extends PersonInfo {
		public String identifier;

		@Override
		public String toString() {
			return getName() + ", " + identifier + ", " + id;
		}
	}

	public static class UserInfo extends PersonInfo {
		public String username;
		public String password = DEFAULT_PWD;
		public String userUuid;
		public String userId;
		public Set<RoleInfo> roles = new HashSet<TestData.RoleInfo>();

		@Override
		public String toString() {
			return "User: " + username + ", id: " + userId + ", personid: "
					+ id;
		}

		public void addRole(RoleInfo role) {
			roles.add(role);
		}

		public void addRole(String roleName) {
			roles.add(new RoleInfo(roleName));
		}
	}

	public static class RoleInfo {
		public String name;
		public String description = "Test Role Description";
		public String uuid;

		public RoleInfo(String nameArg) {
			name = nameArg;
		}

		@Override
		public String toString() {
			return "Role: " + name + ", description: " + description
					+ ", uuid: " + uuid;
		}
	}

	static String createRole(RoleInfo ri) {
		TestRole tr = new TestRole(ri.name, ri.description);
		ri.uuid = tr.create();
		return ri.uuid;
	}

	public static PersonInfo generateRandomPerson() {
		return generateRandomPerson(new PersonInfo());
	}

	public static PatientInfo generateRandomPatient() {
		return (PatientInfo) generateRandomPerson(new PatientInfo());
	}

	public static PersonInfo generateRandomPerson(PersonInfo pi) {
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

	private static final String[] MONTHS = { "January", "February", "March",
			"April", "May", "June", "July", "August", "September", "October",
			"November", "December" };

	private static final String[] DAYS = { "1", "2", "3", "4", "5", "6", "7",
			"8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18",
			"19", "20", "21", "22", "23", "24", "25", "26", "27", "28" };

	private static final String[] YEARS = { "1980", "1981", "1982", "1983",
			"1990", "1991", "1995" };

	static int randomArrayIndex(String[] array) {
		return (int) (Math.random() * array.length);
	}

	static String randomArrayEntry(String[] array) {
		return array[randomArrayIndex(array)];
	}

	public static String randomSuffix() {
		return randomSuffix(6);
	}

	static String randomSuffix(int digits) {
		// Last n digits of the current time.
		return StringUtils.right(String.valueOf(System.currentTimeMillis()),
				digits);
	}

	/**
	 * Add a Person to the database and return its uuid.
	 * 
	 * @param person The (test) Person to add to the database.
	 * @return The new Person's uuid.
	 */
	public static String createPerson(PersonInfo person) {
		TestPerson tp = new TestPerson(person.givenName, person.middleName, person.familyName, person.gender, makeBirthdate(person));
		tp.addAddress(new TestPersonAddress(person.address1, person.address2, person.city, person.state, person.postalCode, person.country));
		person.uuid = tp.create();
		person.id = getPersonId(person.uuid);
		return person.uuid;
	}
	
	public static final String BDAY_SEP = "-";
	private static String makeBirthdate(PersonInfo pi) {
	    return pi.birthYear + BDAY_SEP + pi.birthMonthIndex + BDAY_SEP + pi.birthDay;
    }

	public static void createUser(UserInfo ui) {
		// assumes the person has already been created
		String[] roleUuids = new String[ui.roles.size()];
		int i = 0;
		for (RoleInfo role : ui.roles) {
			if (role.uuid != null) {
				roleUuids[i] = role.uuid;
			} else {
				String roleName = role.name;
				role.uuid = getRoleUuid(roleName);
				if (role.uuid != null) {
					roleUuids[i] = role.uuid;
				} else {
					// Not sure if this is the right thing to do here, because
					// the role may not get cleaned up after..
					roleUuids[i] = createRole(role);
				}
			}
			i++;
		}
		TestUser tu = new TestUser(ui.username, ui.uuid, roleUuids);
		ui.userUuid = tu.create();
		ui.userId = TestData.getUserId(ui.userUuid);
	}

	public static String getRoleUuid(String roleName) {
		JsonNode json = RestClient.get("role");
		return getFromJsonResults(json, "display", roleName, "uuid");
	}

}
