package sbot13;

import battlecode.common.*;

// TODO: Rewrite this code from ssnk to make it cleaner

public class Comm extends Bot {
    static void sendLocation() throws GameActionException{
        MapLocation location = rc.getLocation();
        int x = location.x, y = location.y;
        int encodedLocation = (x % 128) * 128 + (y%128);
        if(rc.canSetFlag(encodedLocation)){
            rc.setFlag(encodedLocation);
        }
    }

    /**
     * Sends the location of the robot
     */
    static void sendLocation(MapLocation location) throws GameActionException {
        int x = location.x, y = location.y;
        int encodedLocation = ((x & BITMASK) << NBITS) + (y & BITMASK);
        if (rc.canSetFlag(encodedLocation)) {
            rc.setFlag(encodedLocation);
        }
    }

    /**
     * Generic to send a location on the map.
     */
    static void sendLocation(MapLocation location, int extraInformation, int command) throws GameActionException {
        int x = location.x, y = location.y;
        int encodedLocation = (extraInformation << (2*NBITS)) + ((x & BITMASK) << NBITS) + (y & BITMASK) + (command << 22);
        if (rc.canSetFlag(encodedLocation)) {
            rc.setFlag(encodedLocation);
        }
    }

    static void sendCommand(int command) throws GameActionException{
        int encodedLocation = (command << 22);
        if(rc.canSetFlag(encodedLocation)){
            rc.setFlag(encodedLocation);
        }
    }

    /* ENCODED INFO AS SUCH
         2^23     2^14      2^7       2^0
         [         |         |        ]
             10         7        7
             extra      x        y
    */

    /**
     * Decodes the location from the flag of a robot.
     */
    static final int NBITS = 7;
    static final int BITMASK = (1 << NBITS) - 1;

    public static MapLocation getLocationFromFlag(int flag){
        int y = flag & BITMASK;
        int x = (flag >> NBITS) & BITMASK;

        MapLocation currentLocation = rc.getLocation();
        int offsetX128 = currentLocation.x >> NBITS;
        int offsetY128 = currentLocation.y >> NBITS;
        MapLocation actualLocation = new MapLocation((offsetX128 << NBITS) + x, (offsetY128 << NBITS) + y);

        // You can probably code this in a neater way, but it works
        MapLocation alternative = actualLocation.translate(-(1 << NBITS), 0);
        if (rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)) {
            actualLocation = alternative;
        }
        alternative = actualLocation.translate(1 << NBITS, 0);
        if (rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)) {
            actualLocation = alternative;
        }
        alternative = actualLocation.translate(0, -(1 << NBITS));
        if (rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)) {
            actualLocation = alternative;
        }
        alternative = actualLocation.translate(0, 1 << NBITS);
        if (rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)) {
            actualLocation = alternative;
        }
        return actualLocation;
    }

    public static int getExtraInformationFromFlag(int flag) {
        int mask = (1 << 8) - 1;
        return ((flag >> (2*NBITS)) & mask);
    }

    public static int getCommandFromFlag(int flag) {
        return (flag >> 22);
    }
}
