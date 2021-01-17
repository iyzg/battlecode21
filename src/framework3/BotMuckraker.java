package framework3;

import battlecode.common.*;

public class BotMuckraker extends Bot {
    private static int actionRadius = rc.getType().actionRadiusSquared;
    private static int sensorRadius = rc.getType().sensorRadiusSquared;
    private static NavPolicy navPolicy;

	/**
	 * Commands
	 * 2 - Found EC
	 * 1 - Found Enemy
	 */
    public static void setFlag(RobotInfo[] nearby) throws GameActionException {
        MapLocation loc = null;
        int bucket = -1;
		boolean foundEnemy = false;
        for (RobotInfo robot : nearby) {
            if (robot.getType().equals(RobotType.ENLIGHTENMENT_CENTER) && !robot.getTeam().equals(us)) {
				if (robot.getTeam().equals(them)) foundEnemy = true;
                loc = robot.location;
                bucket = (int)Math.min(Math.ceil(robot.getInfluence() / 50.0), 31);
            }
        }

        if (loc != null) {
			Comm.sendLocation(loc, bucket, 2);
		} else if (foundEnemy) {
			Comm.sendCommand(1);
	    } else rc.setFlag(0);
    }

    private static boolean destroySlanderer(RobotInfo[] enemies) throws GameActionException {
		for (RobotInfo robot : enemies) {
			if (robot.type.canBeExposed()) {
				if (rc.canExpose(robot.location)) {
					rc.expose(robot.location);
					return true;
				}
			}
		}
		return false;
	}
	
	private static boolean chaseSlanderer(RobotInfo[] enemies) throws GameActionException {
		int targetDistance = 1000;
		MapLocation target = null;

		for (RobotInfo robot : enemies) {
			if (robot.type.canBeExposed()) {
				int distanceFrom = here.distanceSquaredTo(robot.location);
				if (distanceFrom < targetDistance) {
					target = robot.location;
					targetDistance = distanceFrom;
				}
			}
		}

		// Chase slanderer if any found.
		if (target != null) {
			Nav.goTo(target, navPolicy);
			return true;
		}
		
		return false;
	}

    public static Direction chooseDirection(RobotInfo[] allies) throws GameActionException {
		int density[] = new int[8];
		double spreadDensity[] = new double[8];

		for (RobotInfo ally : allies) {
			if (!ally.type.equals(RobotType.MUCKRAKER)) continue;
			Direction dirTo = here.directionTo(ally.location);
			int distTo = here.distanceSquaredTo(ally.location);
			int allyFlag = rc.getFlag(ally.ID);
			density[Nav.numRightRotations(Direction.NORTH, dirTo)] += (31 - distTo);
        }
        
        // Never try to walk directly at border
		for (Direction dir : directions) {
			if (!rc.onTheMap(here.add(dir))) {
				density[Nav.numRightRotations(Direction.NORTH, dir)] += 10000;
			}
		}

		for (int i = 0; i < 8; ++i) {
			for (int j = 0; j < 8; ++j) {
				int naiveDiff = Math.abs(j - i);
				int diff = Math.min(naiveDiff, 8 - naiveDiff);
				switch (diff) {
					case 0:
						spreadDensity[j] += (1.0d * density[i]);
						break;
					case 1:
						spreadDensity[j] += (0.5d * density[i]);
						break;
					case 2:
						spreadDensity[j] += (0.25d * density[i]);
						break;
					case 3:
						spreadDensity[j] += (0.125d * density[i]);
						break;
					case 4:						
						spreadDensity[j] += (0.0625d * density[i]);
						break;
				}
			}
		}

		double minDensity = spreadDensity[0];
		Direction chosenDir = Direction.NORTH;

		for (Direction dir : directions) {
			double dirDensity = spreadDensity[Nav.numRightRotations(Direction.NORTH, dir)];
            
			if (dirDensity < minDensity) {
				minDensity = dirDensity;
				chosenDir = dir;
			}
		}
		return chosenDir;
    }
    
    public static void turn() throws GameActionException {
        RobotInfo[] nearby = rc.senseNearbyRobots(sensorRadius);
        RobotInfo[] nearbyNeutral = rc.senseNearbyRobots(sensorRadius, neutral);
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(sensorRadius, us);
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(sensorRadius, them);

        here = rc.getLocation();
        
        navPolicy = new PolicyMuckrakerSpread(nearbyAllies, nearbyEnemies);
        setFlag(nearby);

        if (destroySlanderer(nearbyEnemies)) return;
        if (chaseSlanderer(nearbyEnemies)) return;

        Nav.moveDirection(chooseDirection(nearbyAllies), navPolicy);
		++turnsAlive;
    }
}