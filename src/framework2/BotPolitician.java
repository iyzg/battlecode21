package framework2;

import battlecode.common.*;

public class BotPolitician extends Bot {
    private static int actionRadius = rc.getType().actionRadiusSquared;
    private static int sensorRadius = rc.getType().sensorRadiusSquared;
    private static double buff;
    private static int conviction;
    private static MapLocation target;
    private static NavPolicy navPolicy;

    public static void turn() throws GameActionException {
        here = rc.getLocation();

        buff = rc.getEmpowerFactor(us, 0);
        conviction = (int)(rc.getConviction() * buff);

        RobotInfo[] nearbyNeutral = rc.senseNearbyRobots(sensorRadius, neutral);
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(sensorRadius, us);
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(sensorRadius, them);

        if (turnsAlive == 0) {
            Bot.setHome();
        }

        if (rc.canGetFlag(home) && rc.getFlag(home) != 0) {
            int homeFlag = rc.getFlag(home);
            int ECInf = Comm.getExtraInformationFromFlag(homeFlag) * 50;
            if (ECInf <= conviction && target == null) target = Comm.getLocationFromFlag(homeFlag);
        }
        // TODO: Also be able to get target from reading spawning EC
        // TODO: If you got target from EC or from scouting, DON't CHANGE IT if receive new one.
        // TODO: If you see some neutral EC you can take, take it.
        // TODO: Tidy up code
        // TODO: Try to move within radius of 1, only explode radius of 2 if needed
        // TODO: If you see EC and it's converted, get rid of target
        // TODO: Clean up code and seperate into functions
        int distTo = 100;
        if (target != null) distTo = here.distanceSquaredTo(target);
        if (distTo <= 2) {
            if (rc.canEmpower(distTo)) {
                rc.empower(distTo);
                return;
            }
        }

        if (target != null) {
            for (RobotInfo robot : nearbyAllies) {
                if (robot.location.equals(target)) {
                    target = null;
                    break;
                }
            }
        }

        if (target == null) {
            for (RobotInfo robot : nearbyNeutral) {
                if (robot.getType().equals(RobotType.ENLIGHTENMENT_CENTER)) {
                    if (robot.getConviction() < conviction - 10) {
                        target = robot.getLocation();
                        break;
                    }
                }
            }
        }

        if (target != null) {
            navPolicy = new PolicyRuthless();
            Nav.goTo(target, navPolicy);
        }
        
        int bestTakenOut = 0, bestR = -1;
        for (int r = 1; r <= 9; ++r) {
            int localTakenOut = 0;
            RobotInfo[] localAllies = rc.senseNearbyRobots(r, us);
            RobotInfo[] localEnemies = rc.senseNearbyRobots(r, them);
            if (localAllies.length + localEnemies.length == 0) continue;
            int infSpread = (conviction - 10) / (localAllies.length + localEnemies.length);
            for (RobotInfo enemy : localEnemies) {
                if (enemy.conviction < infSpread) ++localTakenOut;
            }
            if (localTakenOut > bestTakenOut) {
                bestTakenOut = localTakenOut;
                bestR = r;
            }
        }

        if (bestR > -1 && rc.canEmpower(bestR)) {
            rc.empower(bestR);
            return;
        }

        if (Bot.tryMove(Bot.randomDirection())) return;
        ++turnsAlive;
    }
}