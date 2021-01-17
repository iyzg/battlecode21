package framework3;

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

    public static MapLocation here;

    static final RobotType[] spawnableRobot = {
        RobotType.POLITICIAN,
        RobotType.SLANDERER,
        RobotType.MUCKRAKER,
    };
    
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

    public static void setHome() throws GameActionException {
        for (Direction dir : directions) {
            MapLocation loc = here.add(dir);
            if (!rc.onTheMap(loc)) continue;
            RobotInfo robot = rc.senseRobotAtLocation(loc);
            if (robot != null && robot.getType().equals(RobotType.ENLIGHTENMENT_CENTER)) {
                home = robot.ID;
                break;
            }
        }
    }

    public static boolean nearHome() throws GameActionException {
        for (Direction dir : directions) {
            MapLocation loc = here.add(dir);
            if (!rc.onTheMap(loc)) continue;
            RobotInfo robot = rc.senseRobotAtLocation(loc);
            if (robot != null && robot.getType().equals(RobotType.ENLIGHTENMENT_CENTER) && robot.ID == home) {
                return true;
            }
        }
        return false;
    }

    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    static RobotType randomSpawnableRobotType() {
        return spawnableRobot[(int) (Math.random() * spawnableRobot.length)];
    }

    static boolean tryMove(Direction dir) throws GameActionException {
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }
}
