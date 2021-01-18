package framework4;

import battlecode.common.*;

public class BotSlanderer extends Bot {
    private static int sensorRadius = rc.getType().sensorRadiusSquared;

    public static void turn() throws GameActionException {
        // TODO: Run away from muckrakers
        if (turnsAlive == 0) Bot.setHome();
        here = rc.getLocation();

        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(sensorRadius, them);

        for (RobotInfo robot : nearbyEnemies) {
            if (robot.getType().equals(RobotType.MUCKRAKER)) {
                Direction dir = here.directionTo(robot.location);
                NavPolicy navPolicy = new PolicyAvoidEnemies(nearbyEnemies);
                Nav.moveDirection(dir.opposite(), navPolicy);
            }
        }

        if (Bot.nearHome()) {
            if (Bot.tryMove(Bot.randomDirection())) return;
        }

        if ((here.x + here.y) % 2 == 0) return;
        if (Bot.tryMove(Bot.randomDirection())) return;
        ++turnsAlive;
    }
}
