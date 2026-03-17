package com.codingame.game;

import com.codingame.gameengine.core.AbstractPlayer.TimeoutException;
import com.codingame.gameengine.core.AbstractReferee;
import com.codingame.gameengine.core.MultiplayerGameManager;
import com.codingame.view.ViewModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class Referee extends AbstractReferee {

    @Inject private MultiplayerGameManager<Player> gameManager;
    @Inject private CommandManager commandManager;
    @Inject private Game game;
    @Inject private ViewModule viewModule;

    @Override
    public void init() {
        try {
            gameManager.setMaxTurns(200);
            gameManager.setFirstTurnMaxTime(1000);

            game.init();
            sendGlobalInfo();

            gameManager.setFrameDuration(1000);
            gameManager.setTurnMaxTime(50);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Referee failed to initialize");
            abort();
        }
    }

    private void abort() {
        gameManager.endGame();

    }

    private void sendGlobalInfo() {
        // Give input to players
        for (Player player : gameManager.getActivePlayers()) {
            for (String line : Serializer.serializeGlobalInfoFor(player, game)) {
                player.sendInputLine(line);
            }
        }
    }

    @Override
    public void gameTurn(int turn) {
        game.resetGameTurnData();

        System.out.println("DEBUG_TURN_BEGIN turn=" + turn);

        // Give input to players
        for (Player player : gameManager.getActivePlayers()) {
            if (game.shouldSkipPlayerTurn(player)) {
                continue;
            }
            System.out.println(
                "DEBUG_BEFORE_EXEC turn=" + turn
                    + " player=" + player.getIndex()
                    + " active=" + player.isActive()
            );
            for (String line : Serializer.serializeFrameInfoFor(player, game)) {
                player.sendInputLine(line);
            }
            player.execute();
            System.out.println(
                "DEBUG_AFTER_EXEC turn=" + turn
                    + " player=" + player.getIndex()
                    + " active=" + player.isActive()
                    + " execMs=" + player.getLastExectionTimeMs()
            );
        }
        // Get output from players
        handlePlayerCommands(turn);

        game.performGameUpdate(turn);

        for (Player player : game.players) {
            System.out.println(
                "DEBUG_AFTER_UPDATE turn=" + turn
                    + " player=" + player.getIndex()
                    + " active=" + player.isActive()
                    + " score=" + player.getScore()
                    + " liveBirds=" + player.birds.stream().filter(Bird::isAlive).count()
            );
        }

        if (gameManager.getActivePlayers().size() < 2) {
            System.out.println("DEBUG_ABORT turn=" + turn + " activePlayers=" + gameManager.getActivePlayers().size());
            abort();
        }
    }

    private void handlePlayerCommands(int turn) {
        for (Player player : gameManager.getActivePlayers()) {
            if (game.shouldSkipPlayerTurn(player)) {
                continue;
            }
            try {
                System.out.println(
                    "DEBUG_PARSE_BEGIN turn=" + turn
                        + " player=" + player.getIndex()
                        + " outputs=" + player.getOutputs().size()
                );
                commandManager.parseCommands(player, player.getOutputs());
                System.out.println(
                    "DEBUG_PARSE_END turn=" + turn
                        + " player=" + player.getIndex()
                        + " active=" + player.isActive()
                );
            } catch (TimeoutException e) {
                System.out.println("DEBUG_TIMEOUT player=" + player.getIndex() + " turn=" + turn);
                player.deactivate("Timeout!");
                gameManager.addToGameSummary(player.getNicknameToken() + " has not provided " + player.getExpectedOutputLines() + " lines in time");
            }
        }

    }

    @Override
    public void onEnd() {
        game.onEnd();
    }
}
