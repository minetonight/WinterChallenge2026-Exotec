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
        System.out.println("FailCause: " + result.failCause);
        System.out.println("ScoresMap: " + result.scores);
        System.out.println("Agents: " + result.agents);
        System.out.println("Outputs keys: " + result.outputs.keySet());
        System.out.println("Errors keys: " + result.errors.keySet());
        for (String key : result.outputs.keySet()) {
            java.util.List<String> vals = result.outputs.get(key);
            if (vals != null && !vals.isEmpty()) {
                System.out.println("--- Output tail key=" + key + " ---");
                int start = Math.max(0, vals.size() - 10);
                for (int i = start; i < vals.size(); i++) {
                    System.out.println(vals.get(i));
                }
            }
        }
        for (String key : result.errors.keySet()) {
            java.util.List<String> vals = result.errors.get(key);
            if (vals != null && !vals.isEmpty()) {
                System.out.println("--- Error tail key=" + key + " ---");
                int start = Math.max(0, vals.size() - 10);
                for (int i = start; i < vals.size(); i++) {
                    System.out.println(vals.get(i));
                }
            }
        }
        if (result.summaries != null && !result.summaries.isEmpty()) {
            System.out.println("--- Summaries tail ---");
            int start = Math.max(0, result.summaries.size() - 20);
            for (int i = start; i < result.summaries.size(); i++) {
                System.out.println(result.summaries.get(i));
            }
        }
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
