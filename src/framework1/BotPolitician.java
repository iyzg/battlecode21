package framework1;

import battlecode.common.*;

public class BotPolitician extends Bot {
    public static void turn() throws GameActionException {
        here = rc.getLocation();

        int actionRadius = rc.getType().actionRadiusSquared;
        double buff = rc.getEmpowerFactor(us, 0);

        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(actionRadius, them);
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(actionRadius, us);

        int conviction = (int)(rc.getConviction() * buff);
        if (nearbyEnemies.length != 0 && rc.canEmpower(actionRadius)) {
            rc.empower(actionRadius);
            return;
        }
        if (Bot.tryMove(Bot.randomDirection())) return;
    }
}