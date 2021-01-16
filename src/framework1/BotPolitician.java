package framework1;

import battlecode.common.*;

public class BotPolitician extends Bot {
    public static void turn() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
            int actionRadius = rc.getType().actionRadiusSquared;
            RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
            if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
                rc.empower(actionRadius);
                return;
            }
            if (Bot.tryMove(Bot.randomDirection()))
                System.out.println("I moved!");
    }
}