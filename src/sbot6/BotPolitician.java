package sbot6;

import battlecode.common.*;

public class BotPolitician extends Bot {
    // Defense, 20s that stay close
    // Offense -> Guys that go to ECs and wait until you can take it
    private static enum POLITICIAN_BEHAVIOR {
        DEFENSE,
        OFFENSE,
        SCOUT,
    }

    private static int actionRadius = rc.getType().actionRadiusSquared;
    private static int sensorRadius = rc.getType().sensorRadiusSquared;

    private static double buff;
    private static int conviction;
    private static POLITICIAN_BEHAVIOR behavior;

    private static MapLocation target;
    private static NavPolicy navPolicy = new PolicyRuthless();
    private static Direction defaultDir;


    private static void init() {
        here = rc.getLocation();
        buff = rc.getEmpowerFactor(us, 0);
        conviction = (int)(rc.getConviction() * buff);

        if (rc.getInfluence() <= 30) {
            behavior = POLITICIAN_BEHAVIOR.DEFENSE;
        } else {
            behavior = POLITICIAN_BEHAVIOR.OFFENSE;
        }
    }

    private static void checkHome() throws GameActionException {
        if (rc.canGetFlag(home) && rc.getFlag(home) != 0) {
            int homeFlag = rc.getFlag(home);
            int ECInf = Comm.getExtraInformationFromFlag(homeFlag) * 50;
            MapLocation homeTarget = Comm.getLocationFromFlag(homeFlag);
            int homeCommand = Comm.getCommandFromFlag(homeFlag);
            if (homeCommand == 1) {
                if (ECInf < rc.getConviction() - 10 && target == null) target = homeTarget;
                else if (ECInf < rc.getConviction() - 10 && here.distanceSquaredTo(homeTarget) < here.distanceSquaredTo(target)) target = homeTarget;
            } else if (homeCommand == 2 && homeTarget.equals(target)) {
                target = null;
            }
        }
    }

    // TODO: Only set target if command center is set to command 1
    private static void setTarget(RobotInfo[] nearbyAllies, RobotInfo[] nearby) throws GameActionException {
        if (target != null) {
            for (RobotInfo robot : nearbyAllies) {
                if (robot.location.equals(target)) {
                    target = null;
                    break;
                }
            }
        }

        // Look for local ECs to take
        for (RobotInfo robot : nearby) {
            if (robot.getType().equals(RobotType.ENLIGHTENMENT_CENTER) && !robot.getTeam().equals(us)) {
                if (robot.getConviction() < conviction - 10) {
                    target = robot.getLocation();
                    break;
                }
            }
        }
    }

    private static int lastDist = -1;
    private static int turnsStuck = 0;

    // Detonate if you can
    private static boolean tryEmpowerTarget() throws GameActionException {
        int distTo = here.distanceSquaredTo(target);
        // TODO: Only detonate 2 away if you can convert the EC


        if (distTo <= actionRadius) {
            RobotInfo[] nearby = rc.senseNearbyRobots(distTo);
            if (nearby.length == 0) return false;
            int targetConviction = rc.senseRobotAtLocation(target).getConviction();

            if ((int)((conviction - 10) / nearby.length) > targetConviction) {
                if (rc.canEmpower(distTo)) {
                    rc.empower(distTo);
                    return true;
                }
            }
        }

        if (distTo == lastDist) {
            ++turnsStuck;
        } else {
            lastDist = distTo;
            turnsStuck = 0;
        }

        if (turnsStuck >= 8) {
            if (rc.canEmpower(distTo)) {
                rc.empower(distTo);
                return true;
            }
        }
        return false;
    }

    public static boolean moveTowardsTarget() throws GameActionException {
        if (target != null) {
            Nav.goTo(target, navPolicy);
            return true;
        }
        return false;
    }

    // TODO: Should try to trade 2 : 1 or if you spot slanderer
    public static boolean tryEmpower() throws GameActionException{
        boolean slandererFound = false;
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(sensorRadius, us);
        for (RobotInfo robot : nearbyAllies) {
            if (Comm.getCommandFromFlag(rc.getFlag(robot.ID)) == 1) {
                slandererFound = true;
                break;
            }
        }

        int bestTakenOut = 0, bestR = -1;
        for (int r = 1; r <= 9; ++r) {
            int localTakenOut = 0;
            RobotInfo[] nearby = rc.senseNearbyRobots(r);
            if (nearby.length == 0) continue;
            int infSpread = (conviction - 10) / nearby.length;
            for (RobotInfo robot : nearby) {
                if (robot.conviction < infSpread && !robot.getTeam().equals(us)) ++localTakenOut;
            }
            if (localTakenOut > bestTakenOut) {
                bestTakenOut = localTakenOut;
                bestR = r;
            }
        }

        if (bestR == -1) return false;

        if (rc.getRoundNum() >= 1000 && bestTakenOut >= 1) {
            if (rc.canEmpower(bestR)) {
                rc.empower(bestR);
                return true;
            }
        }

        if (bestTakenOut >= 2 || (bestTakenOut == 1 && slandererFound)) {
            if (rc.canEmpower(bestR)) {
                rc.empower(bestR);
                return true;
            }
        }
        return false;
    }
    
    // TODO: Keep away from friendly ECs in general
    private static boolean validSpot(MapLocation loc) throws GameActionException {
        if ((loc.x + loc.y) % 4 != 3) return false;
        if (loc.x % 2 == 0) return false;
        for (Direction dir : directions) {
            MapLocation adj = loc.add(dir);
            if (!rc.canSenseLocation(adj)) continue;
            RobotInfo robot = rc.senseRobotAtLocation(adj);
            if (robot == null) continue;
            if (!robot.team.equals(us)) continue;
            if (robot.type.equals(RobotType.ENLIGHTENMENT_CENTER) || Comm.getCommandFromFlag(rc.getFlag(robot.ID)) == 1) return false;
        }
        return true;
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


    // Set to location if you see big muckraker
    public static void setFlag(RobotInfo[] nearbyEnemies) throws GameActionException {
        MapLocation loc = null;
        int bucket = -1;
        for (RobotInfo robot : nearbyEnemies) {
			RobotType robotType = robot.getType();
            int robotConviction = robot.getInfluence();
            if (!robotType.equals(RobotType.MUCKRAKER)) continue;
			MapLocation robotLocation = robot.location;

            if (robotType.equals(RobotType.MUCKRAKER) && robotConviction > 1) {
                loc = robotLocation;
                bucket = (int)Math.min(Math.ceil(robot.getInfluence() / 20.0), 31);
            }
        }

        if (loc != null) {
			Comm.sendLocation(loc, bucket, 2);
		} else rc.setFlag(0);
    }

    private static void modeDefense() throws GameActionException {
        // How to lattice?
        /**
         * Move to the outside of slanderers & defense politicians
         * Stay there unless any slanderers too close
         */
        if (tryEmpower()) return;
        // TODO: If you see border, move somewhere else

        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(sensorRadius, them);
        setFlag(nearbyEnemies);

        if (validSpot(here)) return;

        target = findSpace();
        //System.out.println("found target @ " + target);

        // Need to do something for ones who get converted
        if (target != null) {
            Nav.goTo(target, navPolicy);
        } else {
            Bot.tryMove(Bot.randomDirection());
        }
    }

    private static void modeOffense() throws GameActionException {
        RobotInfo[] nearby = rc.senseNearbyRobots(sensorRadius);
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(sensorRadius, us);

        if (rc.getRoundNum() == 950) {
            if (tryEmpower()) return;
        }

        setTarget(nearbyAllies, nearby);
        //System.out.println(Clock.getBytecodesLeft());
        if (target != null) {
            //System.out.println(target);
            if (tryEmpowerTarget()) return;
        }
        if (moveTowardsTarget()) return;
        if (Bot.tryMove(Bot.randomDirection())) return;
        // TODO: Explode if you get huge buff and can nuke
    }

    private static boolean weakCheck() throws GameActionException {
        if (rc.getConviction() <= 10 && rc.canEmpower(1)) {
            rc.empower(1);
            return true;
        }
        return false;
    }

    public static void turn() throws GameActionException {
        init();
        if (weakCheck()) return;

        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(sensorRadius, them);

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
        ++turnsAlive;

        if (rc.getRoundNum() == 1495 && rc.canEmpower(actionRadius)) {
            rc.empower(2);
        }
        
        if (behavior == POLITICIAN_BEHAVIOR.OFFENSE) {
            checkHome();
        }

        if (rc.isReady()) {
            switch (behavior) {
                case DEFENSE:
                    modeDefense();
                    break;
                case OFFENSE:
                    modeOffense();
                    break;
            }
        }
    }
}
