package framework5;

import battlecode.common.*;

/**
 * Generic Bot class with universal items for all robot types.
 */
public class Bot {
    public static RobotController rc;
    protected static Team us;
    protected static Team them;
    protected static Team neutral;
    public static int flag;
    public static int id;
    public static int turnsAlive;
    public static int home;
    public static MapLocation homeLoc;

    public static MapLocation here;

    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    protected static void init(RobotController theRC) throws GameActionException {
        rc = theRC;
        id = rc.getID();
        turnsAlive = 0;

        us = rc.getTeam();
        them = us.opponent();
        neutral = Team.NEUTRAL;

        // Note that each bot has to continually update here each turn.
        here = rc.getLocation();
        flag = 0;
    }

    public static void setFlag(int flagAttempt) throws GameActionException {
        if (rc.canSetFlag(flagAttempt)) {
            rc.setFlag(flagAttempt);
        }
    }

    public static void setHome() throws GameActionException {
        for (Direction dir : directions) {
            MapLocation loc = here.add(dir);
            if (!rc.onTheMap(loc)) continue;
            RobotInfo robot = rc.senseRobotAtLocation(loc);
            if (robot != null && robot.getType().equals(RobotType.ENLIGHTENMENT_CENTER)) {
                homeLoc = loc;
                home = robot.ID;
                break;
            }
        }
    }

    public static boolean nearHome(MapLocation loc) throws GameActionException {
        for (Direction dir : directions) {
            MapLocation testLoc = loc.add(dir);
            if (!rc.onTheMap(testLoc)) continue;
            RobotInfo robot = rc.senseRobotAtLocation(testLoc);
            if (robot != null && robot.ID == home) {
                return true;
            }
        }
        return false;
    }

    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    static boolean tryMove(Direction dir) throws GameActionException {
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }
}
