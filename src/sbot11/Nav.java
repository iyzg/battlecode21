package sbot11;

import java.util.LinkedList; 
import java.util.Queue;
import battlecode.common.*;

interface NavPolicy {
    boolean isSafeToMoveTo(MapLocation loc);
}

class PolicyRuthless extends Bot implements NavPolicy {
	public boolean isSafeToMoveTo(MapLocation loc) { return true; }
}

class PolicyAvoidEnemies extends Bot implements NavPolicy {
	RobotInfo[] nearbyEnemies;

	public PolicyAvoidEnemies(RobotInfo[] nearbyEnemies) {
		this.nearbyEnemies = nearbyEnemies;
	}

	public boolean isSafeToMoveTo(MapLocation loc) {
		for (RobotInfo enemy : nearbyEnemies) {
			switch (enemy.type) {
				case POLITICIAN:
					if (loc.distanceSquaredTo(enemy.location) <= 9) return false;
					break;
				case MUCKRAKER:
					if (loc.distanceSquaredTo(enemy.location) <= 12) return false;
					break;
				default:
					break;
			}
		}

		return true;
	}
}

class PolicyMuckrakerSpread extends Bot implements NavPolicy {
	RobotInfo[] nearbyAllies;
	RobotInfo[] nearbyEnemies;

	public PolicyMuckrakerSpread(RobotInfo[] nearbyAllies, RobotInfo[] nearbyEnemies) {
		this.nearbyAllies = nearbyAllies;
		this.nearbyEnemies = nearbyEnemies;
	}

	public boolean isSafeToMoveTo(MapLocation loc) {
		for (RobotInfo enemy : nearbyEnemies) {
			if (!enemy.type.equals(RobotType.POLITICIAN)) continue;
			if (loc.distanceSquaredTo(enemy.location) > 9) continue;

			for (RobotInfo ally : nearbyAllies) {
				if (!ally.type.equals(RobotType.MUCKRAKER)) continue;

				if (ally.location.distanceSquaredTo(enemy.location) <= 9) {
					return false;
				}
			}
		}

		return true;
	}
}

public class Nav extends Bot {
	private static MapLocation dest;
    private static NavPolicy policy;

	private static boolean move(Direction dir) throws GameActionException {
		rc.move(dir);
        return true;
    }

    /**
     * Check if you can move in a direction
     * @param dir - Direction to check
     * @return - True if you can move in that direction and it's safe.
     */
    private static boolean canMove(Direction dir) {
        return rc.canMove(dir) && policy.isSafeToMoveTo(here.add(dir));
    }

    /**
     * Number of right rotations to face different directions
     */
    public static int numRightRotations(Direction start, Direction end) {
        return (end.ordinal() - start.ordinal() + 8) % 8;
    }

    /**
     * Number of left rotations to face different directions
     */
    public static int numLeftRotations(Direction start, Direction end) {
        return (-end.ordinal() + start.ordinal() + 8) % 8;
    }

	/**
	 * Try to directly take the straight line to the destination.
	 * @return - True if you can take the direct line
	 * @throws GameActionException
	 */
	private static boolean tryMoveDirect() throws GameActionException {
		Direction toDest = here.directionTo(dest);

		Direction[] dirs = new Direction[3];
		dirs[0] = toDest;
		dirs[1] = toDest.rotateLeft();
		dirs[2] = toDest.rotateRight();
		Direction chosenDir = null;
		double maxPassability = -1;
		for (Direction dir : dirs) {
			if (canMove(dir) && rc.sensePassability(here.add(dir)) > maxPassability) {
				chosenDir = dir;
				maxPassability = rc.sensePassability(here.add(dir));
			}
		}

		if (here.add(toDest).equals(dest) && canMove(toDest)) chosenDir = toDest;

		if (chosenDir != null) {
			move(chosenDir);
			return true;
		}

		return false;
	}
	
	/**
	 * Move directly to the destination.
	 * @param theDest - Destination square you want to reach
	 * @param thePolicy - Policy to decide whether you can head in a direction
	 * @throws GameActionException
	 */
	public static void goTo(MapLocation theDest, NavPolicy thePolicy) throws GameActionException {
		dest = theDest;
		policy = thePolicy;

		if (here.equals(dest)) return;
		
		tryMoveDirect();
	}

	// TODO: Bug nav around enemy units?

    /**
     * Moves a bot in the general direction
     * @param theDir - Direction to move
     * @param thePolicy - Policy to determine whether or not you should move
     * @throws GameActionException
     */
	public static void moveDirection(Direction theDir, NavPolicy thePolicy) throws GameActionException {
		dest = here.add(theDir);

		goTo(dest, thePolicy);
	}
}
