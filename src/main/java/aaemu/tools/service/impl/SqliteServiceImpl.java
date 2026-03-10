package aaemu.tools.service.impl;

import static aaemu.tools.util.FunctionUtils.collectCountFunctions;
import static aaemu.tools.util.FunctionUtils.getCallFunctionName;
import static aaemu.tools.util.FunctionUtils.isBlob;
import static aaemu.tools.util.FunctionUtils.isBoolean;
import static aaemu.tools.util.FunctionUtils.isCallFunction;
import static aaemu.tools.util.FunctionUtils.isColor;
import static aaemu.tools.util.FunctionUtils.isDouble;
import static aaemu.tools.util.FunctionUtils.isEndColumnInfo;
import static aaemu.tools.util.FunctionUtils.isInt;
import static aaemu.tools.util.FunctionUtils.isNum;
import static aaemu.tools.util.FunctionUtils.isStartColumnInfo;
import static aaemu.tools.util.FunctionUtils.isString;
import static aaemu.tools.util.FunctionUtils.parseCallCountFunction;
import static aaemu.tools.util.FunctionUtils.parseFunctions;
import static aaemu.tools.util.FunctionUtils.parseQueryInfo;
import static aaemu.tools.util.SqlUtils.parseCountQuery;
import static org.apache.logging.log4j.util.Strings.isBlank;

import java.util.List;
import java.util.Map;

import aaemu.tools.model.ColumnInfo;
import aaemu.tools.model.FunctionInfo;
import aaemu.tools.model.QueryInfo;
import aaemu.tools.service.FileService;
import aaemu.tools.service.SqliteService;
import lombok.RequiredArgsConstructor;

/**
 * @author Shannon
 */
@RequiredArgsConstructor
public class SqliteServiceImpl implements SqliteService {
    private final FileService fileService;

    @Override
    public void createSchema() throws Exception {
        List<String> allLines = fileService.readFunctionsFile();
        Map<String, FunctionInfo> functions = parseFunctions(allLines);
        List<String> countFunctions = collectCountFunctions(functions);
        Map<QueryInfo, FunctionInfo> queryFunctions = parseQueryInfo(functions);

        System.out.println("=== SQL queries ===");

        for (Map.Entry<QueryInfo, FunctionInfo> entry : queryFunctions.entrySet()) {
            QueryInfo queryInfo = entry.getKey();
            System.out.printf("func: %s, table: %s, query: %s%n", entry.getValue().getName(), queryInfo.table(), queryInfo.query());
        }

        System.out.println("\nTotal queries: " + queryFunctions.size());

        for (Map.Entry<QueryInfo, FunctionInfo> entry : queryFunctions.entrySet()) {
            QueryInfo queryInfo = entry.getKey();
            FunctionInfo functionInfo = entry.getValue();
            List<String> functionLines = functionInfo.getLines();
            parseCallCountFunction(functionLines, queryInfo, countFunctions);
            parseCountQuery(functionLines, queryInfo);
            boolean inColumnInfo = false;

            for (String functionLine : functionLines) {
                if (isStartColumnInfo(functionLine)) {
                    inColumnInfo = true;

                    continue;
                }
                if (inColumnInfo) {
                    if (isCallFunction(functionLine)) {
                        String callFunctionName = getCallFunctionName(functionLine);

                        if (!functions.containsKey(callFunctionName)) {
                            continue;
                        }

                        FunctionInfo callFunction = functions.get(callFunctionName);

                        for (String line : callFunction.getLines()) {
                            findType(line, queryInfo.columnsInfo());
                        }

                        continue;
                    }
                    findType(functionLine, queryInfo.columnsInfo());
                }
                if (isEndColumnInfo(functionLine)) {
                    break;
                }
            }
        }

        for (Map.Entry<QueryInfo, FunctionInfo> entry : queryFunctions.entrySet()) {
            QueryInfo queryInfo = entry.getKey();
            List<ColumnInfo> columnInfos = queryInfo.columnsInfo();
            boolean needQueryUpdate = false;

            for (int i = 0; i < columnInfos.size(); i++) {
                if (isBlank(columnInfos.get(i).type())) {
                    System.out.println("Unknown column type at index: " + i);
                    needQueryUpdate = true;
                }
            }

            if (needQueryUpdate) {
                FunctionInfo functionInfo = entry.getValue();
                System.out.printf("Unknown column type(s) in: %s, query: %s%n", functionInfo.getName(), queryInfo.query());
            }
        }

        fileService.writeDbShema(queryFunctions.keySet());
    }

    private void findType(String functionLine, List<ColumnInfo> columnsInfo) {
        for (ColumnInfo columnInfo : columnsInfo) {
            if (isBlank(columnInfo.type())) {
                if (isBoolean(functionLine)) {
                    columnInfo.type("boolean");
                    break;
                } else if (isNum(functionLine)) {
                    columnInfo.type("num");
                    break;
                } else if (isBlob(functionLine)) {
                    columnInfo.type("blob");
                    break;
                } else if (isDouble(functionLine)) {
                    columnInfo.type("double");
                    break;
                } else if (isInt(functionLine)) {
                    columnInfo.type("int");
                    break;
                } else if (isString(functionLine)) {
                    columnInfo.type("string");
                    break;
                } else if (isColor(functionLine)) {
                    columnInfo.type("color");
                    break;
                }
            }
        }
    }
}
