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
 *     - Code cleanup & optimizations
 *     - Removed getBattleView().setDoubleBuffered(false) as BufferStrategy is
 *       used now
 *     - Replaced FileSpecificationVector, RobotPeerVector, and
 *       RobotClassManagerVector with plain Vector
 *     - Added check for if GUI is enabled before using graphical components
 *     - Added restart() method
 *     - Ported to Java 5
 *     - Added support for the replay feature
 *     - Removed the clearBattleProperties()
 *     - Updated to use methods from FileUtil and Logger, which replaces methods
 *       that have been (re)moved from the robocode.util.Utils class
 *     - Added PauseResumeListener interface, addListener(), removeListener(),
 *       notifyBattlePaused(), notifyBattleResumed() for letting listeners
 *       receive notifications when the game is paused or resumed
 *     - Added missing functionality in to support team battles in
 *       startNewBattle(BattleSpecification spec, boolean replay)
 *     - Added missing close() on FileInputStreams and FileOutputStreams
 *     - isPaused() is now synchronized
 *     - Extended sendResultsToListener() to handle teams as well as robots
 *     Luis Crespo
 *     - Added debug step feature, including the nextTurn(), shouldStep(),
 *       startNewRound()
 *     Robert D. Maupin
 *     - Replaced old collection types like Vector and Hashtable with
 *       synchronized List and HashMap
 *******************************************************************************/
package robocode.manager;


import static robocode.io.Logger.log;

import java.io.*;
import java.security.AccessControlContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.swing.JFileChooser;

import org.mortbay.log.Log;

import com.google.appengine.api.ThreadManager;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.spi.ServiceFactoryFactory;

import robocode.battle.Battle;
import robocode.battle.BattleProperties;
import robocode.battle.BattleResultsTableModel;
import robocode.battlefield.BattleField;
import robocode.battlefield.DefaultBattleField;
import robocode.control.BattleSpecification;
import robocode.control.RobocodeListener;
import robocode.control.RobotResults;
import robocode.io.FileUtil;
import robocode.peer.ContestantStatistics;
import robocode.peer.ContestantPeer;
import robocode.peer.RobotPeer;
import robocode.peer.TeamPeer;
import robocode.peer.robot.RobotClassManager;
import robocode.repository.FileSpecification;
import robocode.repository.RobotSpecification;
import robocode.repository.TeamSpecification;
import robocode.security.RobocodeSecurityManager;


/**
 * @author Mathew A. Nelson (original)
 * @author Flemming N. Larsen (contributor)
 * @author Luis Crespo (contributor)
 * @author Robert D. Maupin (contributor)
 */
public class BattleManager {
	private BattleProperties battleProperties = new BattleProperties();
	private String battleFilename;
	private String battlePath;
	private Battle battle;
	private boolean battleRunning;
	private int pauseCount;
	private String resultsFile;
	private RobocodeManager manager;
	private int stepTurn;
Logger log = Logger.getLogger(BattleManager.class.getName());
	private List<PauseResumeListener> pauseResumeListeners;

	public interface PauseResumeListener {
		public void battlePaused();
		public void battleResumed();
	}

	/**
	 * Steps for a single turn, then goes back to paused
	 */
	public void nextTurn() {
		if (battleRunning) {
			stepTurn = battle.getCurrentTime() + 1;
		}
	}

	/**
	 * If the battle is paused, this method determines if it should perform one turn and then stop again.
	 *
	 * @return true if the battle should perform one turn, false otherwise
	 */
	public boolean shouldStep() {
		// This code assumes it is called only if the battle is paused.
		return stepTurn > battle.getCurrentTime();
	}

	/**
	 * This method should be called to inform the battle manager that a new round is starting
	 */
	public void startNewRound() {
		stepTurn = 0;
	}

	public BattleManager(RobocodeManager manager) {
		log.info("Battlemanager initializing");
		this.manager = manager;
	}

	public void stop(boolean showResultsDialog) {
		if (getBattle() != null) {
			if (manager.isSoundEnabled()) {
				manager.getSoundManager().stopBackgroundMusic();
			}

			getBattle().stop(showResultsDialog);
		}
	}

	public void restart() {
		stop(false);
		startNewBattle(battleProperties, false, false);
	}

	public void replay() {
		startNewBattle(battleProperties, false, true);
	}

	public void startNewBattle(BattleProperties battleProperties, boolean exitOnComplete, boolean replay) {
		log.info("Battle starting...");
		this.battleProperties = battleProperties;

		List<RobotClassManager> battlingRobotsList = Collections.synchronizedList(new ArrayList<RobotClassManager>());
		RobotRepositoryManager robotRepositoryManager = manager.getRobotRepositoryManager();
		if (battleProperties.getSelectedRobots() != null) {
			StringTokenizer tokenizer = new StringTokenizer(battleProperties.getSelectedRobots(), ",");

			while (tokenizer.hasMoreTokens()) {
				String bot = tokenizer.nextToken();
				RobotSpecification robotSpec = new RobotSpecification(bot, "", false);
				RobotClassManager classManager = new RobotClassManager(robotSpec);
				battlingRobotsList.add(classManager);
			}
		}
		startNewBattle(battlingRobotsList, exitOnComplete, replay, null);
	}
	public void startNewBattle1(BattleProperties battleProperties, boolean exitOnComplete, boolean replay) {
		this.battleProperties = battleProperties;

		List<FileSpecification> robotSpecificationsList = manager.getRobotRepositoryManager().getRobotRepository().getRobotSpecificationsList(
				false, false, false, false, false, false);

		List<RobotClassManager> battlingRobotsList = Collections.synchronizedList(new ArrayList<RobotClassManager>());

		if (battleProperties.getSelectedRobots() != null) {
			StringTokenizer tokenizer = new StringTokenizer(battleProperties.getSelectedRobots(), ",");

			while (tokenizer.hasMoreTokens()) {
				String bot = tokenizer.nextToken();

				for (FileSpecification fileSpec : robotSpecificationsList) {
					if (fileSpec.getNameManager().getUniqueFullClassNameWithVersion().equals(bot)) {
						if (fileSpec instanceof RobotSpecification) {
							battlingRobotsList.add(new RobotClassManager((RobotSpecification) fileSpec));
							break;
						} else if (fileSpec instanceof TeamSpecification) {
							TeamSpecification currentTeam = (TeamSpecification) fileSpec;
							TeamPeer teamManager = new TeamPeer(currentTeam.getName());

							StringTokenizer teamTokenizer = new StringTokenizer(currentTeam.getMembers(), ",");

							while (teamTokenizer.hasMoreTokens()) {
								bot = teamTokenizer.nextToken();
								RobotSpecification match = null;

								for (FileSpecification teamFileSpec : robotSpecificationsList) {
									// Teams cannot include teams
									if (teamFileSpec instanceof TeamSpecification) {
										continue;
									}
									if (teamFileSpec.getNameManager().getUniqueFullClassNameWithVersion().equals(bot)) {
										// Found team member
										match = (RobotSpecification) teamFileSpec;
										if (currentTeam.getRootDir().equals(teamFileSpec.getRootDir())
												|| currentTeam.getRootDir().equals(teamFileSpec.getRootDir().getParentFile())) {
											break;
										}
										// else, still looking
									}
								}
								battlingRobotsList.add(new RobotClassManager(match, teamManager));
							}
							break;
						}
					}
				}
			}
		}
		startNewBattle(battlingRobotsList, exitOnComplete, replay, null);
	}
	public void startNewBattle(BattleSpecification spec, boolean replay) {
		battleProperties = new BattleProperties();
		battleProperties.setBattlefieldWidth(spec.getBattlefield().getWidth());
		battleProperties.setBattlefieldHeight(spec.getBattlefield().getHeight());
		battleProperties.setGunCoolingRate(spec.getGunCoolingRate());
		battleProperties.setInactivityTime(spec.getInactivityTime());
		battleProperties.setNumRounds(spec.getNumRounds());
		battleProperties.setSelectedRobots(spec.getRobots());

		List<FileSpecification> robotSpecificationsList = manager.getRobotRepositoryManager().getRobotRepository().getRobotSpecificationsList(
				false, false, false, false, false, false);
		List<RobotClassManager> battlingRobotsList = Collections.synchronizedList(new ArrayList<RobotClassManager>());

		for (robocode.control.RobotSpecification battleRobotSpec : spec.getRobots()) {
			if (battleRobotSpec == null) {
				break;
			}

			String bot = battleRobotSpec.getClassName();

			if (!(battleRobotSpec.getVersion() == null || battleRobotSpec.getVersion().length() == 0)) {
				bot += ' ' + battleRobotSpec.getVersion();
			}

			boolean found = false;

			for (FileSpecification fileSpec : robotSpecificationsList) {
				if (fileSpec.getNameManager().getUniqueFullClassNameWithVersion().equals(bot)) {
					if (fileSpec instanceof RobotSpecification) {
						RobotClassManager rcm = new RobotClassManager((RobotSpecification) fileSpec);

						rcm.setControlRobotSpecification(battleRobotSpec);
						battlingRobotsList.add(rcm);
						found = true;
						break;
					} else if (fileSpec instanceof TeamSpecification) {
						TeamSpecification currentTeam = (TeamSpecification) fileSpec;
						TeamPeer teamManager = new TeamPeer(currentTeam.getName());

						StringTokenizer teamTokenizer = new StringTokenizer(currentTeam.getMembers(), ",");

						while (teamTokenizer.hasMoreTokens()) {
							bot = teamTokenizer.nextToken();
							RobotSpecification match = null;

							for (FileSpecification teamFileSpec : robotSpecificationsList) {
								// Teams cannot include teams
								if (teamFileSpec instanceof TeamSpecification) {
									continue;
								}
								if (teamFileSpec.getNameManager().getUniqueFullClassNameWithVersion().equals(bot)) {
									// Found team member
									match = (RobotSpecification) teamFileSpec;
									if (currentTeam.getRootDir().equals(teamFileSpec.getRootDir())
											|| currentTeam.getRootDir().equals(teamFileSpec.getRootDir().getParentFile())) {
										found = true;
										break;
									}
									// else, still looking
								}
							}
							RobotClassManager rcm = new RobotClassManager(match, teamManager);

							rcm.setControlRobotSpecification(battleRobotSpec);
							battlingRobotsList.add(rcm);
						}
						break;
					}
				}
			}
			if (!found) {
				log("Aborting battle, could not find robot: " + bot);
				if (manager.getListener() != null) {
					manager.getListener().battleAborted(spec);
				}
				return;
			}
		}
		startNewBattle(battlingRobotsList, false, replay, spec);
	}

	private void startNewBattle(List<RobotClassManager> battlingRobotsList, boolean exitOnComplete, boolean replay,
			BattleSpecification battleSpecification) {
		log.info("Preparing battle...");
		log("Preparing battle...");
		if (battle != null) {
			battle.stop();
		}

		BattleField battleField = new DefaultBattleField(battleProperties.getBattlefieldWidth(),
				battleProperties.getBattlefieldHeight());
		log.info("battlefield");
		if (manager.isGUIEnabled()) {
			manager.getWindowManager().getRobocodeFrame().getBattleView().setBattleField(battleField);
		}
		battle = new Battle(battleField, manager);
		battle.setExitOnComplete(exitOnComplete);

		// Only used when controlled by RobocodeEngine
		battle.setBattleSpecification(battleSpecification);

		// Set stuff the view needs to know
		battle.setProperties(battleProperties);
//		Thread battleThread = new Thread(Thread.currentThread().getThreadGroup(), battle);
		log.info("before Thread manager");
		Thread battleThread = ThreadManager.createBackgroundThread(battle);
		log.info("after thread manager");
		//Thread battleThread = new Thread(battle);
		//battleThread.setPriority(Thread.NORM_PRIORITY);
		//battleThread.setName("Battle Thread");
		battle.setBattleThread(battleThread);
		battle.setReplay(replay);

		/*if (!System.getProperty("NOSECURITY", "false").equals("true")) {
			((RobocodeSecurityManager) System.getSecurityManager()).addSafeThread(battleThread);
			((RobocodeSecurityManager) System.getSecurityManager()).setBattleThread(battleThread);
		}
*/
		if (manager.isGUIEnabled()) {
			robocode.battleview.BattleView battleView = manager.getWindowManager().getRobocodeFrame().getBattleView();

			battleView.setVisible(true);
			battleView.setInitialized(false);
		}

		for (RobotClassManager robotClassMgr : battlingRobotsList) {
			battle.addRobot(robotClassMgr);
		}

		if (manager.isGUIEnabled()) {
			robocode.dialog.RobocodeFrame frame = manager.getWindowManager().getRobocodeFrame();

			frame.getRobocodeMenuBar().getBattleSaveAsMenuItem().setEnabled(true);
			frame.getRobocodeMenuBar().getBattleSaveMenuItem().setEnabled(true);

			if (frame.getPauseButton().getText().equals("Resume")) {
				frame.pauseResumeButtonActionPerformed();
			}

			manager.getRobotDialogManager().setActiveBattle(battle);
		}
		battleThread.start();
	}

	public String getBattleFilename() {
		return battleFilename;
	}

	public void setBattleFilename(String newBattleFilename) {
		battleFilename = newBattleFilename;
	}

	public synchronized boolean isPaused() {
		return (pauseCount != 0);
	}

	public synchronized void pauseBattle() {
		pauseCount++;

		if (pauseCount == 1) {
			notifyBattlePaused();
		}
	}

	public String getBattlePath() {
		if (battlePath == null) {
			battlePath = System.getProperty("BATTLEPATH");
			if (battlePath == null) {
				battlePath = "battles";
			}
			battlePath = new File(FileUtil.getCwd(), battlePath).getAbsolutePath();
		}
		return battlePath;
	}

	public void saveBattle() {
		pauseBattle();
		saveBattleProperties();
		resumeBattle();
	}

	public void saveBattleAs() {
		pauseBattle();
		File f = new File(getBattlePath());

		JFileChooser chooser;

		chooser = new JFileChooser(f);

		javax.swing.filechooser.FileFilter filter = new javax.swing.filechooser.FileFilter() {
			@Override
			public boolean accept(File pathname) {
				if (pathname.isDirectory()) {
					return false;
				}
				String fn = pathname.getName();
				int idx = fn.lastIndexOf('.');
				String extension = "";

				if (idx >= 0) {
					extension = fn.substring(idx);
				}
				if (extension.equalsIgnoreCase(".battle")) {
					return true;
				}
				return false;
			}

			@Override
			public String getDescription() {
				return "Battles";
			}
		};

		chooser.setFileFilter(filter);
		int rv = chooser.showSaveDialog(manager.getWindowManager().getRobocodeFrame());

		if (rv == JFileChooser.APPROVE_OPTION) {
			battleFilename = chooser.getSelectedFile().getPath();
			int idx = battleFilename.lastIndexOf('.');
			String extension = "";

			if (idx > 0) {
				extension = battleFilename.substring(idx);
			}
			if (!(extension.equalsIgnoreCase(".battle"))) {
				battleFilename += ".battle";
			}
			saveBattleProperties();
		}
		resumeBattle();
	}

	public void saveBattleProperties() {
		if (battleProperties == null) {
			log("Cannot save null battle properties");
			return;
		}
		if (battleFilename == null) {
			saveBattleAs();
			return;
		}
		FileOutputStream out = null;

		try {
			out = new FileOutputStream(battleFilename);

			battleProperties.store(out, "Battle Properties");
		} catch (IOException e) {
			log.info("IO Exception saving battle properties: ");
			log("IO Exception saving battle properties: " + e);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {}
			}
		}
	}

	public void loadBattleProperties() {
		FileInputStream in = null;

		try {
			in = new FileInputStream(battleFilename);
			getBattleProperties().load(in);
		} catch (FileNotFoundException e) {
			log.info("No file " + battleFilename + " found, using defaults.");
			log("No file " + battleFilename + " found, using defaults.");
		} catch (IOException e) {
			log.info("IO Exception reading " + battleFilename + ": ");
			log("IO Exception reading " + battleFilename + ": " + e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {}
			}
		}
	}

	public Battle getBattle() {
		return battle;
	}

	public void setOptions() {
		if (battle != null) {
			battle.setOptions();
		}
	}

	public BattleProperties getBattleProperties() {
		if (battleProperties == null) {
			battleProperties = new BattleProperties();
		}
		return battleProperties;
	}

	public synchronized void resumeBattle() {
		int oldPauseCount = pauseCount;

		pauseCount = Math.max(--pauseCount, 0);

		if (oldPauseCount == 1) {
			notifyBattleResumed();
		}
	}

	public boolean isBattleRunning() {
		return battleRunning;
	}

	public void setBattle(Battle newBattle) {
		battle = newBattle;
	}

	public void setBattleRunning(boolean newBattleRunning) {
		battleRunning = newBattleRunning;
	}

	public void setResultsFile(String newResultsFile) {
		resultsFile = newResultsFile;
	}

	public String getResultsFile() {
		return resultsFile;
	}

	public void sendResultsToListener(Battle battle, RobocodeListener listener) {
		List<ContestantPeer> orderedPeers = Collections.synchronizedList(
				new ArrayList<ContestantPeer>(battle.getContestants()));

		Collections.sort(orderedPeers);

		RobotResults results[] = new RobotResults[orderedPeers.size()];

		for (int i = 0; i < results.length; i++) {
			ContestantPeer peer = orderedPeers.get(i);
			RobotPeer robotPeer = (peer instanceof RobotPeer) ? (RobotPeer) peer : ((TeamPeer) peer).getTeamLeader();
			
			ContestantStatistics stats = peer.getStatistics();

			results[i] = new RobotResults(robotPeer.getRobotClassManager().getControlRobotSpecification(), (i + 1),
					stats.getTotalScore(), stats.getTotalSurvivalScore(), stats.getTotalLastSurvivorBonus(),
					stats.getTotalBulletDamageScore(), stats.getTotalBulletKillBonus(), stats.getTotalRammingDamageScore(),
					stats.getTotalRammingKillBonus(), stats.getTotalFirsts(), stats.getTotalSeconds(), stats.getTotalThirds());
		}
		listener.battleComplete(battle.getBattleSpecification(), results);
	}

	public void printResultsData(Battle battle) {
		PrintStream out=null;
		boolean close = false;

		if (getResultsFile() == null) {
			out = System.out;
		} /*else {
			File f = new File(getResultsFile());

			FileOutputStream fos = null;

			try {
				fos = new FileOutputStream(f);
				out = new PrintStream(fos);
				close = true;
			} catch (IOException e) {
				log(e);
				return;
			} finally {
				if (fos != null) {
					try {
						fos.close();
					} catch (IOException e) {}
				}
			}
		}*/

		BattleResultsTableModel resultsTable = new BattleResultsTableModel(battle);

		resultsTable.print(out);
		if (close) {
			out.close();
		}
	}

	/**
	 * Gets the manager.
	 *
	 * @return Returns a RobocodeManager
	 */
	public RobocodeManager getManager() {
		return manager;
	}

	public void addListener(PauseResumeListener listener) {
		if (pauseResumeListeners == null) {
			pauseResumeListeners = new ArrayList<PauseResumeListener>();
		}
		pauseResumeListeners.add(listener);
	}

	public void removeListener(PauseResumeListener listener) {
		pauseResumeListeners.remove(listener);
	}

	private void notifyBattlePaused() {
		for (PauseResumeListener l : pauseResumeListeners) {
			l.battlePaused();
		}
	}

	private void notifyBattleResumed() {
		for (PauseResumeListener l : pauseResumeListeners) {
			l.battleResumed();
		}
	}
}
