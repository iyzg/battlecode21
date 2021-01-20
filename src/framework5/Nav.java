package framework5;

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
		boolean[][] dangerous = new boolean[11][11];

		for (RobotInfo enemy : nearbyEnemies) {
			if (!enemy.type.equals(RobotType.POLITICIAN)) continue;
			if (loc.distanceSquaredTo(enemy.location) > 9) continue;

			for (int dist = 1; dist <= 3; ++dist) {
				for (int a = 0; a <= dist / 2; ++a) {
					int b = dist - a;
					MapLocation to = enemy.location.translate(a, b);
					if (to.x >= 11 || to.x < 0 || to.y >= 11 || to.y < 0) continue;
					dangerous[to.x - here.x + 5][to.y - here.y + 5] = true;

					loc = here.translate(-a, b);
					dangerous[to.x - here.x + 5][to.y - here.y + 5] = true;

					loc = here.translate(-a, -b);
					dangerous[to.x - here.x + 5][to.y - here.y + 5] = true;

					loc = here.translate(a, -b);
					dangerous[to.x - here.x + 5][to.y - here.y + 5] = true;
				}
			}
		}

		for (RobotInfo ally : nearbyAllies) {
			if (!ally.type.equals(RobotType.MUCKRAKER)) continue;
			if (dangerous[ally.location.x - here.x + 5][ally.location.y - here.y + 5]) return false;
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

	// set max to 10 (1 / 0.1)
	// TODO: Find direction to move which you can feed into move direct
	private static boolean tryDialMove() throws GameActionException {
		//System.out.println(Clock.getBytecodesLeft());
		int[][] D = new int[11][11];
		Direction[][] P = new Direction[11][11];
		boolean[][] V = new boolean[11][11];
		Queue<MapLocation>[] Q = new LinkedList[11];

		for (int i = 0; i < 11; i++)
			Q[i] = new LinkedList<MapLocation>();
		
		Q[0].add(here);
		for (int i = 0; i < 11; ++i) {
			for (int j = 0; j < 11; ++j) {
				// TODO: 10000
				D[i][j] = 100;
			}
		}

		D[5][5] = 0;

		int emptyBuckets = 0;
		boolean chained = false;
		for (int i = 0; true; i = (i + 1) % 11) {
			if (emptyBuckets >= 11) break;
			if (Q[i].isEmpty()) {
				if (chained) {
					++emptyBuckets;
				} else {
					chained = true;
					emptyBuckets = 1;
				}
				continue;
			} else {
				chained = false;
				emptyBuckets = 0;
			}

			while (!Q[i].isEmpty()) {
				MapLocation loc = Q[i].poll();
				int locx = loc.x - here.x + 5, locy = loc.y - here.y + 5;
				if (V[locx][locy]) continue;

				for (Direction dir : directions) {
					MapLocation to = loc.add(dir);
					if (!rc.canSenseLocation(to)) continue;
					int tox = to.x - here.x + 5, toy = to.y - here.y + 5;
					int T = D[locx][locy] + (int)(1 / rc.sensePassability(loc));
					if (T < D[tox][toy]) {
						D[tox][toy] = T;
						Q[D[tox][toy] % 11].add(to);
						P[tox][toy] = dir;
					}
				}

				V[locx][locy] = true;
			}
		}

		Direction toDest = here.directionTo(dest);
		MapLocation tempTarget = here;
		if (rc.canSenseLocation(dest)) tempTarget = dest;
		else {
			while (rc.canSenseLocation(tempTarget.add(toDest))) {
				tempTarget = tempTarget.add(toDest);
			}
		}

		Direction to = toDest;
		while (!tempTarget.equals(here)) {
			to = P[tempTarget.x - here.x + 5][tempTarget.y - here.y + 5].opposite();
			tempTarget = tempTarget.add(to);
		}
		to = to.opposite();

		if (canMove(to)) {
			move(to);
			return true;
		}

		Direction[] dirs = new Direction[2];
		dirs[0] = to.rotateLeft();
		dirs[1] = to.rotateRight();
		for (Direction idirs : dirs) {
			if (canMove(idirs)) {
				move(idirs);
				return true;
			}
		}
		return false;
	}


	/**
	 * Try to directly take the straight line to the destination.
	 * @return - True if you can take the direct line
	 * @throws GameActionException
	 */
	private static boolean tryMoveDirect() throws GameActionException {
		Direction toDest = here.directionTo(dest);

		if (canMove(toDest)) {
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

		if (rc.getType().equals(RobotType.SLANDERER)) tryMoveDirect();
		else tryDialMove();
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
