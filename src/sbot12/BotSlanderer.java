package sbot12;

import battlecode.common.*;

public class BotSlanderer extends Bot {
    private static int sensorRadius = rc.getType().sensorRadiusSquared;
    private static int detectRadius = rc.getType().detectionRadiusSquared;

    private static Direction defaultDir;
    private static NavPolicy navPolicy;

    private static boolean validSpot(MapLocation loc) throws GameActionException {
        return (!loc.isAdjacentTo(homeLoc) && (loc.x + loc.y) % 2 == 0);
    }

    // TODO: Make this Dial's algo and find closest w/ passability
    private static MapLocation findSpace() throws GameActionException {
        for (int dist = 1; dist * dist <= sensorRadius; ++dist) {
            for (int a = 0; a <= dist; ++a) {
                int b = dist - a;
                MapLocation loc = here.translate(a, b);
                if (rc.canSenseLocation(loc) && validSpot(loc) && rc.senseRobotAtLocation(loc) == null) return loc;

                loc = here.translate(-a, b);
                if (rc.canSenseLocation(loc) && validSpot(loc) && rc.senseRobotAtLocation(loc) == null) return loc;

                loc = here.translate(-a, -b);
                if (rc.canSenseLocation(loc) && validSpot(loc) && rc.senseRobotAtLocation(loc) == null) return loc;

                loc = here.translate(a, -b);
                if (rc.canSenseLocation(loc) && validSpot(loc) && rc.senseRobotAtLocation(loc) == null) return loc;
            }
        }

        return null;
    }

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

        Comm.sendCommand(1);

        ++turnsAlive;
        
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(sensorRadius, them);
        navPolicy = new PolicyAvoidEnemies(nearbyEnemies);

        for (RobotInfo robot : nearbyEnemies) {
            if (robot.getType().equals(RobotType.MUCKRAKER)) {
                Direction dir = here.directionTo(robot.location);
                Nav.moveDirection(dir.opposite(), navPolicy);
                return;
            }
        }

        if (validSpot(here)) return;

        MapLocation target = findSpace();

        if (target != null) {
            Nav.goTo(target, navPolicy);
        } else {
            Nav.moveDirection(defaultDir, navPolicy);
        }

    }
}
