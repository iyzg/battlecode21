package framework2;

import battlecode.common.*;

public class BotEC extends Bot {
    private static int robotsSpawned = 0;
    private static int[] scouts = new int[80];
    private static MapLocation[] ECs = new MapLocation[12];
    private static int[] cooldown = new int[12];
    private static int next = 0;

    // TODO: Change this depending on either how many ECs or turns in
    private static int SCOUT_CAP = 80;

    private static void trySpawn(RobotType type, int influence) throws GameActionException {
        for (Direction dir : directions) {
            if (rc.canBuildRobot(type, dir, influence)) {
                rc.buildRobot(type, dir, influence);
                if (type.equals(RobotType.MUCKRAKER)) {
                    // TODO: add to array, limit to 80 muckraker
                    for (int i = 0; i < 80; ++i) {
                        if (scouts[i] == 0 || !rc.canGetFlag(scouts[i])) {
                            scouts[i] = rc.senseRobotAtLocation(here.add(dir)).getID();
                            break;
                        }
                    }
                }
                break;
            }
        }
    }

    public static void turn() throws GameActionException {
        int influence = rc.getInfluence();

        // TODO: Special spawning depending on how many muckrakers you find, or how close you are to opponent
        // TODO: After seeing flag from scout, save it and flash it, flash it once every 100 turns it isn't capped.
        // TODO: Change spawning based on how deep into the game you are
        for (int id : scouts) {
            if (id != 0 && rc.canGetFlag(id)) {
                int allyFlag = rc.getFlag(id);
                if (allyFlag != 0) {
                    if (rc.canSetFlag(allyFlag)) {
                        rc.setFlag(allyFlag);
                        break;
                    }
                }
            } else id = 0;
        }

        flag = rc.getFlag(id);

        if (rc.isReady()) {
            if (rc.getRoundNum() >= 700) {
                if (robotsSpawned % 2 == 0) {
                    // Range from 100 - 300, capped at inf - 20
                    int randomInfluence;
                    if (influence >= 949) randomInfluence = 949;
                    else randomInfluence = (int)(Math.random() * (950 - 500)) + 500;
                    trySpawn(RobotType.SLANDERER, Math.min(randomInfluence, influence - 20));
                } else {
                    int randomInfluence;
                    // Spawn larger one if possible, these will go cap neutrals
                    if (influence >= 500) {
                        randomInfluence = (int)(Math.random() * (601 - 250)) + 250;
                    } else {
                        randomInfluence = (int)(Math.random() * (31 - 20)) + 20;
                    }
                    // Range from 20 - 30
                    trySpawn(RobotType.POLITICIAN, Math.min(randomInfluence, influence - 10));
                }
                ++robotsSpawned;
            } else {
                // Spawn pattern S M M P (w/ max 80 slanderers per base)
                // TODO: Lower amnt of slanderers/base the longer the game goes on -> to poli
                if (robotsSpawned % 4 == 0) {
                    // Range from 100 - 300, capped at inf - 20
                    int randomInfluence = (int)(Math.random() * (301 - 100)) + 100;
                    trySpawn(RobotType.SLANDERER, Math.min(randomInfluence, influence - 20));
                } else if (robotsSpawned % 4 < 3) {
                    trySpawn(RobotType.MUCKRAKER, 1);
                } else {
                    int randomInfluence = -1;
                    if (flag != 0) {
                        int needed = Comm.getExtraInformationFromFlag(flag) * 50 + 10;
                        if (needed < influence) randomInfluence = needed;
                    }

                    // Spawn larger one if possible, these will go cap neutrals
                    if (influence >= 500 && randomInfluence > -1) {
                        randomInfluence = (int)(Math.random() * (601 - 250)) + 250;
                    } else if (randomInfluence > -1) {
                        randomInfluence = (int)(Math.random() * (31 - 20)) + 20;
                    }
                    // Range from 20 - 30
                    trySpawn(RobotType.POLITICIAN, Math.min(randomInfluence, influence - 10));
                }
                ++robotsSpawned;
            }
        }

    }
}
