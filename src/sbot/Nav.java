package sbot;

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

	// TODO: This bugs for no reason. pain. When added, it'll randomly bug and I can't figure out why. makes false && true == true??
	public boolean isSafeToMoveTo(MapLocation loc) {
		for (RobotInfo enemy : nearbyEnemies) {
			switch (enemy.type) {
				case POLITICIAN:
					if (loc.distanceSquaredTo(enemy.location) <= 9) return false;
					break;

				case MUCKRAKER:
					if (loc.distanceSquaredTo(enemy.location) <= 12) return false;
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

		// TODO: Revert back to canMove(toDest)
		boolean a = rc.canMove(toDest), b = policy.isSafeToMoveTo(here.add(toDest));
		if (canMove(toDest)) {
			if (!a) {
				System.out.println(a + " " + b);
				System.out.println(rc.canMove(toDest) + " " + policy.isSafeToMoveTo(here.add(toDest)));
				System.out.println("resigning here " + here + canMove(toDest));
				rc.resign();
			}
			//System.out.println("Moving: " + toDest);
			move(toDest);
			return true;
		}

		Direction[] dirs = new Direction[2];
		dirs[0] = toDest.rotateLeft();
		dirs[1] = toDest.rotateRight();
		for (Direction idirs : dirs) {
			//System.out.println("Trying: " + idirs);

			if (canMove(idirs)) {
				move(idirs);
				return true;
			}
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
