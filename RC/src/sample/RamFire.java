package sample;


//import java.awt.Color;
import robocode.HitRobotEvent;
import robocode.Robot;
import robocode.ScannedRobotEvent;

import com.google.code.appengine.awt.Color;


/**
 * RamFire - a sample robot by Mathew Nelson, and maintained by Flemming N. Larsen
 * 
 * Drives at robots trying to ram them.
 * Fires when it hits them.
 */
public class RamFire extends Robot {
	int turnDirection = 1; // Clockwise or counterclockwise

	/**
	 * run: Spin around looking for a target
	 */
	public void run() {
		// Set colors
		setBodyColor(Color.lightGray);
		setGunColor(Color.gray);
		setRadarColor(Color.darkGray);

		while (true) {
			turnRight(5 * turnDirection);
		}
	}

	/**
	 * onScannedRobot:  We have a target.  Go get it.
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
				
		if (e.getBearing() >= 0) {
			turnDirection = 1;
		} else {
			turnDirection = -1;
		}
			
		turnRight(e.getBearing());
		ahead(e.getDistance() + 5);
		scan(); // Might want to move ahead again!
	}

	/**
	 * onHitRobot:  Turn to face robot, fire hard, and ram him again!
	 */
	public void onHitRobot(HitRobotEvent e) {
		if (e.getBearing() >= 0) {
			turnDirection = 1;
		} else {
			turnDirection = -1;
		}
		turnRight(e.getBearing());
		
		// Determine a shot that won't kill the robot...
		// We want to ram him instead for bonus points
		if (e.getEnergy() > 16) {
			fire(3);
		} else if (e.getEnergy() > 10) {
			fire(2);
		} else if (e.getEnergy() > 4) {
			fire(1);
		} else if (e.getEnergy() > 2) {
			fire(.5);
		} else if (e.getEnergy() > .4) {
			fire(.1);
		}
		ahead(40); // Ram him again!
	}
}
