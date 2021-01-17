package framework3;

import battlecode.common.*;

public class BotSlanderer extends Bot {
    public static void turn() throws GameActionException {
        // TODO: Run away from muckrakers
        if (turnsAlive == 0) Bot.setHome();
        here = rc.getLocation();

        if (Bot.nearHome()) {
            if (Bot.tryMove(Bot.randomDirection())) return;
        }

        if ((here.x + here.y) % 2 == 0) return;
        if (Bot.tryMove(Bot.randomDirection())) return;
        ++turnsAlive;
    }
}