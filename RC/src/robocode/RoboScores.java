package robocode;

public class RoboScores implements Comparable<RoboScores>{
	String robot;
	long score;
	long rank;
	long level;
	boolean recorded;
	
	public boolean isRecorded() {
		return recorded;
	}
	public void setRecorded(boolean recorded) {
		this.recorded = recorded;
	}
	public long getRank() {
		return rank;
	}
	public void setRank(long rank) {
		this.rank = rank;
	}
	public String getRobot() {
		return robot;
	}
	public void setRobot(String robot) {
		this.robot = robot;
	}
	public long getScore() {
		return score;
	}
	public void setScore(long score) {
		this.score = score;
	}
	public long getLevel() {
		return level;
	}
	public void setLevel(long level) {
		this.level = level;
	}
	
	@Override
	public int compareTo(RoboScores o) {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		if (this.getScore() > o.getScore()) {
			return 1;
		} else if (this.getScore() == o.getScore()) {
			return 0;
		} else {
			return -1;
		}
	}

}