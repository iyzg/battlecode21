package framework6;

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
	// TODO: If you see any muckraker with higher priority, set it to that instead (Set to EC found which is closest to home)
    public static void setFlag(RobotInfo[] nearby) throws GameActionException {
        MapLocation loc = null;
        int bucket = -1;
		boolean foundEnemy = false;
        for (RobotInfo robot : nearby) {
			Team robotTeam = robot.getTeam();
			RobotType robotType = robot.getType();
			MapLocation robotLocation = robot.location;

			if (robot.getTeam().equals(them)) foundEnemy = true;

            if (robotType.equals(RobotType.ENLIGHTENMENT_CENTER) && !robotTeam.equals(us)) {
                loc = robotLocation;
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

		// Move away from your home EC
		for (RobotInfo ally : allies) {
			RobotType allyType = ally.type;
			if (ally.ID == home) {
				Direction dirTo = here.directionTo(ally.location);
				spreadDensity[Nav.numRightRotations(Direction.NORTH, dirTo)] += 10000;
			}
			if (!allyType.equals(RobotType.MUCKRAKER)) continue;
			Direction dirTo = here.directionTo(ally.location);
			int distTo = here.distanceSquaredTo(ally.location);
			density[Nav.numRightRotations(Direction.NORTH, dirTo)] += (31 - distTo);
        }
        

		// Stay clear of borders
        MapLocation[] q = new MapLocation[300];
        boolean[][] vis = new boolean[64][64];

        q[0] = here;
        vis[here.x % 64][here.y % 64] = true;
        int head = 0, tail = 1;
        while (head != tail) {
            MapLocation loc = q[head];
            ++head;
            
            for (Direction dir : directions) {
                MapLocation to = loc.add(dir);
				Direction dirTo = here.directionTo(to);
				int distTo = here.distanceSquaredTo(to);

				if (distTo > sensorRadius) continue;
				if (distTo == sensorRadius) {
					if (!rc.onTheMap(to)) spreadDensity[Nav.numRightRotations(Direction.NORTH, dirTo)] += 10000;
					continue;
				}

				if (!rc.onTheMap(to)) spreadDensity[Nav.numRightRotations(Direction.NORTH, dirTo)] += 10000;
                if (!rc.onTheMap(to) || vis[to.x % 64][to.y % 64]) continue;
                q[tail] = to;
                vis[to.x % 64][to.y % 64] = true;
                ++tail;
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
            System.out.println(dir + " " + dirDensity);
			if (dirDensity < minDensity) {
				minDensity = dirDensity;
				chosenDir = dir;
			}
		}
		return chosenDir;
    }
    
    public static void turn() throws GameActionException {
		if (turnsAlive == 0) Bot.setHome();

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
