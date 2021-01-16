package framework1;

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
    public static void sendLocation(int extraInformation) throws GameActionException{
        MapLocation location = here;
        int x = location.x, y = location.y;
        int encodedLocation = (x % 128) * 128 + (y%128) + extraInformation * 128 * 128;
        if(rc.canSetFlag(encodedLocation)) rc.setFlag(encodedLocation);
    }

    /**
     * Generic to send a location on the map.
     */
    public static void sendLocation(MapLocation loc, int extraInformation) throws GameActionException{
        MapLocation location = loc;
        int x = location.x, y = location.y;

        int encodedLocation = (x % 128) * 128 + (y%128) + extraInformation * 128 * 128;
        if(rc.canSetFlag(encodedLocation)) {
            rc.setFlag(encodedLocation);
            //System.out.print("FLAG SET: " + encodedLocation);
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
    public static MapLocation getLocationFromFlag(int flag){
        int y = flag % 128;
        int x = (flag / 128) % 128;
        int extraInformation = flag / 128 / 128;

        MapLocation currentLocation = rc.getLocation();
        int offsetX128 = currentLocation.x /128;
        int offsetY128 = currentLocation.y/128;
        MapLocation actualLocation = new MapLocation(offsetX128 * 128 + x, offsetY128 *128 + y);

        MapLocation alternative = actualLocation.translate(-128,0);
        if(rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)) actualLocation = alternative;

        alternative = actualLocation.translate(-128,0);
        if(rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)) actualLocation = alternative;

        alternative = actualLocation.translate(-128,0);
        if(rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)) actualLocation = alternative;

        alternative = actualLocation.translate(-128,0);
        if(rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)) actualLocation = alternative;

        return actualLocation;
    }

    public static int getExtraInformationFromFlag(int flag) {
        return flag / 128 / 128;
    }
}