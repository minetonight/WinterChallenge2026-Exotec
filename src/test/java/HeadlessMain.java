import com.codingame.gameengine.runner.MultiplayerGameRunner;
import com.codingame.gameengine.runner.simulate.GameResult;

public class HeadlessMain {
    public static void main(String[] args) throws Exception {
        MultiplayerGameRunner gameRunner = new MultiplayerGameRunner();

        String bot1Command = "python3 config/Boss.py";
        String bot2Command = "python3 config/Boss.py";
        
        if (args.length > 0) {
            String combinedArgs = args[0];
            String[] parts = combinedArgs.split("\\|\\|\\|");
            if (parts.length >= 1) bot1Command = parts[0];
            if (parts.length >= 2) bot2Command = parts[1];
        }

        gameRunner.addAgent(bot1Command, "Player 1");
        gameRunner.addAgent(bot2Command, "Player 2");

        GameResult result = gameRunner.simulate();

        System.out.println("===GAME_RESULT===");
        System.out.println("Player 1 Score: " + result.scores.get(0));
        System.out.println("Player 2 Score: " + result.scores.get(1));
        System.out.println("Player 1 Errors: " + (result.errors.get(0) != null ? result.errors.get(0).size() : 0));
        System.out.println("Player 2 Errors: " + (result.errors.get(1) != null ? result.errors.get(1).size() : 0));
        System.out.println("===GAME_RESULT_END===");
        
        System.exit(0);
    }
}
