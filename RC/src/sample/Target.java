package sample;


//import java.awt.Color;
import robocode.AdvancedRobot;
import robocode.Condition;
import robocode.CustomEvent;

import com.google.code.appengine.awt.Color;


/**
 * Target - a sample robot by Mathew Nelson, and maintained by Flemming N. Larsen
 * 
 * Sits still.  Moves every time energy drops by 20.
 * This Robot demonstrates custom events.
 */
public class Target extends AdvancedRobot {

	int trigger; // Keeps track of when to move

	/**
	 * TrackFire's run method
	 */
	public void run() {
		// Set colors
		setBodyColor(Color.white);
		setGunColor(Color.white);
		setRadarColor(Color.white);
		
		// Initially, we'll move when life hits 80
		trigger = 80;
		// Add a custom event named "trigger hit",
		addCustomEvent(new Condition("triggerhit") { 
			public boolean test() {
				return (getEnergy() <= trigger);
			}
			;
		});
	}

	/**
	 * onCustomEvent handler
	 */	
	public void onCustomEvent(CustomEvent e) {
		// If our custom event "triggerhit" went off,
		if (e.getCondition().getName().equals("triggerhit")) {
			// Adjust the trigger value, or
			// else the event will fire again and again and again...
			trigger -= 20;
			out.println("Ouch, down to " + (int) (getEnergy() + .5) + " energy.");
			// move around a bit.
			turnLeft(65);
			ahead(100);
		}
	}
}
