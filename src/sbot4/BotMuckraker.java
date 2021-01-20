package sbot4;

import battlecode.common.*;

public class BotMuckraker extends Bot {
    private static int actionRadius = rc.getType().actionRadiusSquared;
    private static int sensorRadius = rc.getType().sensorRadiusSquared;
    private static NavPolicy navPolicy;

	private static Direction defaultDir;
	private static Direction lastDir; 


	/**
	 * Commands
	 * 2 - Found EC
	 * 1 - Found friendly ec
	 */
	// TODO: If you see any muckraker with higher priority, set it to that instead (Set to EC found which is closest to home)
    public static void setFlag(RobotInfo[] nearby) throws GameActionException {
        MapLocation loc = null;
        int bucket = -1;
		MapLocation friendlyLoc = null;
        for (RobotInfo robot : nearby) {
			Team robotTeam = robot.getTeam();
			RobotType robotType = robot.getType();
			MapLocation robotLocation = robot.location;
			if (!robotType.equals(RobotType.ENLIGHTENMENT_CENTER)) continue;

            if (!robotTeam.equals(us)) {
                loc = robotLocation;
                bucket = (int)Math.min(Math.ceil(robot.getInfluence() / 50.0), 31);
            } else {
				friendlyLoc = robotLocation;
			}
        }

        if (loc != null) {
			Comm.sendLocation(loc, bucket, 2);
		} else if (friendlyLoc != null) {
			Comm.sendLocation(friendlyLoc, 0, 1);
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
			if (ally.type.equals(RobotType.ENLIGHTENMENT_CENTER)) {
				spreadDensity[Nav.numRightRotations(Direction.NORTH, dirTo)] += 10000;
				continue;
			}
			density[Nav.numRightRotations(Direction.NORTH, dirTo)] += (31 - distTo);
        }
        
        // Never try to walk directly at border
		for (int dist = 1; dist * dist <= sensorRadius; ++dist) {
            for (int a = 0; a <= dist / 2; ++a) {
                int b = dist - a;
                MapLocation loc = here.translate(a, b);
                if (!rc.onTheMap(loc)) spreadDensity[Nav.numRightRotations(Direction.NORTH, here.directionTo(loc))] += 10000;

                loc = here.translate(-a, b);
                if (!rc.onTheMap(loc)) spreadDensity[Nav.numRightRotations(Direction.NORTH, here.directionTo(loc))] += 10000;

                loc = here.translate(-a, -b);
                if (!rc.onTheMap(loc)) spreadDensity[Nav.numRightRotations(Direction.NORTH, here.directionTo(loc))] += 10000;

                loc = here.translate(a, -b);
                if (!rc.onTheMap(loc)) spreadDensity[Nav.numRightRotations(Direction.NORTH, here.directionTo(loc))] += 10000;
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
						//spreadDensity[j] += (0.125d * density[i]);
						break;
					case 4:						
						//spreadDensity[j] += (0.0625d * density[i]);
						break;
					default:
						break;
				}
			}
		}

		double minDensity = spreadDensity[Nav.numRightRotations(Direction.NORTH, lastDir)];
		Direction chosenDir = lastDir;

		for (Direction dir : directions) {
			double dirDensity = spreadDensity[Nav.numRightRotations(Direction.NORTH, dir)];
            
			if (dirDensity < minDensity) {
				minDensity = dirDensity;
				chosenDir = dir;
			}
		}
		lastDir = chosenDir;
		return chosenDir;
    }
    
    public static void turn() throws GameActionException {
		if (turnsAlive == 0) {
			Bot.setHome();
			for (Direction dir : directions) {
                if (!rc.onTheMap(here.add(dir))) continue;
                RobotInfo robot = rc.senseRobotAtLocation(here.add(dir));
                if (robot != null && robot.ID == home) {
                    defaultDir = dir.opposite();
					lastDir = defaultDir;
                    break;
                }
            }
		}

        RobotInfo[] nearby = rc.senseNearbyRobots(sensorRadius);
        RobotInfo[] nearbyNeutral = rc.senseNearbyRobots(sensorRadius, neutral);
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(sensorRadius, us);
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(sensorRadius, them);

        here = rc.getLocation();
        
        navPolicy = new PolicyMuckrakerSpread(nearbyAllies, nearbyEnemies);
        setFlag(nearby);		
		++turnsAlive;

		if (!rc.isReady()) return;

        if (destroySlanderer(nearbyEnemies)) return;
        if (chaseSlanderer(nearbyEnemies)) return;

        Nav.moveDirection(chooseDirection(nearbyAllies), navPolicy);
    }
}
