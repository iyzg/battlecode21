package framework6;

import java.util.Deque;
import battlecode.common.*;

public class BotSlanderer extends Bot {
    private static int sensorRadius = rc.getType().sensorRadiusSquared;
    private static int detectRadius = rc.getType().detectionRadiusSquared;

    private static Direction defaultDir;

    private static boolean validSpot(MapLocation loc) throws GameActionException {
        return (!loc.isAdjacentTo(homeLoc) && (loc.x + loc.y) % 2 == 0);
    }

    // TODO: Make this Dial's algo and find closest w/ passability
    private static MapLocation findSpace() throws GameActionException {
        MapLocation[] q = new MapLocation[300];
        boolean[][] vis = new boolean[64][64];

        q[0] = here;
        vis[here.x % 64][here.y % 64] = true;
        int head = 0, tail = 1;
        while (head != tail) {
            MapLocation loc = q[head];
            ++head;
            
            if (validSpot(loc) && rc.senseRobotAtLocation(loc) == null) return loc;

            for (Direction dir : directions) {
                MapLocation to = loc.add(dir);
                if (!rc.canSenseLocation(to) || !rc.onTheMap(to) || vis[to.x % 64][to.y % 64]) continue;
                q[tail] = to;
                vis[to.x % 64][to.y % 64] = true;
                ++tail;
            }
        }

        return null;
    }


    // TODO: Reduce bytecode :D 
    public static void turn() throws GameActionException {
        here = rc.getLocation();

        if (turnsAlive == 0) {
            Bot.setHome();
            for (Direction dir : directions) {
                if (!rc.onTheMap(here.add(dir))) continue;
                RobotInfo robot = rc.senseRobotAtLocation(here.add(dir));
                if (robot != null && robot.ID == home) {
                    defaultDir = dir.opposite();
                    break;
                }
            }
        }

        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(sensorRadius, them);

        for (RobotInfo robot : nearbyEnemies) {
            if (robot.getType().equals(RobotType.MUCKRAKER)) {
                Direction dir = here.directionTo(robot.location);
                NavPolicy navPolicy = new PolicyAvoidEnemies(nearbyEnemies);
                Nav.moveDirection(dir.opposite(), navPolicy);
            }
        }

        if (validSpot(here)) return;

        MapLocation target = findSpace();

        // TODO: Maybe new policy
    
        NavPolicy navPolicy = new PolicyAvoidEnemies(nearbyEnemies);
        if (target != null) Nav.goTo(target, navPolicy);
        else Nav.moveDirection(defaultDir, navPolicy);

        int randomFlag = 13 * (int)(Math.random() * (10000 - 200)) + 200;
        Bot.setFlag(randomFlag);

        ++turnsAlive;
    }
}
