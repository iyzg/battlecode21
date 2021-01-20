package sbot16;

import battlecode.common.*;

public class BotEC extends Bot {
    private static int robotsSpawned = 0;

    private static int[] scouts = new int[80];
    private static int[] attackers = new int[80];
private static MapLocation[] ECs = new MapLocation[12]; private static int[] buckets = new int[12];
    private static boolean[] converted = new boolean[12];
    private static int tail = 0, cur = 0;

    private static boolean spawnedTroop = false;
    private static int influence;

    private static enum ECBehavior {
        GREEDY,
        SAFE,
        CLEANUP,
        MR_DEFENSE,
        BUFFER,
    }

    private static ECBehavior behavior = ECBehavior.GREEDY;
    private static ECBehavior savedBehavior;

    private static void updateFlag() throws GameActionException {
        int toSet = 0;
        MapLocation loc = null;

        for (int id : scouts) {
            if (id != 0 && rc.canGetFlag(id)) {
                int allyFlag = rc.getFlag(id);
                if (allyFlag == 0) continue;
                int command = Comm.getCommandFromFlag(allyFlag);
                MapLocation ecLoc = Comm.getLocationFromFlag(allyFlag); int bucket = Comm.getExtraInformationFromFlag(allyFlag);

                boolean friendly = (command == 1);

                boolean alreadyFound = false;
                for (int i = 0; i < tail; ++i) {
                    if (ECs[i].equals(ecLoc)) {
                        buckets[i] = bucket;
                        converted[i] = friendly;
                        alreadyFound = true;
                        break;
                    }
                }

                if (!alreadyFound) {
                    ECs[tail] = ecLoc;
                    buckets[tail] = bucket;
                    converted[tail] = friendly;
                    ++tail;
                }
            } else id = 0;
        }

        for (int id : attackers) {
            if (id != 0 && rc.canGetFlag(id)) {
                int allyFlag = rc.getFlag(id);
				int allyCommand = Comm.getCommandFromFlag(allyFlag);
                if (allyFlag == 0 || allyCommand != 1) continue;
                MapLocation ecLoc = Comm.getLocationFromFlag(allyFlag); int bucket = Comm.getExtraInformationFromFlag(allyFlag);

                for (int i = 0; i < tail; ++i) {
                    if (ECs[i].equals(ecLoc)) {
                        buckets[i] = bucket;
                        converted[i] = true;
                        break;
                    }
                }
            } else id = 0;
        }

        // TODO: Check if any big muckrakers to deal with

        // Put target to next not converted EC
        if (tail != 0) {
            int start = cur;
            cur = (cur + 1) % tail;
            while (converted[cur] && cur != start) {
                cur = (cur + 1) % tail;
            }
            if (!converted[cur]) {
                Comm.sendLocation(ECs[cur], buckets[cur], 1);
            } else {
				if (!ECs[cur].equals(here)) Comm.sendLocation(ECs[cur], buckets[cur], 2);
				cur = (cur + 1) % tail;
			}
        }

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
                spawnedTroop = true;
                switch (type) {
                    case MUCKRAKER:
                        for (int i = 0; i < 80; ++i) {
                            if (scouts[i] == 0 || !rc.canGetFlag(scouts[i])) {
                                scouts[i] = rc.senseRobotAtLocation(here.add(dir)).getID();
                                break;
                            }
                        }
                        break;
                    case POLITICIAN:
						if (infToSpawn > 30) {
							for (int i = 0; i < 80; ++i) {
								if (attackers[i] == 0 || !rc.canGetFlag(attackers[i])) {
									attackers[i] = rc.senseRobotAtLocation(here.add(dir)).getID();
									break;
								}
							}
						} 
						/*
							for (int i = 0; i < 80; ++i) {
								if (poli_scouts[i] == 0 || !rc.canGetFlag(poli_scouts[i])) {
									poli_scouts[i] = rc.senseRobotAtLocation(here.add(dir)).getID();
									break;
								}
							}
						*/
                        break;
                    default:
                        break;
                }
                return;
            }
        }
        spawnedTroop = false;
    }

    private static void spawnDefense() throws GameActionException {
        if (influence >= 25) {
            trySpawn(RobotType.POLITICIAN, 15, 20, 10);
        } else {
            trySpawn(RobotType.MUCKRAKER, 1, 1, 0);
        }
    }


    private static void spawnBuffer() throws GameActionException {
        trySpawn(RobotType.MUCKRAKER, 1, 1, 0);
    }

    private static void spawnCleanup() throws GameActionException {
        if (robotsSpawned % 6 == 0 || robotsSpawned % 6 == 3) {
            //trySpawn(RobotType.SLANDERER, Math.min(rc.getInfluence() - 20, 949), 949, 20);
            trySpawn(RobotType.SLANDERER, 949, 949, 50);
        } else if (robotsSpawned % 6 == 2) {
            trySpawn(RobotType.MUCKRAKER, 50, 500, 100);
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

    private static void spawnSafe() throws GameActionException {
        // Spawn pattern S P M S P
        if (robotsSpawned % 5 == 0 || robotsSpawned % 5 == 3) {
            trySpawn(RobotType.SLANDERER, 949, 949, 20);
        } else if (robotsSpawned % 5 == 2) {
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
        // Spawn pattern S M P M
        if (robotsSpawned % 4 == 0) {
            trySpawn(RobotType.SLANDERER, 949, 949, 20);
        } else if (robotsSpawned % 2 < 3) {
			if (robotsSpawned % 12 == 9 && influence >= 100) {
				trySpawn(RobotType.MUCKRAKER, 10, 35, 50);
			} else trySpawn(RobotType.MUCKRAKER, 1, 1, 0);
        } else {
            if (flag != 0) {
                int needed = Comm.getExtraInformationFromFlag(flag) * 50 + 10;
                if (needed <= influence - 20) {
                    trySpawn(RobotType.POLITICIAN, needed, needed + 300, 20);
                    return;
                }
            }

            trySpawn(RobotType.POLITICIAN, 15, 25, 20);
        }
    }

    private static boolean findMuckrakers() {
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, them);
        for (RobotInfo robot : nearbyEnemies) {
            if (robot.type.equals(RobotType.MUCKRAKER)) return true;
        }
        return false;
    }
    
    public static void turn() throws GameActionException {
        influence = rc.getInfluence();

        updateFlag();
		// TODO: set target if within range
        
        if (robotsSpawned == 0) {
			if (influence >= 41 && rc.getRoundNum() <= 300) {
				behavior = ECBehavior.GREEDY;
			} else {
				behavior = ECBehavior.BUFFER;
			}
        } else if (behavior == ECBehavior.BUFFER && robotsSpawned == 15) {
            behavior = ECBehavior.GREEDY;
        } else if (rc.getRoundNum() >= 900) {
            behavior = ECBehavior.CLEANUP;
        }

        boolean muckrakerFound = findMuckrakers();
        if (muckrakerFound && behavior != ECBehavior.MR_DEFENSE && behavior != ECBehavior.BUFFER) {
            savedBehavior = behavior;
            behavior = ECBehavior.MR_DEFENSE;
        } else if (!muckrakerFound && behavior == ECBehavior.MR_DEFENSE) {
            behavior = savedBehavior;
            savedBehavior = null;
        }

        if (rc.isReady()) {
            switch (behavior) {
                case MR_DEFENSE:
                    spawnDefense();
                    break;
                case BUFFER:
                    spawnBuffer();
                    break;
                case GREEDY:
                    spawnGreedy();
                    break;
                case SAFE:
                    spawnSafe();
                    break;
                case CLEANUP:
                    spawnCleanup();
                    break;
            }

            if (spawnedTroop) ++robotsSpawned;
        }
    }
}
