package se.cygni.snake.utils;
import org.omg.CORBA.INTERNAL;
import se.cygni.snake.client.MapCoordinate;
import se.cygni.snake.client.MapUtil;

import java.awt.*;
import java.util.*;
import java.util.List;

public class astar {
    static HashMap<MapCoordinate, Integer> fScore;
    static HashMap<MapCoordinate, Integer> gScore;
    final static int LARGE = 1000;

    /** The start function for calculating the shortest path between two points on the map, with no limitations of number of steps.
     */

    public static ArrayList<MapCoordinate> getPath(MapCoordinate startPos, MapCoordinate endPos, MapUtil mapUtil)
            throws NullPointerException, IndexOutOfBoundsException {
        return getPath(startPos, endPos, mapUtil, Integer.MAX_VALUE);
    }

    /** The start function for calculating the shortest path between two points on the map, a maximum number of steps included
     * @throws IndexOutOfBoundsException if the start or end position is negative
     * @throws  NullPointerException    if either of the points is null
     */
    public static ArrayList<MapCoordinate> getPath(MapCoordinate startPos, MapCoordinate endPos, MapUtil mapUtil, int maxCost)
            throws NullPointerException, IndexOutOfBoundsException {
        if (mapUtil == null)
            throw new NullPointerException("PathAlgorithm: the navigational mesh cannot be null");
        if(startPos.equals(endPos)){
            ArrayList<MapCoordinate> singlePointArray = new ArrayList<>();
            singlePointArray.add(endPos);
            return singlePointArray;
        }
        return calculatePath(startPos,endPos,mapUtil,maxCost);
    }

    /**
     * Calculates the shortest path given the data from the constructor
     *
     * @return the shortest path or, if none found, null
     */

    private static ArrayList<MapCoordinate> calculatePath(MapCoordinate startPos, MapCoordinate endPos,MapUtil mapUtil, int maxCost) {
        HashSet<MapCoordinate> closedSet = new HashSet<>();
        Queue<MapCoordinate> openSet = new PriorityQueue<>(coordinateComparator);
        openSet.add(startPos);

        HashMap<MapCoordinate, MapCoordinate> cameFrom = new HashMap<>();

        gScore = new HashMap<>();
        gScore.put(startPos,0);

        fScore = new HashMap<>();
        fScore.put(startPos, startPos.getManhattanDistanceTo(endPos));
        while(!openSet.isEmpty()){
            MapCoordinate currentPos = openSet.poll();
            if(currentPos.equals(endPos)){
            //   System.out.println("Hittat en väg till " + endPos);
                return reconstructPath(cameFrom, currentPos);
            }
            closedSet.add(currentPos);
            gScore.putIfAbsent(currentPos, LARGE);

            for(MapCoordinate neighbor : getWalkableNeighbors(currentPos, mapUtil)){
                if(!closedSet.contains(neighbor)) {
                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                    }
                    gScore.putIfAbsent(neighbor, LARGE);

                    int tent_g = gScore.get(currentPos) + distanceBetween(currentPos, neighbor);

                    if (tent_g < gScore.get(neighbor)) {
                        cameFrom.put(neighbor, currentPos);
                        gScore.put(neighbor, tent_g);
                        fScore.put(neighbor, tent_g + neighbor.getManhattanDistanceTo(endPos));
                    }
                }
            }

           // }

        }
      //  System.out.println("Ingen hittad");
        return null;
    }//calculatePath

    private static List<MapCoordinate> getWalkableNeighbors(MapCoordinate current, MapUtil mapUtil){
        List<MapCoordinate> neighbors = new ArrayList<>(4);
        MapCoordinate[] allNeighbors = new MapCoordinate[4];
        allNeighbors[0] = current.translateBy(0,1);
        allNeighbors[1] = current.translateBy(1,0);
        allNeighbors[2] = current.translateBy(0,-1);
        allNeighbors[3] = current.translateBy(-1,0);
        for(MapCoordinate coordinate : allNeighbors){
            if(mapUtil.isTileAvailableForMovementTo(coordinate)){
                neighbors.add(coordinate);
            }
        }
        return neighbors;

    }

    private static int distanceBetween(MapCoordinate start, MapCoordinate next) {
        int dY = Math.abs(start.y-next.y);
        int dX = Math.abs(start.x-next.x);

        if(dY == 0 && dX == 1 || dY == 1 && dX == 0){
            return 1;
        }
        if(dY == 0 && dX == 0){
            return 0;
        }
        return LARGE;
    }


    private static ArrayList<MapCoordinate> reconstructPath(HashMap<MapCoordinate, MapCoordinate> cameFrom, MapCoordinate current) {
        ArrayList<MapCoordinate> path = new ArrayList<>();
        path.add(current);
        while(cameFrom.containsKey(current)){
            current = cameFrom.get(current);
            path.add(current);
        }
       return path;
    }

    public static Comparator<MapCoordinate> coordinateComparator = new Comparator<MapCoordinate>() {
        @Override
        public int compare(MapCoordinate o1, MapCoordinate o2) {
            fScore.putIfAbsent(o1, LARGE);
            fScore.putIfAbsent(o2, LARGE);

            if (fScore.get(o1) < fScore.get(o2))
                return -1;
            if (fScore.get(o2) < fScore.get(o1))
                return 1;
            return 0;
        }
    };

}
