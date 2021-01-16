package framework1;

import battlecode.common.*;

public class BotMuckraker extends Bot {
    public static void turn() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
            if (robot.type.canBeExposed()) {
                if (rc.canExpose(robot.location)) {
                    rc.expose(robot.location);
                    return;
                }
            }
        }

        
        if (Bot.tryMove(Bot.randomDirection())) return;
    }
}