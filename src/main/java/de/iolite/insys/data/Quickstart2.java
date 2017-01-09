package de.iolite.insys.data;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.IOUtils;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;



import org.mortbay.log.Log;

/**
 * Source mainly taken from
 * https://developers.google.com/google-apps/calendar/quickstart/java
 * additional:
 */

public class Quickstart2 {
	/** Application name. */
	private static final String APPLICATION_NAME = "Google Calendar API Java Quickstart";

	/** Directory to store user credentials for this application. */
//	private static final java.io.File DATA_STORE_DIR = new java.io.File(
//			System.getProperty("user.home"),
//			".credentials/calendar-java-quickstart");

	/** Global instance of the {@link FileDataStoreFactory}. */
	private static FileDataStoreFactory DATA_STORE_FACTORY;

	/** Global instance of the JSON factory. */
	private static final JsonFactory JSON_FACTORY = JacksonFactory
			.getDefaultInstance();

	/** Global instance of the HTTP transport. */
	private static HttpTransport HTTP_TRANSPORT;

	/**
	 * Global instance of the scopes required by this quickstart.
	 *
	 * If modifying these scopes, delete your previously saved credentials at
	 * ~/.credentials/calendar-java-quickstart
	 */
	private static final List<String> SCOPES = Arrays
			.asList(CalendarScopes.CALENDAR_READONLY);

	static {
		try {
			HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		//	DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
	}

	public InputStream getInput(){
		InputStream in = getClass().getResourceAsStream("/My Project.p12");	
		return in; 
	}
	
	/**
	 * Build and return an authorized Calendar client service.
	 * 
	 * @return an authorized Calendar client service
	 * @throws IOException
	 * @throws GeneralSecurityException
	 * @throws URISyntaxException 
	 */
	public com.google.api.services.calendar.Calendar getCalendarService()
			throws IOException, GeneralSecurityException, URISyntaxException {
//		  ArrayList<String> result = new ArrayList<String>();
		  InputStream in = getInput();
		 
//          BufferedReader reader = new BufferedReader(new InputStreamReader(in));
//          //The line that will be read:
//          String line = null;
//          //Reads the source-file line-by-line until it ends:
//          while ((line = reader.readLine()) != null) {
//        	  
//              //Adds the actual line to the result-array:
//              result.add(line);
//                          
//          }
          
                 
          String PREFIX = "MyProject";
          String SUFFIX = ".p12";

         File tempFile = File.createTempFile(PREFIX, SUFFIX);
         //tempFile.deleteOnExit();
              try (FileOutputStream out = new FileOutputStream(tempFile)) {
                  IOUtils.copy(in, out);
              }
          
          System.out.println(tempFile.getAbsolutePath());
    
//		URI uri = Quickstart2.class.getResource("/My Project.p12").toURI();
//		File p12File;
//		if (uri.getScheme().equals("jar")) {
//            final FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.<String, Object> emptyMap());
//            p12File = fileSystem.getPath("src/main/resources/My Project.p12").toFile();
//        } else {
//        	p12File = Paths.get(uri).toFile();
//        }
//		
		GoogleCredential credential = new GoogleCredential.Builder()
				.setTransport(HTTP_TRANSPORT)
				.setJsonFactory(JSON_FACTORY)
				.setServiceAccountId(
						"calendarapp@massive-triumph-150916.iam.gserviceaccount.com")
				//.setServiceAccountPrivateKeyId("0dc02b2ce032bd67fc5a039a61372a026358af6c")

				.setServiceAccountPrivateKeyFromP12File(tempFile)
				//.setServiceAccountPrivateKeyId(serviceAccountPrivateKeyId)
				.setServiceAccountScopes(SCOPES)
				// .setServiceAccountScopes(Collections.singleton(SQLAdminScopes.SQLSERVICE_ADMIN))
				.setServiceAccountUser("arianeziehn@googlemail.com")

				.build();

		// Credential credential = authorize();

		return new com.google.api.services.calendar.Calendar.Builder(
				HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName(
				APPLICATION_NAME).build();
	}

	public DailyEvents getData() throws IOException, ParseException,
			GeneralSecurityException, URISyntaxException {
		
		// Build a new authorized API client service.
		// Note: Do not confuse this class with the
		// com.google.api.services.calendar.model.Calendar class.
		com.google.api.services.calendar.Calendar service = getCalendarService();

		// create the current dateTime
		com.google.api.client.util.DateTime now = new com.google.api.client.util.DateTime(
				System.currentTimeMillis());
		// create a maxTime today 23:59:59 as we just need daily information
		Date day = new Date(System.currentTimeMillis());
		// methods old but needed for google.api.client
		day.setHours(23);
		day.setMinutes(59);
		day.setSeconds(59);
		com.google.api.client.util.DateTime t = new com.google.api.client.util.DateTime(
				day);

		Events events = service.events().list("primary")
		 .setMaxResults(5)
		// just upcoming Events
				.setTimeMin(now)
				// just today's Events
				// .setTimeMax(t)
				.setOrderBy("startTime").setSingleEvents(true).execute();

		List<Event> items = events.getItems();
		List<GoogleEvent> allToday = new LinkedList<GoogleEvent>();

		

		for (com.google.api.services.calendar.model.Event event : items) {
			GoogleEvent today = new GoogleEvent();

			com.google.api.client.util.DateTime start = event.getStart()
					.getDateTime();
			com.google.api.client.util.DateTime end = event.getEnd()
					.getDateTime();

			// Date startDate = start.toDate();
			// ohne Zeitangabe kein Event
			if (start != null && end != null) {

				java.util.Calendar cStart = java.util.Calendar.getInstance();
				cStart.setTimeInMillis(start.getValue());
				java.util.Calendar cEnd = java.util.Calendar.getInstance();
				cEnd.setTimeInMillis(end.getValue());

				today.setBegin(cStart);
				today.setEnd(cEnd);

				// DateTime end = event.getEnd().getDateTime();
				// if (end != null) {
				// today.setEnd(end);
				// }
				String Name = event.getSummary();
				if (Name != null)
					today.setName(Name);
				else
					today.setName("Your Appointment");

				String color = event.getColorId();

				String status = "nA";
				if (color != null) {
					switch (color) {
					case "1":
						status = "work";
						break;

					case "2":
						status = "uiiiii";
						break;
					case "3":
						status = "task";
						break;
					case "4":
						status = "dunkelesorange";
						break;
					case "5":
						status = "gelb";
						break;
					case "6":
						status = "orange";
						break;
					case "7":
						status = "iwann";
						break;
					case "8":
						status = "iwall";
						break;
					case "9":
						status = "blau";
						break;
					case "10":
						status = "gr�n";
						break;
					// rot
					case "11":
						status = "red";
						break;
					default:
						status = "So geht es nicht";
					}
				}
				// TODO default for null value
				if (color == null)
					status = "wei�";

				today.setColor(status);

				// String kind = event.getKind();
				// if (kind != null) today.setKind(kind);

				String location = event.getLocation();
				;
				if (location != null)
					today.setLocation(location);
				else
					today.setLocation("unkown");

				String share = event.getVisibility();
				if (share != null)
					today.setStatus(share);
				else
					today.setStatus("private");

				// System.out.println(event..getVisibility());

				allToday.add(today);
				
				// return todayFinal;

			}
		}// for loop
		DailyEvents todayFinal = new DailyEvents(allToday);
		System.out.println(todayFinal.toString());
		Log.debug(todayFinal.toString());
		return todayFinal;
		// event.getCreator();
	}
	
	public static void main(String[] args) throws IOException, ParseException,
	GeneralSecurityException, URISyntaxException {
		new Quickstart2().getData();
		//this.getData();
	}

}