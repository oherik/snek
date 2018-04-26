package se.cygni.snake;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.WebSocketSession;
import se.cygni.snake.api.event.*;
import se.cygni.snake.api.exception.InvalidPlayerName;
import se.cygni.snake.api.model.*;
import se.cygni.snake.api.response.PlayerRegistered;
import se.cygni.snake.api.util.GameSettingsUtils;
import se.cygni.snake.client.AnsiPrinter;
import se.cygni.snake.client.BaseSnakeClient;
import se.cygni.snake.client.MapCoordinate;
import se.cygni.snake.client.MapUtil;
import se.cygni.snake.utils.astar;
import se.cygni.snake.utils.utils;

import java.util.*;

public class SimpleSnakePlayer extends BaseSnakeClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleSnakePlayer.class);

    // Set to false if you want to start the game from a GUI
    private static final boolean AUTO_START_GAME = true;

    // Personalise your game ...
    private static final String SERVER_NAME = "snake.cygni.se";
    private static  final int SERVER_PORT = 80;

    private static final GameMode GAME_MODE = GameMode.TRAINING;
    private static final String SNAKE_NAME = "Snek";

    // Set to false if you don't want the game world printed every game tick.
    private static final boolean ANSI_PRINTER_ACTIVE = false;
    private AnsiPrinter ansiPrinter = new AnsiPrinter(ANSI_PRINTER_ACTIVE, true);


    private MapCoordinate lastTailCoordinate = null;



    public static void main(String[] args) {
        SimpleSnakePlayer simpleSnakePlayer = new SimpleSnakePlayer();

        try {
            ListenableFuture<WebSocketSession> connect = simpleSnakePlayer.connect();
            connect.get();
        } catch (Exception e) {
            LOGGER.error("Failed to connect to server", e);
            System.exit(1);
        }

        startTheSnake(simpleSnakePlayer);
    }

    /**
     * The Snake client will continue to run ...
     * : in TRAINING mode, until the single game ends.
     * : in TOURNAMENT mode, until the server tells us its all over.
     */
    private static void startTheSnake(final SimpleSnakePlayer simpleSnakePlayer) {
        Runnable task = () -> {
            do {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (simpleSnakePlayer.isPlaying());

            LOGGER.info("Shutting down");
        };

        Thread thread = new Thread(task);
        thread.start();
    }

    final int maxSelfFoodSearchCost = 40;
    final int maxPathToOwnTailCost = 300;
    final int maxOthersFoodSearchCost = 40;
    final int maxPathExtensions = 200;
    final int maxFloodDepth = 200; //eller 400
    final int maxSafeSearchFloodDepth = 40;
    final int maxHuntTailCost = 150;
    final int foodRewardFloodFill = 0; //Dessa kanske kan sättas till 0
    final int sneakHeadPenaltyInFloodFill = 10; //TODO implementera detta :)
    final int edgePenaltyInFloodFill = 2; //TODO testa detta :D
    final int huntPreference = 1; // TODO denna kan tweakas
    final int hamiltonCutoff = 5; // needs to be >= 1

    public SnakeDirection huntHead(MapUtil mapUtil, MapUpdateEvent mapUpdateEvent){
        MapCoordinate current = mapUtil.getMyPosition();
        MapCoordinate nextToHead = mapUtil.getSnakeSpread(mapUpdateEvent.getReceivingPlayerId())[1];
        int dY = current.y - nextToHead.y;
        int dX = current.x - nextToHead.x;
        //  System.out.println("#Hamilton");
        SnakeDirection currentDir = getDirection(dX,dY);


        //TODO Kolla om snett bakom! Isf DÖDA! :D :D :D :D


        return null;
    }




    public SnakeDirection survive(MapUtil mapUtil, MapUpdateEvent mapUpdateEvent){

        //Shortest to tail
        //Extend
        MapCoordinate current = mapUtil.getMyPosition();
        ArrayList<MapCoordinate> longestPath = getLongestPathToSafePlace(mapUpdateEvent, mapUtil);
        if(longestPath != null && longestPath.size() > hamiltonCutoff){
            MapCoordinate goTo = longestPath.get(longestPath.size()-2); //TODO var -2
            System.out.println("#Hamilton");
            SnakeDirection chosen =  utils.getDirectionToNeighbor(current,goTo);
            if (mapUtil.isTileAvailableForMovementTo(utils.getNeighbor(current, chosen))){//TODO why yho????
                return chosen;
            }
          //  int dY = goTo.y - current.y;
           //int dX = goTo.x - current.x;

           // return getDirection(dX,dY);


        }

        //Räkna ut current dir
        if(mapUtil.getSnakeSpread(mapUpdateEvent.getReceivingPlayerId()).length < 2){
            return goRandom(mapUtil);
        }

        MapCoordinate nextToHead = mapUtil.getSnakeSpread(mapUpdateEvent.getReceivingPlayerId())[1];
        int dY = current.y - nextToHead.y;
        int dX = current.x - nextToHead.x;
        //  System.out.println("#Hamilton");
        SnakeDirection currentDir = getDirection(dX,dY);

        MapCoordinate inFrontOfHead = utils.getNeighbor(current,currentDir);
     //   if(!mapUtil.isTileAvailableForMovementTo(inFrontOfHead)){
           // System.out.println("Can't go straight ahead");
            List<SnakeDirection> directions = new ArrayList<>();

            // Let's see in which directions I can move
            for (SnakeDirection direction : SnakeDirection.values()) {
                if (mapUtil.canIMoveInDirection(direction)) {
                    directions.add(direction);
                }
            }
            if(directions.size() == 0){
                System.out.println("Ded");
            }
            if(directions.size() == 1){
                System.out.println("Only wae");
                return directions.get(0);
            }
            if(directions.size() == 2){
                MapCoordinate aNieghbor = utils.getNeighbor(current, directions.get(0));
                MapCoordinate bNeighbor = utils.getNeighbor(current, directions.get(1));
                int aAreaSize = floodedSize(aNieghbor,mapUtil, maxFloodDepth);
                int bAreaSize = floodedSize(bNeighbor, mapUtil, maxFloodDepth);
                System.out.println("Fllood! a : " + aAreaSize + "  b: " +bAreaSize);
                return aAreaSize > bAreaSize ?  directions.get(0) : directions.get(1);
            }
            if(directions.size() == 3){
                MapCoordinate aNieghbor = utils.getNeighbor(current, directions.get(0));
                MapCoordinate bNeighbor = utils.getNeighbor(current, directions.get(1));
                MapCoordinate cNeighbor = utils.getNeighbor(current, directions.get(2));
                HashMap<SnakeDirection, Integer> floodSize = new HashMap<>();
                floodSize.put(directions.get(0),  floodedSize(aNieghbor,mapUtil, maxFloodDepth));
                floodSize.put(directions.get(1),  floodedSize(bNeighbor,mapUtil, maxFloodDepth));
                floodSize.put(directions.get(2),  floodedSize(cNeighbor,mapUtil, maxFloodDepth));

                System.out.println("Fllood! hashapen : " + floodSize );
                SnakeDirection chose =  floodSize.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
                return chose;
            }



      //  }



       // System.out.println("#Rrandom");
        return goRandom(mapUtil);

        //None? Go to a place with empty space
        //Or an obstacle right in front of you? Floodfill both sides.
        // Or heuristic? Just check number of free cells up, left, right

    }

    public SnakeDirection goRandom(MapUtil mapUtil){
        List<SnakeDirection> directions = new ArrayList<>();
        SnakeDirection  chosenDirection;
        // Let's see in which directions I can move
        for (SnakeDirection direction : SnakeDirection.values()) {
            if (mapUtil.canIMoveInDirection(direction)) {
                directions.add(direction);
            }
        }
        Random r = new Random();
        chosenDirection = SnakeDirection.DOWN;
        // Choose a random direction
        if (!directions.isEmpty()){
            chosenDirection = directions.get(r.nextInt(directions.size()));
        }
        return chosenDirection;
    }

    private HashMap<MapCoordinate, Boolean> floodedVisited;
    private String floodedPlayerId;
    private MapUtil floodedMapUtil;

    public ArrayList<MapCoordinate> getPathToSafePlace(MapUpdateEvent mapUpdateEvent, MapUtil mapUtil, MapCoordinate current, int initialPathLength, List<MapCoordinate> forbiddenTiles){
        if(safeSpot == null){
            ArrayList<MapCoordinate> safe = getGoodPositions(mapUpdateEvent, mapUtil);

            Collections.sort(safe, new Comparator<MapCoordinate>() {
                public int compare(MapCoordinate left, MapCoordinate right) {
                    return Integer.compare(floodedSize(right, mapUtil, maxSafeSearchFloodDepth),floodedSize(left, mapUtil, maxSafeSearchFloodDepth));
                }
            });

            ArrayList<MapCoordinate> tempPath;
            for(MapCoordinate mc : safe){
                tempPath = astar.getPath(current,mc,mapUtil,maxPathToOwnTailCost,initialPathLength,forbiddenTiles,null, mapUpdateEvent.getReceivingPlayerId());
                if(tempPath != null && tempPath.size() > 1){
                    safeSpot = mc;
                    return tempPath;
                }
            }
        }
        else{
            return astar.getPath(current,safeSpot,mapUtil,maxPathToOwnTailCost,0,null,null, mapUpdateEvent.getReceivingPlayerId());
        }



return null;

    }

    public ArrayList<MapCoordinate> getGoodPositions(MapUpdateEvent mapUpdateEvent, MapUtil mapUtil){
        ArrayList safePosition = getSafePositions(mapUpdateEvent, mapUtil);
        int sizen = safePosition.size();
        ArrayList goodPositions = new ArrayList();
        goodPositions.add(safePosition.get(sizen/2));
        goodPositions.add(safePosition.get(sizen/4));
        goodPositions.add(safePosition.get(sizen/4+sizen/2));
        goodPositions.add(safePosition.get(sizen/8));
        goodPositions.add(safePosition.get(sizen/8 + sizen/ 2));
        goodPositions.add(safePosition.get(sizen/8 +  sizen/4 +sizen/ 2));
        goodPositions.add(safePosition.get(sizen/16 ));
        goodPositions.add(safePosition.get(sizen/8 + sizen/ 16));
        goodPositions.add(safePosition.get(sizen/8 +  sizen/16 +sizen/ 4));
        goodPositions.add(safePosition.get(sizen/8 +  sizen/16 +sizen/ 4 + sizen/2));
        goodPositions.add(safePosition.get(sizen/8));
        goodPositions.add(safePosition.get(sizen-1));
        Iterator<MapCoordinate> iter = goodPositions.iterator();
       MapCoordinate temp;
       while(iter.hasNext()) {
           temp = iter.next();
           int size = floodedSize(temp, mapUtil, 20);
           if (size < 20) {
               iter.remove();
           }


       }


        return goodPositions;

        //TODO factor in food and stuff :)
    }


    public ArrayList<MapCoordinate> getSafePositions(MapUpdateEvent mapUpdateEvent, MapUtil mapUtil){

        int[] allpos = new int[mapUpdateEvent.getMap().getHeight()*mapUpdateEvent.getMap().getHeight()];
        int[] obstacles = mapUpdateEvent.getMap().getObstaclePositions();
        ArrayList<MapCoordinate> safePos =  new ArrayList();
        int i = 0;

        List obstacleList = Arrays.asList(obstacles);
        for(int pos : allpos) {
            if (!obstacleList.contains(pos)) {
                safePos.add(mapUtil.translatePosition(pos));
            }
        }
        return safePos;





    }

    public int floodedSize(MapCoordinate start, MapUtil mapUtil, int maxDepth){
        HashMap<MapCoordinate, Boolean> visited = new HashMap<>(300);
        Deque<MapCoordinate> queue = new ArrayDeque();

        int penalty = 0;

        visited.put(start, true);
        queue.add(start);
        int depth = 0;
        while (!queue.isEmpty() && depth < maxDepth) {
            MapCoordinate current = queue.pop();
            MapCoordinate[] neighbors = new MapCoordinate[4];
            neighbors[0] = utils.getNeighbor(current, SnakeDirection.DOWN);
            neighbors[1] = utils.getNeighbor(current, SnakeDirection.RIGHT);
            neighbors[2] = utils.getNeighbor(current, SnakeDirection.UP);
            neighbors[3] = utils.getNeighbor(current, SnakeDirection.LEFT);

            for(MapCoordinate neighbor : neighbors){
                if(!visited.containsKey(neighbor)) {
                    if (mapUtil.isTileAvailableForMovementTo(neighbor)) {
                        visited.put(neighbor, true);
                        queue.add(neighbor);
                        if (mapUtil.getTileAt(current).getContent().equals("food")) {
                            penalty -= foodRewardFloodFill;
                        }

                    } else {
                        if (mapUtil.isCoordinateOutOfBounds(current)) {
                            penalty += edgePenaltyInFloodFill;
                        }


                        String cont = mapUtil.getTileAt(current).getContent();
                        if (cont.equals("snakehead") && !((MapSnakeHead) mapUtil.getTileAt(current)).getName().equals(SNAKE_NAME)) {
                            penalty += sneakHeadPenaltyInFloodFill;
                        }
                    }
                }
            }
            depth ++;


        }
        return visited.size() - penalty;
        /*



        this.floodedVisited = new HashMap();
        this.floodedPlayerId = playerID;
        this.floodedMapUtil = mapUtil;
        MapCoordinate current = start;
        floodedVisited.put(current, true);
        floodedRec(utils.getNeighbor(current, SnakeDirection.DOWN),1);
        floodedRec(utils.getNeighbor(current, SnakeDirection.RIGHT),  1);
        floodedRec(utils.getNeighbor(current, SnakeDirection.LEFT),  1);
        floodedRec(utils.getNeighbor(current, SnakeDirection.UP),  1);

        return floodedVisited.size() - sneakHeadPenaltyInFloodFill;
        */
    }

    private int floodedRec(MapCoordinate current,int depth){
        if(!utils.isWalkable(current,floodedMapUtil,floodedPlayerId,0,null,null)){
            if(floodedMapUtil.isCoordinateOutOfBounds(current)){
                return edgePenaltyInFloodFill;
            }



            String cont = floodedMapUtil.getTileAt(current).getContent();
            if(cont.equals("snakehead") && !((MapSnakeHead) floodedMapUtil.getTileAt(current)).getPlayerId().equals(floodedPlayerId)   ){
                return sneakHeadPenaltyInFloodFill;
            }

            return 0;
        }
        if(depth > maxFloodDepth){
            return 0;
        }
        floodedVisited.put(current, true);
        floodedRec(utils.getNeighbor(current, SnakeDirection.DOWN),depth+1);
        floodedRec(utils.getNeighbor(current, SnakeDirection.RIGHT), depth+1);
        floodedRec(utils.getNeighbor(current, SnakeDirection.LEFT), depth+1);
        floodedRec(utils.getNeighbor(current, SnakeDirection.UP), depth+1);
        return 0;

    }

    public ArrayList<MapCoordinate> getLongestPathToSafePlace(MapUpdateEvent mapUpdateEvent, MapUtil mapUtil){
        MapCoordinate head = mapUtil.getMyPosition();
        ArrayList<MapCoordinate> path = getPathToSafePlace(mapUpdateEvent,mapUtil,head,0,null);// getShortestPathToTail(head, mapUtil,mapUpdateEvent,0,null,null);
        if(path == null){
            return null;
        }
        MapCoordinate current = head;

        HashMap<MapCoordinate, Boolean> visited = new HashMap<>();
        //All in the shortest path are visited
        visited.put(current, true);
        for(MapCoordinate coordinate : path){
            visited.put(coordinate, true);
        }

        //Extend between each pair
        int index = 0;
        int extensions = 0;
        boolean extended = false;
        SnakeDirection[] tests;
        MapCoordinate next, currentTest, nextTest;
        SnakeDirection currentDirection;
        while(extensions < maxPathExtensions && index < path.size() - 1){
            next = path.get(index + 1);
            currentDirection = utils.getDirectionToNeighbor(current, next);

            if(currentDirection == SnakeDirection.LEFT || currentDirection == SnakeDirection.RIGHT){
               tests = new SnakeDirection[]{SnakeDirection.UP, SnakeDirection.DOWN};
            } else {
                tests = new SnakeDirection[]{SnakeDirection.LEFT, SnakeDirection.RIGHT};
            }
            extended = false;
            for(SnakeDirection testDirection : tests){
                currentTest = utils.getNeighbor(current,testDirection);
                nextTest = utils.getNeighbor(next,testDirection);
                if(isValidForLongestPath(currentTest, mapUtil, visited) && isValidForLongestPath(nextTest, mapUtil, visited)

                        ){
                    visited.put(currentTest, true);
                    visited.put(nextTest, true);
                    path.add(index+1, currentTest);
                    path.add(index +2, nextTest);
                    extended = true;
                    extensions++;
                    break;
                }
            }
            if(!extended){
                current = next;
                index++;

            }


        }
        return path;


    }

    private boolean isValidForLongestPath(MapCoordinate mapCoordinate, MapUtil mapUtil, HashMap<MapCoordinate, Boolean> visited){
        return mapUtil.isTileAvailableForMovementTo(mapCoordinate) && (visited.get(mapCoordinate) == null || !visited.get(mapCoordinate));  //Detta kan ju lösas genom bara kolla om den contains
    }

    private MapCoordinate safeSpot;

    public ArrayList<MapCoordinate> huntTails(MapUtil mapUtil,  MapUpdateEvent mapUpdateEvent){
        MapCoordinate current = mapUtil.getMyPosition();
        SnakeInfo[] snakes = mapUpdateEvent.getMap().getSnakeInfos();

        ArrayList<MapCoordinate> enemyTails = new ArrayList();
       // int i = 0;
        int[] temp;
        for(SnakeInfo info : snakes){
            if(!info.getName().equals(SNAKE_NAME) && info.getTailProtectedForGameTicks() < 1){ //TODO kolla protected sene gentligen när vi vet avståndet dit men detta får funka som en heuristic nu :)
                temp = info.getPositions();
                if(temp.length >0){
                    enemyTails.add(mapUtil.translatePosition(temp[temp.length-1]));

                }

            }
        }
        ArrayList<ArrayList<MapCoordinate>> paths = new ArrayList<ArrayList<MapCoordinate>>();
        int j = 0;
        ArrayList<MapCoordinate> tempPath;
        for(MapCoordinate tail : enemyTails){
            ArrayList<MapCoordinate> allowedTiles = new ArrayList<>();
            allowedTiles.add(tail);


            tempPath =  astar.getPath(current,tail,mapUtil,maxHuntTailCost,0,null,allowedTiles,mapUpdateEvent.getReceivingPlayerId());


            if(tempPath != null && tempPath.size()>1){
                MapCoordinate[] snakeSpread = mapUtil.getSnakeSpread(mapUpdateEvent.getReceivingPlayerId());
              //  ArrayList<MapCoordinate> allowedTiles = new ArrayList<>(Arrays.asList(snakeSpread)) ;// mapUtil.getSnakeSpread(mapUpdateEvent.getReceivingPlayerId()); TODO kanske funkar
                int snakeSize = snakeSpread.length;
                int futureSnakeSize = (snakeSize+tempPath.size())/2+1;
                List<MapCoordinate> forbiddenTiles = null;
                int pathIndex = tempPath.size()-futureSnakeSize;
                forbiddenTiles = pathIndex < 1 ? tempPath :  tempPath.subList(tempPath.size()-futureSnakeSize, tempPath.size());




                ArrayList shortestPathToSafePlace = getPathToSafePlace(mapUpdateEvent,mapUtil,current, tempPath.size(), forbiddenTiles);//, mapUtil, mapUpdateEvent, tempPath.size(), forbiddenTiles, null);


                if(shortestPathToSafePlace != null && shortestPathToSafePlace.size() > 1) {
                    paths.add(tempPath);
                }else{
                  ArrayList shortestPathToTail = getShortestPathToTail(current, mapUtil, mapUpdateEvent, tempPath.size(),forbiddenTiles, null);// getPathToSafePlace(mapUpdateEvent,mapUtil,current);
                   if(shortestPathToTail != null && shortestPathToTail.size() > 1) {
                        paths.add(tempPath);
                    }
                }

                //Check if safe


            }
        }

        Collections.sort(paths, new Comparator<ArrayList>() {
            public int compare(ArrayList left, ArrayList right) {
                return Integer.compare(left.size(), right.size());
            }
        });

        for(ArrayList path : paths){
            if(path != null){
                return path;
            }
        }




        return null;
    }




    public ArrayList<MapCoordinate> huntFood(MapUpdateEvent mapUpdateEvent, MapUtil mapUtil){

        //Lets find food
        ArrayList<MapCoordinate> foodTileList = new ArrayList<MapCoordinate>(Arrays.asList( mapUtil.listCoordinatesContainingFood()));

        if(foodTileList.isEmpty()){
            return null;
        }

        MapCoordinate current = mapUtil.getMyPosition();

        Collections.sort(foodTileList, new Comparator<MapCoordinate>() {
            public int compare(MapCoordinate left, MapCoordinate right) {
                //return Integer.compare(left.getManhattanDistanceTo(current), right.getManhattanDistanceTo(current));

                ArrayList apath = astar.getPath(mapUtil.getMyPosition(), left, mapUtil, maxSelfFoodSearchCost, 0, null, null, mapUpdateEvent.getReceivingPlayerId());
                ArrayList bpath = astar.getPath(mapUtil.getMyPosition(), right, mapUtil, maxSelfFoodSearchCost, 0, null, null, mapUpdateEvent.getReceivingPlayerId());
    //TODO lol men spara alla paths innan!
                int a = apath == null ? Integer.MAX_VALUE : apath.size();
                int b = bpath == null ? Integer.MAX_VALUE : bpath.size();


                return Integer.compare(a,b);
            }
        });

        MapCoordinate foodTile = foodTileList.get(0);
        ArrayList<MapCoordinate> path = null;
        Iterator<MapCoordinate> i = foodTileList.iterator();
        while(i.hasNext()) {
            foodTile =i.next();
           //Find path
            path = astar.getPath(current, foodTile, mapUtil, maxSelfFoodSearchCost, 0, null, null, mapUpdateEvent.getReceivingPlayerId());



            if(path == null || path.size() < 2){// || getShortestPathToTail(foodTile, mapUtil, path.size()) == null) {
                i.remove();
            } else {
                //Check if safe>
                MapCoordinate[] snakeSpread = mapUtil.getSnakeSpread(mapUpdateEvent.getReceivingPlayerId());
                ArrayList<MapCoordinate> allowedTiles = new ArrayList<>(Arrays.asList(snakeSpread)) ;// mapUtil.getSnakeSpread(mapUpdateEvent.getReceivingPlayerId()); TODO kanske funkar
                int snakeSize = snakeSpread.length;
                int futureSnakeSize = (snakeSize+path.size())/2+1;
                List<MapCoordinate> forbiddenTiles = null;
                int pathIndex = path.size()-futureSnakeSize;
                forbiddenTiles = pathIndex < 1 ? path :  path.subList(path.size()-futureSnakeSize, path.size());

                ArrayList shortestToTail = getShortestPathToTail(foodTile, mapUtil, mapUpdateEvent, path.size(), forbiddenTiles, allowedTiles);
                if(shortestToTail == null || shortestToTail.size() < 2) {
                  i.remove();
                }


                //Check if the other snakes can't reach it first
                SnakeInfo[] snakeInfos = mapUpdateEvent.getMap().getSnakeInfos();
                for (SnakeInfo snakeInfo : snakeInfos) {

                   if(snakeInfo.getName().equals(getName()) || snakeInfo.getPositions().length == 0){
                       break;
                   }

                    int otherPos = snakeInfo.getPositions()[0];
                    ArrayList<MapCoordinate> otherPath = astar.getPath(mapUtil.translatePosition(otherPos), foodTile, mapUtil, maxOthersFoodSearchCost, 0, null, null, mapUpdateEvent.getReceivingPlayerId());
                    if (otherPath != null && otherPath.size() <= path.size()) {
                        i.remove();
                    } else {
                       return path;
                      //  int dY = goTo.y - current.y;
                      //  int dX = goTo.x - current.x;

                       // return getDirection(dX,dY);
                    }
                }
            }
        }


    return null;
    }



    public MapCoordinate getTail( MapUtil mapUtil, MapUpdateEvent mapUpdateEvent){
        String myId = mapUpdateEvent.getReceivingPlayerId();

        MapCoordinate[] snake = mapUtil.getSnakeSpread(myId);
        return snake[snake.length-1];

        /*
        MapCoordinate tail = this.lastTailCoordinate;
        if(tail == null || !utils.isWalkable(tail, mapUtil, 0, null, null)){
            String myId = mapUpdateEvent.getReceivingPlayerId(); //getMyId(mapUtil);

            MapCoordinate[] snake = mapUtil.getSnakeSpread(myId);
            tail = snake[snake.length-1];
            List<MapCoordinate> neighbors = utils.getWalkableNeighbors(tail,mapUtil,0, null, null);
            for(MapCoordinate neighbor : neighbors){
                if(mapUtil.isTileAvailableForMovementTo(neighbor)){
                    tail = neighbor;
                    break;
                }
            }
        }
        return tail;
        */
    }

    public ArrayList getShortestPathToTail(MapCoordinate coordinate, MapUtil mapUtil, MapUpdateEvent mapUpdateEvent, int initialCost, List<MapCoordinate> forbiddenTiles, List<MapCoordinate> allowedTiles ){
        String myId = mapUpdateEvent.getReceivingPlayerId();
        MapCoordinate[] snake = mapUtil.getSnakeSpread(myId);
        MapCoordinate tail = snake[snake.length-1];
        //Get walkable neighbors of the tail
        List<MapCoordinate> tailNeighbors = utils.getWalkableNeighbors(tail,mapUpdateEvent.getReceivingPlayerId(),mapUtil, 0,null,null);

        for(MapCoordinate mc : tailNeighbors){
            ArrayList path = astar.getPath(coordinate, mc, mapUtil, maxPathToOwnTailCost, initialCost, forbiddenTiles, allowedTiles, mapUpdateEvent.getReceivingPlayerId());
            if(path != null){
                return path;
            }
        }

        return null;
    }


    @Override
    public void onMapUpdate(MapUpdateEvent mapUpdateEvent) {
        long startTime = System.currentTimeMillis();

        safeSpot = null;



        ansiPrinter.printMap(mapUpdateEvent);

        // MapUtil contains lot's of useful methods for querying the map!
        MapUtil mapUtil = new MapUtil(mapUpdateEvent.getMap(), getPlayerId());

        SnakeDirection  chosenDirection = null;
        ArrayList<MapCoordinate> chosenPath;
        ArrayList<MapCoordinate>  huntPath = huntTails(mapUtil, mapUpdateEvent);
        ArrayList<MapCoordinate> foodPath = huntFood(mapUpdateEvent, mapUtil);

        MapCoordinate current = mapUtil.getMyPosition();

        if(huntPath != null && huntPath.size() > 1){
            if(foodPath != null && foodPath.size() > 1){
                chosenPath = foodPath.size() * huntPreference < huntPath.size() ? foodPath : huntPath;
                System.out.println(foodPath.size() * huntPreference <  huntPath.size() ? "Mat" : "Hunt");
            } else {
                chosenPath = huntPath;
                System.out.println("Hunt");
            }
        } else{
            if(foodPath != null && foodPath.size() > 1){
                chosenPath = foodPath;
                System.out.println("mat");
            } else {
                chosenPath = null;
            }
        }


        if(chosenPath != null && chosenPath.size() >1 ){
            MapCoordinate goTo = chosenPath.get(chosenPath.size()-2);
            if(floodedSize(goTo,mapUtil,40) > 40){//TODO eller?
                chosenDirection = utils.getDirectionToNeighbor(current,goTo);
            }

        }

        MapCoordinate nextPos = utils.getNeighbor(mapUtil.getMyPosition(),chosenDirection);

        if(chosenDirection == null || !isWalkable(nextPos, mapUtil, mapUpdateEvent)){ //TODO varför är den dum ibland????

          chosenDirection = survive(mapUtil, mapUpdateEvent);
          //  System.out.println("Survive: " + chosenDirection);
        }


       // chosenDirection = survive(mapUtil, mapUpdateEvent);



        // Register action here!
        registerMove(mapUpdateEvent.getGameTick(), chosenDirection);

        //String myId = mapUpdateEvent.getReceivingPlayerId();//mapUtil);

       // MapCoordinate[] snake = mapUtil.getSnakeSpread(myId);
       // lastTailCoordinate = snake[snake.length-1];

      System.out.println("Tid: " + (System.currentTimeMillis() - startTime));

      SnakeInfo[] snakeInfos = mapUpdateEvent.getMap().getSnakeInfos();
        for(SnakeInfo si : snakeInfos){
            if(si.getName().equals(SNAKE_NAME)){
                System.out.println(si.getPoints());
            }
        }
      System.out.println("");
    }

    private boolean isWalkable(MapCoordinate coord, MapUtil util, MapUpdateEvent mapUpdateEvent){
        if(util.isTileAvailableForMovementTo(coord)){
            return true;
        }
        if(util.getTileAt(coord).getContent().equals("snakebody") &&  (((MapSnakeBody) util.getTileAt(coord)).isTail())) {
            SnakeInfo[] snakes = mapUpdateEvent.getMap().getSnakeInfos();
            for (SnakeInfo info : snakes) {
                if (info.getId().equals(((MapSnakeBody) util.getTileAt(coord)).getPlayerId()) && info.getTailProtectedForGameTicks() < 1) {
                    return true;
                }

            }
        }

        return false;
    }

    public SnakeDirection getDirection(int dX, int dY){
        if(dX == 0){
            if(dY == -1){
                return SnakeDirection.UP;
            }
            if(dY == 1){
                return SnakeDirection.DOWN;
            }
        }
        if(dY == 0){
            if(dX == -1){
                return SnakeDirection.LEFT;
            }
            if(dX == 1){
                return SnakeDirection.RIGHT;
            }
        }
        return null;
    }


    @Override
    public void onInvalidPlayerName(InvalidPlayerName invalidPlayerName) {
        LOGGER.debug("InvalidPlayerNameEvent: " + invalidPlayerName);
    }

    @Override
    public void onSnakeDead(SnakeDeadEvent snakeDeadEvent) {
        LOGGER.info("A snake {} died by {}",
                snakeDeadEvent.getPlayerId(),
                snakeDeadEvent.getDeathReason());
    }

    @Override
    public void onGameResult(GameResultEvent gameResultEvent) {
        LOGGER.info("Game result:");
        gameResultEvent.getPlayerRanks().forEach(playerRank -> LOGGER.info(playerRank.toString()));
    }

    @Override
    public void onGameEnded(GameEndedEvent gameEndedEvent) {
        LOGGER.debug("GameEndedEvent: " + gameEndedEvent);
    }

    @Override
    public void onGameStarting(GameStartingEvent gameStartingEvent) {
        LOGGER.debug("GameStartingEvent: " + gameStartingEvent);
    }

    @Override
    public void onPlayerRegistered(PlayerRegistered playerRegistered) {
        LOGGER.info("PlayerRegistered: " + playerRegistered);

        if (AUTO_START_GAME) {
            startGame();
        }
    }

    @Override
    public void onTournamentEnded(TournamentEndedEvent tournamentEndedEvent) {
        LOGGER.info("Tournament has ended, winner playerId: {}", tournamentEndedEvent.getPlayerWinnerId());
        int c = 1;
        for (PlayerPoints pp : tournamentEndedEvent.getGameResult()) {
            LOGGER.info("{}. {} - {} points", c++, pp.getName(), pp.getPoints());
        }
    }

    @Override
    public void onGameLink(GameLinkEvent gameLinkEvent) {
        LOGGER.info("The game can be viewed at: {}", gameLinkEvent.getUrl());
    }

    @Override
    public void onSessionClosed() {
        LOGGER.info("Session closed");
    }

    @Override
    public void onConnected() {
        LOGGER.info("Connected, registering for training...");
        GameSettings gameSettings = GameSettingsUtils.trainingWorld();
        registerForGame(gameSettings);
    }

    @Override
    public String getName() {
        return SNAKE_NAME;
    }

    @Override
    public String getServerHost() {
        return SERVER_NAME;
    }

    @Override
    public int getServerPort() {
        return SERVER_PORT;
    }

    @Override
    public GameMode getGameMode() {
        return GAME_MODE;
    }
}
