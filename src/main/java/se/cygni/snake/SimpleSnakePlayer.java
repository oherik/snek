package se.cygni.snake;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.WebSocketSession;
import se.cygni.snake.api.event.*;
import se.cygni.snake.api.exception.InvalidPlayerName;
import se.cygni.snake.api.model.GameMode;
import se.cygni.snake.api.model.GameSettings;
import se.cygni.snake.api.model.PlayerPoints;
import se.cygni.snake.api.model.SnakeDirection;
import se.cygni.snake.api.response.PlayerRegistered;
import se.cygni.snake.api.util.GameSettingsUtils;
import se.cygni.snake.client.AnsiPrinter;
import se.cygni.snake.client.BaseSnakeClient;
import se.cygni.snake.client.MapCoordinate;
import se.cygni.snake.client.MapUtil;
import se.cygni.snake.utils.astar;

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

    @Override
    public void onMapUpdate(MapUpdateEvent mapUpdateEvent) {
        ansiPrinter.printMap(mapUpdateEvent);

        // MapUtil contains lot's of useful methods for querying the map!
        MapUtil mapUtil = new MapUtil(mapUpdateEvent.getMap(), getPlayerId());

        List<SnakeDirection> directions = new ArrayList<>();

        // Let's see in which directions I can move
        for (SnakeDirection direction : SnakeDirection.values()) {
            if (mapUtil.canIMoveInDirection(direction)) {
                directions.add(direction);
            }
        }

        //Lets find food
        MapCoordinate[] foodTiles = mapUtil.listCoordinatesContainingFood();
        List<MapCoordinate> foodTileList = new LinkedList();
        for(MapCoordinate mc : foodTiles){
            foodTileList.add(mc);
        }

        MapCoordinate current = mapUtil.getMyPosition();

        Collections.sort(foodTileList, new Comparator<MapCoordinate>() {
            public int compare(MapCoordinate left, MapCoordinate right) {
                return Integer.compare(left.getManhattanDistanceTo(current), right.getManhattanDistanceTo(current));
            }
        });

        SnakeDirection  chosenDirection;

        if(foodTiles.length > 0){
            //First food;

            MapCoordinate foodTile = foodTileList.get(0);
           // System.out.println("Current:      " +current);
          //  System.out.println("Food:      " +foodTile);
            ArrayList<MapCoordinate> path = astar.getPath(current, foodTile, mapUtil);
            if(path == null){
                Random r = new Random();
                chosenDirection = SnakeDirection.DOWN;
                // Choose a random direction
                if (!directions.isEmpty())
                    chosenDirection = directions.get(r.nextInt(directions.size()));


            } else {
                MapCoordinate goTo = path.get(path.size()-2);

                //System.out.println("path:      " +path);

                int dY = goTo.y - current.y;
                int dX = goTo.x - current.x;
                chosenDirection = getDirection(dX,dY);
                int i = 1;
            }


        } else {
            Random r = new Random();
            chosenDirection = SnakeDirection.DOWN;
            // Choose a random direction
            if (!directions.isEmpty())
                chosenDirection = directions.get(r.nextInt(directions.size()));
        }

        // Register action here!
        registerMove(mapUpdateEvent.getGameTick(), chosenDirection);
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
