import com.codingame.gameengine.runner.MultiplayerGameRunner;
import com.codingame.gameengine.runner.simulate.GameResult;

public class HeadlessMain {
    public static void main(String[] args) throws Exception {
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

        gameRunner.addAgent(bot1Command, "Player 1");
        gameRunner.addAgent(bot2Command, "Player 2");

        GameResult result = gameRunner.simulate();

        System.out.println("===GAME_RESULT===");
        System.out.println("Player 1 Score: " + result.scores.get(0));
        System.out.println("Player 2 Score: " + result.scores.get(1));
        System.out.println("Player 1 Errors: " + (result.errors.get(0) != null ? result.errors.get(0).size() : 0));
        System.out.println("Player 2 Errors: " + (result.errors.get(1) != null ? result.errors.get(1).size() : 0));
        if (result.errors.get(0) != null && !result.errors.get(0).isEmpty()) {
            System.out.println("--- Player 1 Error Details ---");
            for (String err : result.errors.get(0)) {
                System.out.println(err);
            }
        }
        if (result.errors.get(1) != null && !result.errors.get(1).isEmpty()) {
            System.out.println("--- Player 2 Error Details ---");
            for (String err : result.errors.get(1)) {
                System.out.println(err);
            }
        }
        System.out.println("===GAME_RESULT_END===");
        
        System.exit(0);
    }
}
