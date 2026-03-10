package aaemu.tools.util;

import static aaemu.tools.util.SqlUtils.COUNT_QUERY;
import static aaemu.tools.util.SqlUtils.findQuery;
import static java.util.Objects.nonNull;
import static org.apache.logging.log4j.util.Strings.EMPTY;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aaemu.tools.model.FunctionInfo;
import aaemu.tools.model.QueryInfo;
import lombok.experimental.UtilityClass;

/**
 * @author Shannon
 */
@UtilityClass
public class FunctionUtils {
    private static final Pattern START_FUNCTION_PATTERN = Pattern.compile("^--- .* at 0x[0-9a-fA-F]+ ---$");
    private static final Pattern FUNCTION_NAME_PATTERN = Pattern.compile("^--- (.*?) at");
    private static final String CALL_COMMAND = "call";
    private static final String CMP_COMMAND = "cmp";
    private static final String MOV_COMMAND = "mov";
    private static final String CALL_PATTERN = "^.*call\\s+";
    public static final Pattern COMMENT_PATTERN = Pattern.compile(";;\\s*FULL:\\s*\"([^\"]+)\"");

    public static Map<String, FunctionInfo> parseFunctions(List<String> lines) {
        Map<String, FunctionInfo> functions = new LinkedHashMap<>();
        FunctionInfo functionInfo = null;
        boolean inFunction = false;

        for (String line : lines) {
            line = line.trim();

            if (!inFunction) {
                if (isFunctionStart(line)) {
                    functionInfo = buildFunctionInfo(line);
                    inFunction = true;
                    continue;
                }
            }
            if (line.contains(COUNT_QUERY) && nonNull(functionInfo)) {
                functionInfo.setCountFunction(true);
            }
            if (line.isBlank() && nonNull(functionInfo)) {
                functions.put(functionInfo.getName(), functionInfo);
                inFunction = false;
                continue;
            }
            if (inFunction) {
                functionInfo.getLines().add(line);
            }
        }

        return functions;
    }

    public static List<String> collectCountFunctions(Map<String, FunctionInfo> functions) {
        List<String> countFunctions = new ArrayList<>();

        for (Map.Entry<String, FunctionInfo> entry : functions.entrySet()) {
            FunctionInfo functionInfo = entry.getValue();

            if (functionInfo.isCountFunction()) {
                countFunctions.add(entry.getKey());
            }
        }

        return countFunctions;
    }

    public static void parseCallCountFunction(List<String> functionLines, QueryInfo queryInfo, List<String> countFunctions) {
        for (String functionLine : functionLines) {
            if (!functionLine.contains(CALL_COMMAND)) {
                continue;
            }

            String callFunction = functionLine.replaceAll(CALL_PATTERN, "").trim();

            if (countFunctions.contains(callFunction)) {
                queryInfo.hasCount(true);
                break;
            }
        }
    }

    public static Map<QueryInfo, FunctionInfo> parseQueryInfo(Map<String, FunctionInfo> functions) {
        Map<QueryInfo, FunctionInfo> queryFunctions = new LinkedHashMap<>();
        String previousQuery = EMPTY;

        for (FunctionInfo functionInfo : functions.values()) {
            List<String> functionLines = functionInfo.getLines();

            for (String functionLine : functionLines) {
                Optional<QueryInfo> query = findQuery(functionLine);

                if (query.isPresent() && !previousQuery.equals(query.get().query())) {
                    queryFunctions.put(query.get(), functionInfo);
                    previousQuery = query.get().query();
                }
            }
        }

        return queryFunctions;
    }

    public static boolean isCallFunction(String functionLine) {
        return functionLine.contains(CALL_COMMAND);
    }

    public static String getCallFunctionName(String functionLine) {
        return functionLine.replaceAll(CALL_PATTERN, "").trim();
    }

    public static boolean isStartColumnInfo(String line) {
        return line.contains(CMP_COMMAND) && line.contains("64h");
    }

    public static boolean isEndColumnInfo(String line) {
        return line.contains(CMP_COMMAND) && line.contains("65h");
    }

    public static boolean isBoolean(String line) {
        return line.contains(MOV_COMMAND) && line.contains("+1Ch]");
    }

    public static boolean isNum(String line) {
        return line.contains(MOV_COMMAND) && line.contains("+20h]");
    }

    public static boolean isBlob(String line) {
        return line.contains(MOV_COMMAND) && line.contains("+28h]");
    }

    public static boolean isDouble(String line) {
        return line.contains(MOV_COMMAND) && line.contains("+30h]");
    }

    public static boolean isInt(String line) {
        return line.contains(MOV_COMMAND) && line.contains("+34h]");
    }

    public static boolean isString(String line) {
        return line.contains(MOV_COMMAND) && line.contains("+3Ch]");
    }

    public static boolean isColor(String line) {
        return line.contains(MOV_COMMAND) && line.contains("+44h]");
    }

    private static boolean isFunctionStart(String line) {
        return START_FUNCTION_PATTERN.matcher(line).matches();
    }

    private static FunctionInfo buildFunctionInfo(String line) {
        String functionName = extractFunctionName(line);

        return new FunctionInfo(functionName);
    }

    private static String extractFunctionName(String line) {
        Matcher matcher = FUNCTION_NAME_PATTERN.matcher(line);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        throw new RuntimeException("Function name not found");
    }
}
