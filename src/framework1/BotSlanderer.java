package framework1;

import battlecode.common.*;

public class BotSlanderer extends Bot {
    public static void turn() throws GameActionException {
        if (Bot.tryMove(Bot.randomDirection())) return;
    }
}