package framework1;

import battlecode.common.*;

public class BotEC extends Bot {
    private static int robotsSpawned = 0;

    static final RobotType[] spawnableRobot = {
        RobotType.SLANDERER,
        RobotType.MUCKRAKER,
        RobotType.POLITICIAN,
    };

    private static void trySpawn(RobotType type, int influence) throws GameActionException {
        for (Direction dir : directions) {
            if (rc.canBuildRobot(type, dir, influence)) {
                rc.buildRobot(type, dir, influence);
                break;
            }
        }
    }

    public static void turn() throws GameActionException {
        RobotType toBuild = Bot.randomSpawnableRobotType();
        int influence = rc.getInfluence();
        here = rc.getLocation();

        if (rc.isReady()) {
            // Spawn pattern S M M P
            if (robotsSpawned % 4 == 0) {
                // Range from 100 - 300, capped at inf - 20
                int randomInfluence = (int)(Math.random() * (301 - 100)) + 100;
                trySpawn(RobotType.SLANDERER, Math.min(randomInfluence, influence - 20));
            } else if (robotsSpawned % 4 < 3) {
                trySpawn(RobotType.MUCKRAKER, 1);
            } else {
                // Range from 20 - 30
                int randomInfluence = (int)(Math.random() * (31 - 20)) + 20;
                trySpawn(RobotType.POLITICIAN, Math.min(randomInfluence, influence - 10));
            }
            ++robotsSpawned;
        }

    }
}
