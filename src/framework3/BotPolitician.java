package framework3;

import battlecode.common.*;

public class BotPolitician extends Bot {
    private static int actionRadius = rc.getType().actionRadiusSquared;
    private static int sensorRadius = rc.getType().sensorRadiusSquared;
    private static double buff;
    private static int conviction;
    private static MapLocation target;
    private static NavPolicy navPolicy;

    private static void init() {
        here = rc.getLocation();
        buff = rc.getEmpowerFactor(us, 0);
        conviction = (int)(rc.getConviction() * buff);
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

    private static boolean tryEmpowerTarget(RobotInfo[] nearby) throws GameActionException {
        int distTo = here.distanceSquaredTo(target);
        // TODO: Only detonate 2 away if you can convert the EC

        if (distTo == lastDist) {
            ++turnsStuck;
        } else {
            lastDist = distTo;
            turnsStuck = 0;
        }

        if (turnsStuck >= 8 || distTo == 1) {
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

    // TODO: Big ones shouldn't detonate willy nilly
    // TODO: Should try to trade 2 : 1
    public static boolean tryEmpower() throws GameActionException{
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


        if (bestR > -1 && rc.canEmpower(bestR)) {
            rc.empower(bestR);
            return true;
        }
        return false;
    }

    public static void turn() throws GameActionException {
        init();

        RobotInfo[] nearby = rc.senseNearbyRobots(sensorRadius);
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(sensorRadius, us);
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(sensorRadius, them);

        if (turnsAlive == 0) {
            Bot.setHome();
        }
        ++turnsAlive;

        setTarget(nearbyAllies, nearby);
        if (target != null) {
            if (tryEmpowerTarget(nearby)) return;
        }

        if (moveTowardsTarget()) return;
        if (tryEmpower()) return;

        if (Bot.tryMove(Bot.randomDirection())) return;
    }
}