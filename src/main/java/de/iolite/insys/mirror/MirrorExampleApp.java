package de.iolite.insys.mirror;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.iolite.app.api.device.DeviceAPIException;
import de.iolite.app.api.device.access.Device;
import de.iolite.app.api.device.access.DeviceAPI;
import de.iolite.app.api.device.access.DeviceBooleanProperty;
import de.iolite.app.api.device.access.DeviceDoubleProperty;
import de.iolite.api.IOLITEAPINotResolvableException;
import de.iolite.api.IOLITEAPIProvider;
import de.iolite.api.IOLITEPermissionDeniedException;
import de.iolite.app.AbstractIOLITEApp;
import de.iolite.app.api.device.access.Device;
import de.iolite.app.api.device.access.DeviceAPI;
import de.iolite.app.api.frontend.FrontendAPI;
import de.iolite.app.api.frontend.FrontendAPIException;
import de.iolite.app.api.frontend.util.FrontendAPIUtility;
import de.iolite.app.api.storage.StorageAPI;
import de.iolite.app.api.storage.StorageAPIException;
import de.iolite.common.identifier.EntityIdentifier;
import de.iolite.common.lifecycle.exception.CleanUpFailedException;
import de.iolite.common.lifecycle.exception.InitializeFailedException;
import de.iolite.common.lifecycle.exception.StartFailedException;
import de.iolite.common.lifecycle.exception.StopFailedException;
import de.iolite.common.requesthandler.HTTPStatus;
import de.iolite.common.requesthandler.IOLITEHTTPRequest;
import de.iolite.common.requesthandler.IOLITEHTTPRequestHandler;
import de.iolite.common.requesthandler.IOLITEHTTPResponse;
import de.iolite.common.requesthandler.IOLITEHTTPStaticResponse;
import de.iolite.common.requesthandler.StaticResources;
import de.iolite.drivers.basic.DriverConstants;
import de.iolite.insys.data.DailyEvents;
import de.iolite.insys.data.Quickstart2;
import de.iolite.insys.mirror.api.MirrorApiException;
import de.iolite.insys.mirror.api.SimpleMirrorManager;
import de.iolite.utilities.time.series.Function;
import de.iolite.utilities.time.series.TimeInterval;
import de.iolite.utilities.time.series.DataEntries.AggregatedEntry;
import de.iolite.utilities.time.series.DataEntries.BooleanEntry;

/**
 * @author Hendrik Motza
 */
public class MirrorExampleApp extends AbstractIOLITEApp {

	private static final Logger LOG = LoggerFactory.getLogger(MirrorExampleApp.class);
	private static final String HTML_RESOURCES = "de/iolite/insys/mirror/html";
	private static final String APP_ID = "de.iolite.insys.mirror.MirrorExampleApp";

	private static final String MSG_ERR_RETRIEVE_FRONTENDAPI = "Could not retrieve instance of FrontendAPI!";
	private static final String MSG_ERR_RETRIEVE_STORAGEAPI = "Could not retrieve instance of StorageAPI!";
	private static final String MSG_ERR_REGISTER_FRONTEND_RESOURCES = "An error appeared during the registration of frontend resources. This could result in errors when trying to display the app gui!";

	private static final String VIEW_ID_CLOCK = "DateTimeView";
	private static final String ICON_URL_CLOCK = "DateTimeView/clock-icon.jpg";
	private static final String VIEW_URL_CLOCK = "DateTimeView/clock.html";
	
	// CALENDAR PAGE 
	private static final String VIEW_ID_CALENDAR = "CalendarView";
	//private static final String ICON_URL_CLOCK = "DateTimeView/clock-icon.jpg";
	private static final String VIEW_URL_CALENDAR = "CalendarView/calendar.html";

	private static final String VIEW_ID_WELCOME = "WelcomeView";
	private static final String ICON_URL_WELCOME = "WelcomeView/welcome.png";
	private static final String VIEW_URL_WELCOME = "WelcomeView/welcome.html";

	private static final String VIEW_ID_HELLO_WORLD = "WelcomeWorldView";
	private static final String ICON_URL_HELLO_WORLD = "WelcomeView/welcome_friends.jpg";
	private static final String VIEW_URL_HELLO_WORLD = "WelcomeView/welcome.html?world=true";

	private static final String VIEW_ID_QUOTE = "QuoteView";
	private static final String ICON_URL_QUOTE = "QuoteView/quote.png";
	private static final String VIEW_URL_QUOTE = "QuoteView/quote.html";

	private static final String VIEW_ID_WEATHER = "WeatherView";
	private static final String ICON_URL_WEATHER = "WeatherView/weather.jpg";
	private static final String VIEW_URL_WEATHER = "WeatherView/DummyWeather.html";

	private static final String VIEW_ID_LNDW = "LNDWView";
	private static final String ICON_URL_LNDW = "LNDWView/lndw.png";
	private static final String VIEW_URL_LNDW = "LNDWView/lndw.html";

	private Quote quoteOfTheDay = null;
	private ScheduledFuture<?> quoteUpdateThread = null;
	private DailyEvents calendar = null; 

	private SimpleMirrorManager mirrorManager;
	private DeviceAPI deviceAPI;
	private static final List<Quote> quotes = new ArrayList<Quote>();

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void cleanUpHook()
			throws CleanUpFailedException {
		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initializeHook()
			throws InitializeFailedException {
		quotes.add(new Quote("Wer kämpft, der kann verlieren, wer nicht kämpft, hat schon verloren", "Unbekannt"));
		quotes.add(new Quote("Wer sich nicht mehr wundern kann, ist seelisch bereits tot", "Albert Einstein"));
		quotes.add(new Quote("Wer einen Fehler gemacht hat und ihn nicht korrigiert, begeht einen zweiten", "Konfuzius"));
		quotes.add(new Quote("Entscheide lieber ungefähr richtig, als genau falsch", "Goethe"));
		quotes.add(new Quote("Der Schwache kann nicht verzeihen. Verzeihen ist eine Eigenschaft des Starken", "Mahatma Gandhi"));
		quotes.add(new Quote("Wenn du weißt, dass du etwas vermasseln wirst, dann vermassel es wenigstens ordentlich", "Cate Blanchett "));
		quotes.add(new Quote("Sei, was du scheinen willst", "Sokrates"));
		quotes.add(new Quote("Wenn du den wahren Charakter eines Menschen erkennen willst, dann gib ihm Macht", "Abraham Lincoln"));
		quotes.add(new Quote("Von einem Baum, der noch in Blüte steht, musst du nicht schon Früchte erwarten", "Karl Ferdinand Gutzkow"));
		quotes.add(new Quote("Wer an der Küste bleibt, kann keine neuen Ozeane entdecken.", "Fernando Magellan"));
		quotes.add(new Quote("Auch Umwege erweitern unseren Horizont.", "Ernst Ferstl"));
		quotes.add(new Quote("Jeder Tag, an dem du nicht lächelst, ist ein verlorener Tag.", "Charlie Chaplin"));
		quotes.add(new Quote("Gib jedem Tag die Chance, der schönste deines Lebens zu werden.", "Mark Twain"));
		
		
			try {
				Quickstart2 quiiii = new Quickstart2();
				calendar = quiiii.getData();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (GeneralSecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void startHook(final IOLITEAPIProvider context)
			throws StartFailedException {
		final FrontendAPI frontendApi;
		try {
			frontendApi = context.getAPI(FrontendAPI.class);
		}
		catch (final Exception e) {
			LOG.error(MSG_ERR_RETRIEVE_FRONTENDAPI, e);
			throw new StartFailedException(MSG_ERR_RETRIEVE_FRONTENDAPI, e);
		}
		
		// Device API gives access to devices connected to IOLITE
		try {
			LOG.debug("Ich starte hier");
			for (final Device device : context.getAPI(DeviceAPI.class).getDevices()) {
				if(device.getProfileIdentifier().equals(DriverConstants.PROFILE_WeatherStation_ID)){
					DeviceDoubleProperty temp = device.getDoubleProperty(DriverConstants.PROPERTY_outsideEnvironmentTemperature_ID);
					Double temperatur = temp.getValue();
					LOG.debug("DIE TEMPARATUR IIIIIIIIIIISSSST'{}' ", temperatur.toString());
					
				}
			}
		} catch (IOLITEAPINotResolvableException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOLITEPermissionDeniedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		

		final StorageAPI storageApi;
		try {
			storageApi = context.getAPI(StorageAPI.class);
		}
		catch (final Exception e) {
			LOG.error(MSG_ERR_RETRIEVE_STORAGEAPI, e);
			throw new StartFailedException(MSG_ERR_RETRIEVE_STORAGEAPI, e);
		}

		try {
			FrontendAPIUtility.registerPublicHandlers(frontendApi, StaticResources.scanClasspath(HTML_RESOURCES, getClass().getClassLoader()));
			frontendApi.registerPublicClasspathStaticResource("", HTML_RESOURCES + "/index.html");
			frontendApi.registerPublicRequestHandler(VIEW_URL_WELCOME, createHelloWorldRequestHandler(storageApi));
			frontendApi.registerPublicRequestHandler(VIEW_URL_QUOTE, createQuoteRequestHandler());
			frontendApi.registerPublicRequestHandler(VIEW_URL_CALENDAR, createCalendarRequestHandler());
		}
		catch (final FrontendAPIException e) {
			LOG.error(MSG_ERR_REGISTER_FRONTEND_RESOURCES, e);
		}

		try {
			this.mirrorManager = new SimpleMirrorManager(context, APP_ID);
			this.mirrorManager.createView(VIEW_ID_CLOCK, ICON_URL_CLOCK, VIEW_URL_CLOCK, "Clock", false);
			this.mirrorManager.createView(VIEW_ID_WEATHER, ICON_URL_WEATHER, VIEW_URL_WEATHER, "Weather", false);
			this.mirrorManager.createView(VIEW_ID_LNDW, ICON_URL_LNDW, VIEW_URL_LNDW, "Lange Nacht der Wissenschaften", false);
			this.mirrorManager.createView(VIEW_ID_WELCOME, ICON_URL_WELCOME, VIEW_URL_WELCOME, "Greeting");
			this.mirrorManager.createView(VIEW_ID_HELLO_WORLD, ICON_URL_HELLO_WORLD, VIEW_URL_HELLO_WORLD, "Greeting", false);
			this.mirrorManager.createView(VIEW_ID_CALENDAR, ICON_URL_HELLO_WORLD, VIEW_URL_CALENDAR, "Calendar", false);
			
		}
		catch (final IOLITEAPINotResolvableException e) {
			LOG.error("Could not provide views for mirrors! Please make sure that your IOLITE version is up to date!", e);
		}
		catch (final IOLITEPermissionDeniedException e) {
			LOG.error("Could not provide views for mirrors! App misses neccessary permissions!", e);
		}
		catch (final MirrorApiException e) {
			LOG.error("Could not create views!", e);
		}

		this.quoteUpdateThread = context.getScheduler().scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				try {
					final long position = Math.round(Math.random() * (quotes.size() - 1));
					MirrorExampleApp.this.quoteOfTheDay = quotes.get((int) position);
					MirrorExampleApp.this.mirrorManager.createView(VIEW_ID_QUOTE, ICON_URL_QUOTE,
							VIEW_URL_QUOTE + "?timestamp=" + System.currentTimeMillis(), "Quote of the day");
				}
				catch (final MirrorApiException e) {
					LOG.error("Could not create views!", e);
				}
			}
		}, 0, 1, TimeUnit.MINUTES);
		//LOG.debug("Mirror Views got registered!");
		//LOG.info("ExampleApp Path: "+frontendApi.getBaseURI());
		System.out.println("v1");
	}

	private IOLITEHTTPRequestHandler createHelloWorldRequestHandler(final StorageAPI storageApi) {
		return new IOLITEHTTPRequestHandler() {

			@Override
			public void handlerRemoved(final String mapping, final EntityIdentifier callerEntityID) {
				// nothing to do

			}

			@Override
			public IOLITEHTTPResponse handleRequest(final IOLITEHTTPRequest request, final EntityIdentifier callerEntityID, final String subPath) {
				try {
					final InputStream is = this.getClass().getClassLoader().getResourceAsStream(HTML_RESOURCES + "/" + VIEW_URL_WELCOME);
					if (is == null) {
						LOG.error("Resource not found: {}", HTML_RESOURCES + "/" + VIEW_URL_WELCOME);
						return new IOLITEHTTPStaticResponse(HTTPStatus.NotFound, IOLITEHTTPResponse.HTML_CONTENT_TYPE);
					}
					final String template = IOUtils.toString(is, "UTF-8");
					String replacement;
					if (Boolean.valueOf(request.getParameter("world"))) {
						replacement = "Welt";
					}
					else {
						try {
							replacement = storageApi.loadString("uName");
						}
						catch (final StorageAPIException e) {
							replacement = "";
						}
					}
					return new IOLITEHTTPStaticResponse(template.replaceFirst(Pattern.quote("{USERNAME}"), replacement),
							IOLITEHTTPResponse.HTML_CONTENT_TYPE);
				}
				catch (final Exception e) {
					LOG.error(e.getMessage(), e);
					return null;
				}
			}
		};
	}

	private IOLITEHTTPRequestHandler createQuoteRequestHandler() {

		return new IOLITEHTTPRequestHandler() {

			@Override
			public void handlerRemoved(final String mapping, final EntityIdentifier callerEntityID) {
				// nothing to do

			}

			@Override
			public IOLITEHTTPResponse handleRequest(final IOLITEHTTPRequest request, final EntityIdentifier callerEntityID, final String subPath) {
				try {
					final InputStream is = this.getClass().getClassLoader().getResourceAsStream(HTML_RESOURCES + "/" + VIEW_URL_QUOTE);
					if (is == null) {
						LOG.error("Resource not found: {}", HTML_RESOURCES + "/" + VIEW_URL_QUOTE);
						return new IOLITEHTTPStaticResponse(HTTPStatus.NotFound, IOLITEHTTPResponse.HTML_CONTENT_TYPE);
					}
					final String template = IOUtils.toString(is, "UTF-8");
					return new IOLITEHTTPStaticResponse(
							template.replaceFirst(Pattern.quote("{QUOTE}"), MirrorExampleApp.this.quoteOfTheDay.getQuote())
									.replaceFirst(Pattern.quote("{AUTHOR}"), MirrorExampleApp.this.quoteOfTheDay.getAuthor()),
							IOLITEHTTPResponse.HTML_CONTENT_TYPE);
				}
				catch (final Exception e) {
					LOG.error(e.getMessage(), e);
					return null;
				}
			}
		};

	}
	//TODO
	private IOLITEHTTPRequestHandler createCalendarRequestHandler() {

		return new IOLITEHTTPRequestHandler() {

			@Override
			public void handlerRemoved(final String mapping, final EntityIdentifier callerEntityID) {
				// nothing to do

			}

		    
			@Override
			public IOLITEHTTPResponse handleRequest(final IOLITEHTTPRequest request, final EntityIdentifier callerEntityID, final String subPath) {
				try {
					final InputStream is = this.getClass().getClassLoader().getResourceAsStream(HTML_RESOURCES + "/" + VIEW_URL_CALENDAR);
					if (is == null) {
						LOG.error("Resource not found: {}", HTML_RESOURCES + "/" + VIEW_URL_CALENDAR);
						return new IOLITEHTTPStaticResponse(HTTPStatus.NotFound, IOLITEHTTPResponse.HTML_CONTENT_TYPE);
					}
					final String template = IOUtils.toString(is, "UTF-8");

					String replacement = MirrorExampleApp.this.calendar.toString();
					
					String content = template.replaceFirst(Pattern.quote("{CALENDAR}"), replacement.replaceAll("\n","<br/>"));
	
					return new IOLITEHTTPStaticResponse(
							content,
									//.replaceFirst(Pattern.quote("{AUTHOR}"), MirrorExampleApp.this.quoteOfTheDay.getAuthor()),
							IOLITEHTTPResponse.HTML_CONTENT_TYPE);
				}
				catch (final Exception e) {
					LOG.error(e.getMessage(), e);
					return null;
				}
			}
		};

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void stopHook()
			throws StopFailedException {
		LOG.debug("Stopping APP ");
		this.quoteUpdateThread.cancel(false);
		if (this.mirrorManager != null) {
			this.mirrorManager.stopHook();
		}
	}
	
	/**
	 * Example method showing how to use the Device API.
	 */
	private void initializeDeviceManager() {
		// register a device observer
		//this.deviceAPI.setObserver(new DeviceAddAndRemoveLogger());
		LOG.debug("Bin drin");
		// go through all devices, and register a property observer for ON/OFF properties
		for (final Device device : this.deviceAPI.getDevices()) {
			if(device.getProfileIdentifier().equals(DriverConstants.PROFILE_WeatherStation_ID)){
				DeviceDoubleProperty temp = device.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_WeatherStation_outsideEnvironmentTemperature_ID);
				Double temperatur = temp.getValue();
				LOG.debug("DIE TEMPARATUR IIIIIIIIIIISSSST'{}' ", temperatur.toString());
				
			}
			
//			// each device has some properties (accessible under device.getProperties())
//			// let's get the 'on/off' status property
//			
//			final DeviceBooleanProperty onProperty = device.getBooleanProperty(DriverConstants.PROPERTY_on_ID);
//			if (onProperty != null) {
//				LOG.debug("device '{}' has ON/OFF property, current value: '{}'", device.getIdentifier(), onProperty.getValue());
//
//				onProperty.setObserver(new DeviceOnOffStatusLogger(device.getIdentifier()));
//			}
//		}
//
//		// go through all devices, and toggle ON/OFF properties
//		for (final Device device : this.deviceAPI.getDevices()) {
//			// let's get the 'on/off' status property
//			final DeviceBooleanProperty onProperty = device.getBooleanProperty(DriverConstants.PROPERTY_on_ID);
//			final Boolean onValue;
//			if (onProperty != null && (onValue = onProperty.getValue()) != null) {
//				LOG.debug("toggling device '{}'", device.getIdentifier());
//				try {
//					onProperty.requestValueUpdate(!onValue);
//				}
//				catch (final DeviceAPIException e) {
//					LOG.error("Failed to control device", e);
//				}
//			}
//		}
//
//		// go through all devices, and print ON/OFF and POWER USAGE property history datas
//		for (final Device device : this.deviceAPI.getDevices()) {
//			// ON/OFF history data
//			final DeviceBooleanProperty onProperty = device.getBooleanProperty(DriverConstants.PROPERTY_on_ID);
//			if (onProperty != null) {
//				// retrieve the on/off history of last hour
//				final long hourMillis = TimeUnit.SECONDS.toMillis(60 * 60);
//				final List<BooleanEntry> onHistory;
//				try {
//					onHistory = onProperty.getValuesSince(System.currentTimeMillis() - hourMillis);
//				}
//				catch (final DeviceAPIException e) {
//					LOG.error("Failed to retrieve the history of property '{}'", onProperty.getKey(), e);
//					continue;
//				}
//				LOG.debug("Got '{}' historical values for property '{}'", onHistory.size(), onProperty.getKey());
//				// log history values
//				final DateFormat dateFormat = DateFormat.getTimeInstance();
//				for (final BooleanEntry historyEntry : onHistory) {
//					LOGGER.debug("At '{}' the value was '{}'", dateFormat.format(new Date(historyEntry.time)), historyEntry.value);
//				}
//			}
//
//			// POWER USAGE history data
//			final DeviceDoubleProperty powerUsage = device.getDoubleProperty(DriverConstants.PROPERTY_powerUsage_ID);
//			if (powerUsage != null) {
//				LOGGER.debug("Reading today's hourly power usage data from device '{}':", device.getIdentifier());
//				List<AggregatedEntry> history;
//				try {
//					history = powerUsage.getAggregatedValuesOf(System.currentTimeMillis(), TimeInterval.DAY, TimeInterval.HOUR, Function.AVERAGE);
//					for (final AggregatedEntry entry : history) {
//						LOGGER.debug("The device used an average of {} Watt at '{}'.", entry.getAggregatedValue(),
//								DateFormat.getTimeInstance().format(new Date(entry.getEndTime())));
//					}
//				}
//				catch (final DeviceAPIException e) {
//					LOGGER.error("Failed to retrieve history data of device", e);
//				}
//			}
		}
	}

}
