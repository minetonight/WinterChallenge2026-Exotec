import com.codingame.gameengine.runner.MultiplayerGameRunner;

import java.lang.reflect.Method;

public class Main {
    public static void main(String[] args) {

        MultiplayerGameRunner gameRunner = new MultiplayerGameRunner();

        // Set seed here (leave commented for random)
        // gameRunner.setSeed(-1566415677164768800L);

        String bot1Command = "python3 config/Boss.py";
        String bot2Command = "python3 config/Boss.py";

        if (args.length > 0) {
            String combinedArgs = args[0];
            String[] parts = combinedArgs.split("\\|\\|\\|");
            if (parts.length >= 1 && !parts[0].isEmpty()) bot1Command = parts[0];
            if (parts.length >= 2 && !parts[1].isEmpty()) bot2Command = parts[1];
            if (parts.length >= 3 && !parts[2].isEmpty()) {
                gameRunner.setSeed(Long.parseLong(parts[2]));
            }
        }

        // Select agents here
        gameRunner.addAgent(bot1Command, BotNameResolver.resolve(bot1Command, "Player 1"));
        gameRunner.addAgent(bot2Command, BotNameResolver.resolve(bot2Command, "Player 2"));

        gameRunner.simulate();

        String gameJson = extractGameJson(gameRunner);
        LocalViewerServer.start(gameJson, 8888);
    }

    private static String extractGameJson(MultiplayerGameRunner gameRunner) {
        try {
            Method getJsonResult = Class
                .forName("com.codingame.gameengine.runner.GameRunner")
                .getDeclaredMethod("getJSONResult");
            getJsonResult.setAccessible(true);
            return (String) getJsonResult.invoke(gameRunner);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to extract viewer game JSON", exception);
        }
    }
}