package aaemu.tools.util;

import static aaemu.tools.util.FunctionUtils.COMMENT_PATTERN;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aaemu.tools.model.QueryInfo;
import lombok.experimental.UtilityClass;

/**
 * @author Shannon
 */
@UtilityClass
public class SqlUtils {
    private static final String SELECT_PREFIX = "SELECT";
    private static final String SELECT_COUNT_PREFIX = "SELECT COUNT(";
    private static final Pattern COUNT_PATTERN = Pattern.compile("SELECT\\s+COUNT\\s*", Pattern.CASE_INSENSITIVE);
    public static final Pattern COLUMNS_PATTERN = Pattern.compile("SELECT\\s+(.+?)\\s+FROM\\s+", Pattern.CASE_INSENSITIVE);
    public static final Pattern TABLE_NAME_PATTERN = Pattern.compile("FROM\\s+" + "([^\\s,;()]+)", Pattern.CASE_INSENSITIVE);
    public static final String COUNT_QUERY = "SELECT COUNT(*) FROM %s";

    public static Optional<QueryInfo> findQuery(String input) {
        Matcher matcher = COMMENT_PATTERN.matcher(input);

        if (matcher.find()) {
            String query = matcher.group(1);
            String str = query.toUpperCase();

            if (str.startsWith(SELECT_PREFIX) && !str.startsWith(SELECT_COUNT_PREFIX)) {
                return Optional.of(new QueryInfo(query));
            }
        }

        return Optional.empty();
    }

    public static void parseCountQuery(List<String> functionLines, QueryInfo queryInfo) {
        if (!queryInfo.hasCount()) {
            for (String functionLine : functionLines) {
                if (isCountedLine(functionLine)) {
                    queryInfo.hasCount(true);

                    break;
                }
            }
        }
    }

    private static boolean isCountedLine(String line) {
        Matcher matcher = COUNT_PATTERN.matcher(line);

        return matcher.find();
    }
}
