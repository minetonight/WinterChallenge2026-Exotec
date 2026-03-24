import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Derives short display names from bot launch commands.
 *
 * <p>Examples:</p>
 * <ul>
 *   <li>{@code python3 /path/to/MyBot.py -> MyBot}</li>
 *   <li>{@code /path/to/epic4-solver-bot.exe -> epic4-solver-}</li>
 * </ul>
 */
public final class BotNameResolver {
    private static final int MAX_NAME_LENGTH = 14;
    private static final Pattern BOT_FILE_PATTERN = Pattern.compile(
        "(?:^|\\s|\"|')([^\\s\"']+?\\.(?:py|exe))(?:$|\\s|\"|')",
        Pattern.CASE_INSENSITIVE
    );

    private BotNameResolver() {
    }

    public static String resolve(String command, String fallbackName) {
        if (command == null || command.isBlank()) {
            return fallbackName;
        }

        String candidate = extractBotFileName(command);
        if (candidate == null || candidate.isBlank()) {
            return fallbackName;
        }

        int extensionSeparator = candidate.lastIndexOf('.');
        String baseName = extensionSeparator > 0 ? candidate.substring(0, extensionSeparator) : candidate;

        if (baseName.length() <= MAX_NAME_LENGTH) {
            return baseName;
        }

        return baseName.substring(0, MAX_NAME_LENGTH);
    }

    private static String extractBotFileName(String command) {
        Matcher matcher = BOT_FILE_PATTERN.matcher(command);
        String lastMatch = null;

        while (matcher.find()) {
            lastMatch = matcher.group(1);
        }

        if (lastMatch == null) {
            return null;
        }

        int slashIndex = Math.max(lastMatch.lastIndexOf('/'), lastMatch.lastIndexOf('\\'));
        return slashIndex >= 0 ? lastMatch.substring(slashIndex + 1) : lastMatch;
    }
}