package framework1;

import battlecode.common.*;

public class BotPolitician extends Bot {
    public static void turn() throws GameActionException {
        here = rc.getLocation();

        int actionRadius = rc.getType().actionRadiusSquared;
        double buff = rc.getEmpowerFactor(us, 0);

        int bestTakenOut = 0, bestR = -1;

        // TODO: Take neutral ECs
        int conviction = (int)(rc.getConviction() * buff);

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
    }
}