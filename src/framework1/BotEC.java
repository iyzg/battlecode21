package framework1;

import battlecode.common.*;

public class BotEC extends Bot {
    private static int robotsSpawned = 0;

    private static void trySpawn(RobotType type, int influence) throws GameActionException {
        for (Direction dir : directions) {
            if (rc.canBuildRobot(type, dir, influence)) {
                rc.buildRobot(type, dir, influence);
                break;
            }
        }
    }

    public static void turn() throws GameActionException {
        int influence = rc.getInfluence();
        here = rc.getLocation();

        // TODO: Special spawning depending on how many muckrakers you find, or how close you are to opponent
        // TODO: Read flag from scouting muckrakers

        if (rc.isReady()) {
            // Spawn pattern S M M P
            if (robotsSpawned % 4 == 0) {
                // Range from 100 - 300, capped at inf - 20
                int randomInfluence = (int)(Math.random() * (301 - 100)) + 100;
                trySpawn(RobotType.SLANDERER, Math.min(randomInfluence, influence - 20));
            } else if (robotsSpawned % 4 < 3) {
                trySpawn(RobotType.MUCKRAKER, 1);
            } else {
                int randomInfluence;
                // Spawn larger one if possible, these will go cap neutrals
                if (influence >= 500) {
                    randomInfluence = (int)(Math.random() * (501 - 250)) + 250;
                }

                // Range from 20 - 30
                randomInfluence = (int)(Math.random() * (31 - 20)) + 20;
                trySpawn(RobotType.POLITICIAN, Math.min(randomInfluence, influence - 10));
            }
            ++robotsSpawned;
        }

    }
}
