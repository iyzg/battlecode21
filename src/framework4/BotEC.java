package framework4;

import battlecode.common.*;

public class BotEC extends Bot {
    private static int robotsSpawned = 0;
    private static int[] scouts = new int[80];
    private static MapLocation[] ECs = new MapLocation[12];
    private static int[] cooldown = new int[12];
    private static int next = 0;
    private static boolean foundEnemy = false;

    private static int influence;

    private static enum ECBehavior {
        GREEDY,
        SAFE,
        PUSH,
        MR_DEFENSE,
        BUFFER,
    }

    private static ECBehavior behavior = ECBehavior.GREEDY;

    // TODO: Change this depending on either how many ECs or turns in
    private static int SCOUT_CAP = 80;

    // TODO: set target to the closest EC
    private static void updateFlag() throws GameActionException {
        int priority = 0;
        int toSet = 0;
        MapLocation loc = null;
        for (int id : scouts) {
            if (id != 0 && rc.canGetFlag(id)) {
                int allyFlag = rc.getFlag(id);
                if (allyFlag == 0) continue;
                int command = Comm.getCommandFromFlag(allyFlag);
                MapLocation ecLoc = Comm.getLocationFromFlag(allyFlag);
                
                if (command > 0) foundEnemy = true;
                
                if (command > priority) {
                    priority = command;
                    toSet = allyFlag;
                    loc = ecLoc;
                } else if (command == 2) {
                    if (here.distanceSquaredTo(ecLoc) < here.distanceSquaredTo(loc)) {
                        toSet = allyFlag;
                        loc = ecLoc;
                    }
                }
            } else id = 0;
        }

        if (priority < 2) rc.setFlag(0);
        else rc.setFlag(toSet);
        flag = rc.getFlag(id);
    }

    /**
     * Spawn robot type from [lower bound, upper bound]
     */
    private static void trySpawn(RobotType type, int lb, int ub, int buffer) throws GameActionException {
        int infToSpawn = Math.min((int)(Math.random() * (ub - lb + 1)) + lb, influence - buffer);

        for (Direction dir : directions) {
            if (rc.canBuildRobot(type, dir, infToSpawn)) {
                rc.buildRobot(type, dir, infToSpawn);
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

    private static void spawnBuffer() throws GameActionException {
        trySpawn(RobotType.MUCKRAKER, 1, 1, 0);
    }

    private static void spawnPush() throws GameActionException {
        if (robotsSpawned % 6 < 2) {
            trySpawn(RobotType.SLANDERER, 500, 949, 10);
        } else {
            if (influence >= 425) {
                trySpawn(RobotType.POLITICIAN, 250, 600, 20);
            } else {
                trySpawn(RobotType.POLITICIAN, 17, 25, 20);
            }
        }
    }

    private static void spawnSafe() throws GameActionException {
        // Spawn pattern S P M P M
        if (robotsSpawned % 5 == 0) {
            trySpawn(RobotType.SLANDERER, 100, 300, 20);
        } else if (robotsSpawned % 2== 1) {
            trySpawn(RobotType.MUCKRAKER, 1, 1, 0);
        } else {
            if (flag != 0) {
                int needed = Comm.getExtraInformationFromFlag(flag) * 50 + 10;
                if (needed <= influence - 20) {
                    trySpawn(RobotType.POLITICIAN, needed, needed + 300, 20);
                    return;
                }
            }

            if (influence >= 425) {
                trySpawn(RobotType.POLITICIAN, 250, 600, 20);
            } else {
                trySpawn(RobotType.POLITICIAN, 17, 25, 20);
            }
        }
    }

    private static void spawnGreedy() throws GameActionException {
        // Spawn pattern S M M P
        if (robotsSpawned % 4 == 0) {
            trySpawn(RobotType.SLANDERER, 100, 300, 20);
        } else if (robotsSpawned % 4 < 3) {
            trySpawn(RobotType.MUCKRAKER, 1, 1, 0);
        } else {
            if (flag != 0) {
                int needed = Comm.getExtraInformationFromFlag(flag) * 50 + 10;
                if (needed <= influence - 20) {
                    trySpawn(RobotType.POLITICIAN, needed, needed + 300, 20);
                    return;
                }
            }

            if (influence >= 425) {
                trySpawn(RobotType.POLITICIAN, 250, 600, 20);
            } else {
                trySpawn(RobotType.POLITICIAN, 17, 25, 20);
            }
        }
    }

    public static void turn() throws GameActionException {
        influence = rc.getInfluence();

        // TODO: Special spawning depending on how many muckrakers you find, or how close you are to opponent
        // TODO: After seeing flag from scout, save it and flash it, flash it once every 100 turns it isn't capped.
        // TODO: Change spawning based on how deep into the game you are
        // TODO: Send polis from all ECs that are close, not just this one


        updateFlag();

        if (foundEnemy && behavior == ECBehavior.GREEDY) {
            behavior = ECBehavior.SAFE;
        } else if (robotsSpawned == 0 && rc.getRoundNum() >= 30) {
            behavior = ECBehavior.BUFFER;
        } else if (behavior == ECBehavior.BUFFER && robotsSpawned == 15) {
            behavior = ECBehavior.SAFE;
        } else if (rc.getRoundNum() >= 800) {
            behavior = ECBehavior.PUSH;
        }

        if (rc.isReady()) {
            switch (behavior) {
                case BUFFER:
                    spawnBuffer();
                    break;
                case GREEDY:
                    spawnGreedy();
                    break;
                case SAFE:
                    spawnSafe();
                    break;
                case PUSH:
                    spawnPush();
                    break;
            }
            ++robotsSpawned;
        }
    }
}
