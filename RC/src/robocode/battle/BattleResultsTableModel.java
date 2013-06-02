/*******************************************************************************
 * Copyright (c) 2001, 2007 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://robocode.sourceforge.net/license/cpl-v10.html
 *
 * Contributors:
 *     Mathew A. Nelson
 *     - Initial API and implementation
 *     Flemming N. Larsen
 *     - Replaced ContestantPeerVector with plain Vector
 *     - Added Rank column
 *     - Ported to Java 5
 *     - Optimized
 *     - Code cleanup
 *     - Updated to use methods from the Logger and StringUtil, which
 *       replaces methods that have been (re)moved from the robocode.util.Utils
 *     - Changed the column names to be more informative and equal in width
 *     Robert D. Maupin
 *     - Replaced old collection types like Vector and Hashtable with
 *       synchronized List and HashMap
 *******************************************************************************/
package robocode.battle;


import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.mortbay.log.Log;


import robocode.RoboScores;
import robocode.Robocode;
import robocode.Stats;
import robocode.peer.ContestantPeer;
import robocode.peer.ContestantStatistics;
import robocode.peer.TeamPeer;
import robocode.text.StringUtil;
import services.MailService;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;


/**
 * @author Mathew A. Nelson (original)
 * @author Flemming N. Larsen (contributor)
 * @author Robert D. Maupin (contributor)
 */
@SuppressWarnings("serial")
public class BattleResultsTableModel {
	private Battle battle;
	private String title;
	Logger log = Logger.getLogger(BattleResultsTableModel.class.getName());
	public BattleResultsTableModel(Battle battle) {
		super();
		this.battle = battle;
	}

	public int getColumnCount() {
		return 12;
	}

	
	public String getColumnName(int col) {
		switch (col) {
		case 0:
			return "Rank";

		case 1:
			return "Robot Name";

		case 2:
			return "Total Score";

		case 3:
			return "Survival";

		case 4:
			return "Surv Bonus";

		case 5:
			return "Bullet Dmg";

		case 6:
			return "Bullet Bonus";

		case 7:
			return "Ram Dmg * 2";

		case 8:
			return "Ram Bonus";

		case 9:
			return " 1sts ";

		case 10:
			return " 2nds ";

		case 11:
			return " 3rds ";

		default:
			return "";
		}
	}

	public int getRowCount() {
		return battle.getContestants().size();
	}

	public String getTitle() {
		if (title == null) {
			int round = battle.getRoundNum();

			title = "Results for " + round + " round";
			if (round > 1) {
				title += 's';
			}
		}
		return title;
	}

	public Object getValueAt(int row, int col) {
		List<ContestantPeer> orderedContestants = new ArrayList<ContestantPeer>(battle.getContestants());

		Collections.sort(orderedContestants);

		ContestantPeer r = orderedContestants.get(row);
		ContestantStatistics statistics = r.getStatistics();

		switch (col) {
		case 0: {
			int place = row + 1;

			while (place < getRowCount()
					&& statistics.getTotalScore() == orderedContestants.get(place).getStatistics().getTotalScore()) {
				place++;
			}
			return StringUtil.getPlacementString(place);
		}

		case 1:
			return ((r instanceof TeamPeer) ? "Team: " : "") + r.getName();

		case 2:
			//storeResults(r.getName(),(int) (statistics.getTotalScore() + 0.5));
			return "" + (int) (statistics.getTotalScore() + 0.5);

		case 3:
			return "" + (int) (statistics.getTotalSurvivalScore() + 0.5);

		case 4:
			return "" + (int) (statistics.getTotalLastSurvivorBonus() + 0.5);

		case 5:
			return "" + (int) (statistics.getTotalBulletDamageScore() + 0.5);

		case 6:
			return "" + (int) (statistics.getTotalBulletKillBonus() + 0.5);

		case 7:
			return "" + (int) (statistics.getTotalRammingDamageScore() + 0.5);

		case 8:
			return "" + (int) (statistics.getTotalRammingKillBonus() + 0.5);

		case 9:
			return "" + statistics.getTotalFirsts();

		case 10:
			return "" + statistics.getTotalSeconds();

		case 11:
			return "" + statistics.getTotalThirds();

		default:
			return "";
		}
	}

	private void storeResults() {
		List<ContestantPeer> orderedContestants = new ArrayList<ContestantPeer>(battle.getContestants());

		Collections.sort(orderedContestants);

		
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		 Entity roboEntity = new Entity("Score", Robocode.battlename);
		for (int row = 0; row < getRowCount(); row++) {
			ContestantPeer r = orderedContestants.get(row);
			ContestantStatistics statistics = r.getStatistics();
			int score = (int) (statistics.getTotalScore() + 0.5);
			String robotName = r.getName();
			roboEntity.setProperty(robotName,score);	        
			}
		 datastore.put(roboEntity);
		
	}

	// Used for printing to the console only
	public void print(PrintStream out) {
		storeResults();
		log.info(getTitle());
		out.println(getTitle());
		List<RoboScores> roboScoreList = new ArrayList<RoboScores>();
		for (int col = 1; col < getColumnCount(); col++) {
			out.print(getColumnName(col) + "\t");
		}

		out.println();
		MailService mailService = new MailService(this);
		for (int row = 0; row < getRowCount(); row++) {
			RoboScores roboScore = new RoboScores();
			
			out.print(getValueAt(row, 0) + ": ");
			//log.info(getValueAt(row, 0) + ": ");
			for (int col = 1; col < getColumnCount(); col++) {
				List<ContestantPeer> orderedContestants = new ArrayList<ContestantPeer>(battle.getContestants());

				Collections.sort(orderedContestants);

				ContestantPeer r = orderedContestants.get(row);
				ContestantStatistics statistics = r.getStatistics();
				if(col == 1){
					roboScore.setRobot(r.getName());
				}else if(col ==2){			
					roboScore.setScore((int) (statistics.getTotalScore() + 0.5));
					roboScoreList.add(roboScore);
				}
				out.print(getValueAt(row, col) + "\t");
				//log.info(getValueAt(row, col) + "\t");
			}
			out.println();
		}
		
		for (int col = 1; col < getColumnCount(); col++) {
			log.info(getColumnName(col) + "\t");			
		}
		for (int row = 0; row < getRowCount(); row++) {
			log.info(getValueAt(row, 0) + ": ");
			for (int col = 1; col < getColumnCount(); col++) {
				log.info(getValueAt(row, col) + "\t");
			}
		}
		//mailService.sendMail(null, null, null);
		
		Stats stat = new Stats();
		stat.updateBattleStats(battle.battlename, roboScoreList);
	}

	/*public void saveToFile(String filename, boolean append) {
		try {
			PrintStream out = new PrintStream(new FileOutputStream(filename, append));

			out.println(DateFormat.getDateTimeInstance().format(new Date()));

			out.println(getTitle());

			for (int col = 0; col < getColumnCount(); col++) {
				if (col > 0) {
					out.print(',');
				}
				out.print(getColumnName(col));
			}

			out.println();

			for (int row = 0; row < getRowCount(); row++) {
				for (int col = 0; col < getColumnCount(); col++) {
					if (col > 0) {
						out.print(',');
					}
					out.print(getValueAt(row, col));
				}
				out.println();
			}

			out.println("$");

			out.close();

		} catch (IOException e) {
			log.info(e.getMessage());
			return;
		}
	}*/
}
