package it.unibg.cs.jtvguide.xmltv;

import it.unibg.cs.jtvguide.UserPreferences;
import it.unibg.cs.jtvguide.interfaces.xmltv.XMLTVParser;
import it.unibg.cs.jtvguide.model.Channel;
import it.unibg.cs.jtvguide.model.ChannelMap;
import it.unibg.cs.jtvguide.model.Program;
import it.unibg.cs.jtvguide.model.Schedule;
import it.unibg.cs.jtvguide.util.DateFormatter;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class XMLTVParserImpl implements XMLTVParser {

	private Schedule schedule;
	private ChannelMap cm;

	public XMLTVParserImpl() {
		cm = new ChannelMap();
		schedule = new Schedule(cm);
	}

	public Schedule getSchedule() {
		return schedule;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean parse() {
		SAXBuilder builder = new SAXBuilder(true);
		Document doc = null;

		try {
			doc = builder.build(UserPreferences.getXmltvOutputFile());
		} catch (JDOMException e) {
			return false;
		} catch (IOException e) {
			return false;
		}

		if (doc != null) {
			List<Element> channels = doc.getRootElement()
					.getChildren("channel");
			Iterator<Element> channelsIterator = channels.iterator();

			String id;
			String displayName;

			while (channelsIterator.hasNext()) {
				Element channel = channelsIterator.next();
				id = channel.getAttributeValue("id");
				displayName = channel.getChildText("display-name");

				/* Creare un nuovo canale e aggiungerlo alla lista dei canali */

				Channel c = new Channel(id, displayName);
				cm.add(id, c);
			}

			/*
			 * Parsing degli show
			 */

			List<Element> shows = doc.getRootElement().getChildren("programme");

			Iterator<Element> showsIterator = shows.iterator();
			Date startDate, stopDate;
			String channelID;
			String title;
			Program p;
			boolean reverse = false;

			while (showsIterator.hasNext()) {
				Element show = showsIterator.next();
				startDate = null;
				stopDate = null;
				String start = show.getAttributeValue("start");
				startDate = DateFormatter.formatString(start.substring(0, 12));
				String stop = show.getAttributeValue("stop");
				if (stop != null)
					stopDate = DateFormatter
							.formatString(stop.substring(0, 12));
				else
					reverse = true;
				channelID = show.getAttributeValue("channel");
				title = show.getChildText("title");
				/*
				 * Creare un nuovo show e aggiungerlo alla lista gli show
				 */
				p = new Program(startDate, stopDate, cm.get(channelID), title);
				schedule.add(p);
			}
			Collections.sort(schedule.getScheduleList());

			if (reverse) {
				List<Program> s = schedule.getScheduleList();
				for (Program program : s) {
					if (program.getStopDate() == null) {
						for (Program programUpcoming : s) {
							if (program.getStartDate().compareTo(
									programUpcoming.getStartDate()) < 0
									&& program.getChannel().equals(
											programUpcoming.getChannel())) {
								program.setStopDate(programUpcoming
										.getStartDate());
								break;
							}
						}
					}
				}
			}
		}
		return true;
	}
}