package de.iolite.insys.data;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public class DailyEvents {

	List<GoogleEvent> todayEvents = new LinkedList<GoogleEvent>();

	public DailyEvents(List<GoogleEvent> list) {
		this.todayEvents = list;
		// TODO toString to mirror
		System.out.println(this.toString());
	}

	public DailyEvents() {

	}

	public List<GoogleEvent> getTodayEvents() {
		return todayEvents;
	}

	public void setTodayEvents(List<GoogleEvent> todayEvents) {
		this.todayEvents = todayEvents;
	}

	@Override
	public String toString() {
		// new line
		String newline = System.lineSeparator();
		String event = "";
		if (!todayEvents.isEmpty()) {
			for (int i = 0; i < todayEvents.size(); i++) {
				GoogleEvent dummy = todayEvents.get(i);
				int minutes = dummy.end.get(Calendar.MINUTE);
				int minutesStart = dummy.begin.get(Calendar.MINUTE);
				String ende = "";
				String start = "";
				if (minutes < 10) {
					ende = ":0" + minutes;
				} else
					ende = ":" + minutes;

				if (minutesStart < 10) {
					start = ":0" + minutesStart;
				} else
					start = ":" + minutesStart;

				event += dummy.Name
						+ "\n Location: "
						+ dummy.Location
						+
						// "\n Status:"+ dummy.Status +
						"\n type: "
						+ dummy.Color
						+
						// "\n Start: "+dummy.begin.get(Calendar.HOUR_OF_DAY)+start+
						// "\n End: "+dummy.end.get(Calendar.HOUR_OF_DAY)+ende+"\n"
						// + newline;

						"\n " + dummy.begin.get(Calendar.HOUR_OF_DAY) + start
						+ " - " + dummy.end.get(Calendar.HOUR_OF_DAY) + ende
						+ "\n" + newline;

			}
			return event;
		} else {
			return event + "today no upcoming events";
		}
	}

}
