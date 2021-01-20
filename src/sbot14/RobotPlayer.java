package sbot14;

import battlecode.common.*;

public class RobotPlayer extends Bot {
    public static void run(RobotController theRC) throws GameActionException {
        Bot.init(theRC);
        while (true) {
            try {
                switch(theRC.getType()) {
                    case ENLIGHTENMENT_CENTER: BotEC.turn(); break;
                    case MUCKRAKER: BotMuckraker.turn();   break;
                    case POLITICIAN: BotPolitician.turn(); break;
                    case SLANDERER: BotSlanderer.turn();   break;
                }

                Clock.yield();
            } catch (Exception e) {
                e.printStackTrace();
				// rc.resign();
            }
        }
    }
}
