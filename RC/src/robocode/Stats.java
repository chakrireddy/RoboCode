package robocode;

import java.util.Collections;
import java.util.List;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

public class Stats {
	
	public void updateBattleStats(String battlename, List<RoboScores> scores) {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		List<RoboScores> ranked_scores;
		Entity battle = getBattle(battlename);
		ranked_scores = rankRobots(scores);
		Long totalLevel = (long)0;
		Long rank = (long)0;
		Long sessionLevel = (Long)battle.getProperty("level");
		for (RoboScores score: ranked_scores) {
			
			/*
			 * Update Total robot score and level 
			 */
			rank++;
			Entity robot = getRobo(score.getRobot());
			Long newScore = (Long)robot.getProperty("score");
			Long roboLevel = (Long)robot.getProperty("level");
			newScore = newScore + score.getScore();
			Long diff = calculateLevelInc(sessionLevel, roboLevel, rank, (long)ranked_scores.size());
			roboLevel = roboLevel + (diff);
			totalLevel = totalLevel + roboLevel;
			robot.setProperty("score", newScore);
			robot.setProperty("level", roboLevel);
			datastore.put(robot);
			
			Entity bData = getBData(battlename, score.getRobot());
			String user = (String)bData.getProperty("bUser");
			Entity bUser = getBUser(user);
			Long points = (Long)bUser.getProperty("points");
			points = points + (diff);
			bUser.setProperty("points", points);
			datastore.put(bUser);
			/*
			 * Update User robot score and level 
			 */
		}
		
		battle.setProperty("level", totalLevel);
		datastore.put(battle);
	}
	
	
	Entity getRobo(String robot) {
		 DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		 Key key = KeyFactory.createKey("Robot", robot);
		 Entity robotEntry = null;
		 try {
			robotEntry = datastore.get(key);
			return(robotEntry);
		} catch (EntityNotFoundException e) {
			// TODO Auto-generated catch block
			return(null);
		}
	}
	
	Entity getBUser(String user) {
		 DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		 Key key = KeyFactory.createKey("RobotUser", user);
		 Entity robotEntry = null;
		 try {
			robotEntry = datastore.get(key);
			return(robotEntry);
		} catch (EntityNotFoundException e) {
			// TODO Auto-generated catch block
			return(null);
		}
	}
	
	Entity getBData(String battlename, String robot) {
		 DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		 String subkey = "udata" + battlename + robot;
		 Key key = KeyFactory.createKey("BattleData", subkey);
		 Entity robotEntry = null;
		 try {
			robotEntry = datastore.get(key);
			return(robotEntry);
		} catch (EntityNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("entity not found: "+subkey);
			return(null);
		}
	}
	Entity getBattle(String battle) {
		 DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		 Key key = KeyFactory.createKey("Battle", battle);
		 Entity robotEntry = null;
		 try {
			robotEntry = datastore.get(key);
			return(robotEntry);
		} catch (EntityNotFoundException e) {
			// TODO Auto-generated catch block
			return(null);
		}
	}
	
	List<RoboScores> rankRobots(List<RoboScores> scores)
	{
			Collections.sort(scores);
			for (RoboScores score : scores) {
				score.setRank(scores.indexOf(score));
			}
			return(scores);
	}
	
	Long calculateLevelInc (Long baseline, Long newLevel, Long rank, Long size) {
		
		Long diff = Math.abs((baseline - newLevel));
		Long newRank = (size/2 + 1) - rank;
		Long factor;
		
		if (diff == 0) {
			factor = (long)1;
		} else if (diff >= 100 && diff < 200) {
			factor = (long)2;
		} else {
			factor = (long)3;
		}
		return (factor * 10 * newRank);
	}
}
