package it.unibg.cs.jtvguide.gui;

import it.unibg.cs.jtvguide.UserPreferences;
import it.unibg.cs.jtvguide.model.Program;
import it.unibg.cs.jtvguide.model.Schedule;
import it.unibg.cs.jtvguide.model.XMLTVScheduleInspector;
import it.unibg.cs.jtvguide.xmltv.XMLTVCommander;
import it.unibg.cs.jtvguide.xmltv.XMLTVParserImpl;

import java.awt.GridLayout;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.UIManager;

public class jTVGuide extends JFrame implements Runnable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9221049871768124350L;
	private static Thread thread;
	JPanel jp = new JPanel();
	Schedule schedule;

	public jTVGuide(String title) {
		super(title);
		add(jp);
		XMLTVCommander xmltvc = new XMLTVCommander();
		XMLTVParserImpl xmltvParser = new XMLTVParserImpl();
		int tries = 0;

		boolean loaded = UserPreferences.loadFromXMLFile();
		while (!loaded || !UserPreferences.getXmltvConfigFile().exists()
				|| UserPreferences.getXmltvConfigFile().length() == 0) {
			System.out.println("Configuring jTVGuide and XMLTV...");
			xmltvc.configureXMLTV();
			UserPreferences.saveToXMLFile();
			loaded = UserPreferences.loadFromXMLFile();
		}

		boolean parsed = false;
		while (parsed == false && tries <= 3) {
			if (!new XMLTVScheduleInspector().isUpToDate()) {
				System.out.println("Updating schedule...");
				xmltvc.downloadSchedule();
			}
			if (tries >= 1) {
				System.out
				.println("Couldn't parsing. Downloading a new schedule.");
				UserPreferences.getXmltvOutputFile().delete();
				xmltvc.downloadSchedule();
			}
			if (tries == 4)
				throw new RuntimeException(
				"Couldn't download or parse schedule");
			System.out.println("Trying to parse schedule...");
			parsed = xmltvParser.parse();
			tries++;
		}
		schedule = xmltvParser.getSchedule();
		System.out.println("Schedule parsed correctly.");
		thread = new Thread(this);
		thread.start();
	}

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		jTVGuide jtv = new jTVGuide("jTVGuide v2.0");
		jtv.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jtv.setSize(1000, 800);
		jtv.setVisible(true);

	}

	public void run() {
		while (true) {
			if (schedule != null) {
				jp.removeAll();	
				List<Program> lk = schedule.getOnAirPrograms();
				List<Program> lp = schedule.getUpcomingPrograms();
				jp.setLayout(new GridLayout(lk.size() + lp.size(),2 ));
				for (Program p : lk) {
					JProgressBar jb = new JProgressBar();
					jb.setStringPainted(true);
					jb.setValue(p.getCompletionPercentile());
					jp.add(new JLabel(p.toString()));
					jp.add(jb);
				}
				for (Program p : lp) {
					jp.add(new JLabel(p.toString()));
					jp.add(new JLabel(p.getInfo()));
				}
				jp.revalidate();
			}
			try {
				Thread.sleep(1000*30);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
