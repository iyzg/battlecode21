package framework2;

import battlecode.common.*;

public class BotSlanderer extends Bot {
    public static void turn() throws GameActionException {
        here = rc.getLocation();
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, us);

        boolean nearEC = false;
        for (RobotInfo robot : nearbyAllies) {
            if (robot.getType().equals(RobotType.ENLIGHTENMENT_CENTER)) {
                nearEC = true;
                break;
            }
        }

        if (nearEC) {
            if (Bot.tryMove(Bot.randomDirection())) return;
        }

        if ((here.x + here.y) % 2 == 0) return;
        if (Bot.tryMove(Bot.randomDirection())) return;
        ++turnsAlive;
    }
}