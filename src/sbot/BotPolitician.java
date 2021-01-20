package sbot;

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
    private static NavPolicy navPolicy;
    private static Direction defaultDir;


    private static void init() {
        here = rc.getLocation();
        buff = rc.getEmpowerFactor(us, 0);
        conviction = (int)(rc.getConviction() * buff);

        if (rc.getConviction() <= 30) {
            behavior = POLITICIAN_BEHAVIOR.DEFENSE;
        } else {
            behavior = POLITICIAN_BEHAVIOR.OFFENSE;
        }
    }

    private static void setTarget(RobotInfo[] nearbyAllies, RobotInfo[] nearby) throws GameActionException {
        if (rc.canGetFlag(home) && rc.getFlag(home) != 0) {
            int homeFlag = rc.getFlag(home);
            int ECInf = Comm.getExtraInformationFromFlag(homeFlag) * 50;
            if (ECInf <= conviction && target == null) target = Comm.getLocationFromFlag(homeFlag);
        }
        // TODO: If you see some neutral EC you can take, take it.
 
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
            navPolicy = new PolicyRuthless();
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
            if (rc.getFlag(robot.ID) % 13 == 0) {
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

        if (bestTakenOut >= 2 || (bestTakenOut == 1 && slandererFound)) {
            if (rc.canEmpower(bestR)) {
                rc.empower(bestR);
                return true;
            }
        }
        return false;
    }

    private static void modeDefense() throws GameActionException {
        // How to lattice?
        /**
         * Move to the outside of slanderers & defense politicians
         * Stay there unless any slanderers too close
         */
        if (tryEmpower()) return;
        // Detonate if you can get 2 or if you see slanderer and muckraker

        if (Bot.tryMove(Bot.randomDirection())) return;
        Bot.setFlag(17 * ((int)(Math.random() * (10000 - 200)) + 200));
    }

    private static void modeOffense() throws GameActionException {
        RobotInfo[] nearby = rc.senseNearbyRobots(sensorRadius);
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(sensorRadius, us);

        setTarget(nearbyAllies, nearby);
        if (target != null) {
            if (tryEmpowerTarget()) return;
        }
        if (moveTowardsTarget()) return;
        // TODO: Explode if you get huge buff and can nuke
    }

    public static void turn() throws GameActionException {
        init();

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
            rc.empower(actionRadius);
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
