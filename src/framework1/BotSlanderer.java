package framework1;

import battlecode.common.*;

public class BotSlanderer extends Bot {
    public static void turn() throws GameActionException {
        here = rc.getLocation();
        if ((here.x + here.y) % 2 == 0) return;
        if (Bot.tryMove(Bot.randomDirection())) return;
    }
}