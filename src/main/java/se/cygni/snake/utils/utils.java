package se.cygni.snake.utils;

import se.cygni.snake.api.event.MapUpdateEvent;
import se.cygni.snake.api.model.MapSnakeBody;
import se.cygni.snake.api.model.MapSnakeHead;
import se.cygni.snake.api.model.SnakeDirection;
import se.cygni.snake.api.model.TileContent;
import se.cygni.snake.client.MapCoordinate;
import se.cygni.snake.client.MapUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class utils {


    //Forbidden has precedence

    public static boolean isWalkable(MapCoordinate coordinate, MapUtil util, String playerID, int distanceToCoordinate, List<MapCoordinate> forbiddenTiles, List<MapCoordinate> allowedTiles ){
        if(forbiddenTiles != null && forbiddenTiles.contains(coordinate)){
            return false;
        }


      //  String content = util.getTileAt(coordinate).getContent();
        if( distanceToCoordinate < 3) { //Så man inte kör in i fiendehuvud
           // List<MapCoordinate> neighbors = new ArrayList<>(4);
            MapCoordinate[] allNeighbors = new MapCoordinate[4];
            allNeighbors[0] = coordinate.translateBy(0,1);
            allNeighbors[1] = coordinate.translateBy(1,0);
            allNeighbors[2] = coordinate.translateBy(0,-1);
            allNeighbors[3] = coordinate.translateBy(-1,0);
            for(MapCoordinate coord : allNeighbors){
                if(!util.isCoordinateOutOfBounds(coord)){
                    String cont = util.getTileAt(coord).getContent();
                    if(cont.equals("snakehead") && !((MapSnakeHead) util.getTileAt(coord)).getPlayerId().equals(playerID)   ){
                        return false;
                    }
                }

            }
        }

        return util.isTileAvailableForMovementTo(coordinate);

        /*

        if(content.equals("snakebody")||content.equals("snakehead")){



            String playerId = content.equals("snakebody") ?  ((MapSnakeBody) util.getTileAt(coordinate)).getPlayerId() : ((MapSnakeHead) util.getTileAt(coordinate)).getPlayerId() ;
            int snakeLength = util.getPlayerLength(playerId);
            MapCoordinate[] snake = util.getSnakeSpread(playerId);
            int snakepartPostition = Arrays.asList(snake).indexOf(coordinate);
            int tailLength = snakeLength - snakepartPostition;
            if(tailLength*2 < distanceToCoordinate){
             //   System.out.println("Ormen här borde flytta på sig!");
             //   return true;

            }
        }
        return false;
*/
    }

    public static List<MapCoordinate> getWalkableNeighbors(MapCoordinate current, String playerID, MapUtil mapUtil, int distanceToCurrent, List<MapCoordinate> forbiddenTiles, List<MapCoordinate> allowedTiles){
        List<MapCoordinate> neighbors = new ArrayList<>(4);
        MapCoordinate[] allNeighbors = new MapCoordinate[4];
        allNeighbors[0] = current.translateBy(0,1);
        allNeighbors[1] = current.translateBy(1,0);
        allNeighbors[2] = current.translateBy(0,-1);
        allNeighbors[3] = current.translateBy(-1,0);
        for(MapCoordinate coordinate : allNeighbors){
            if(isWalkable(coordinate, mapUtil, playerID,distanceToCurrent + 1, forbiddenTiles, allowedTiles)){//mapUtil.isTileAvailableForMovementTo(coordinate)){
                neighbors.add(coordinate);
            }
        }
        return neighbors;
    }

    public static MapCoordinate getNeighbor(MapCoordinate current, SnakeDirection direction){
        switch(direction){
            case UP:
                return current.translateBy(0,-1);
            case DOWN:
                return current.translateBy(0,1);
            case RIGHT:
                return current.translateBy(1,0);
            case LEFT:
                return current.translateBy(-1,0);
        }
        return null;
    }

    public static SnakeDirection getDirectionToNeighbor(MapCoordinate current, MapCoordinate neighbor){
        switch(neighbor.y - current.y){
            case -1:
                return SnakeDirection.UP;
            case 1:
                return SnakeDirection.DOWN;
        }
        switch(neighbor.x - current.x){
            case -1:
                return SnakeDirection.LEFT;
            case 1:
                return SnakeDirection.RIGHT;
        }
        return null;
    }


}
