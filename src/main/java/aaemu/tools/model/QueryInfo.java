package aaemu.tools.model;

import static aaemu.tools.util.SqlUtils.COLUMNS_PATTERN;
import static aaemu.tools.util.SqlUtils.TABLE_NAME_PATTERN;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author Shannon
 */
@Data
@Accessors(fluent = true)
public class QueryInfo {

    @JsonProperty("query")
    private final String query;

    @JsonProperty("table")
    private String table;

    @JsonProperty("has_count")
    private boolean hasCount;

    @JsonProperty("columns_info")
    private List<ColumnInfo> columnsInfo;

    public QueryInfo(String query) {
        this.query = query;
        this.table = extractTableName();
        this.columnsInfo = initColumnsInfo();
    }

    private String extractTableName() {
        Matcher matcher = TABLE_NAME_PATTERN.matcher(query);

        if (matcher.find()) {
            return matcher.group(1);
        }

        throw new RuntimeException("Table name not found");
    }

    private List<ColumnInfo> initColumnsInfo() {
        List<ColumnInfo> infos = new ArrayList<>();
        String query = this.query.replaceAll("\\s+", " ").trim();
        Matcher matcher = COLUMNS_PATTERN.matcher(query);

        if (matcher.find()) {
            String columnsPart = matcher.group(1);
            String[] columnArray = columnsPart.split(",");

            for (int i = 0; i < columnArray.length; i++) {
                String column = columnArray[i].trim();

                ColumnInfo columnInfo = new ColumnInfo()
                    .index(String.valueOf(i))
                    .name(column);

                infos.add(columnInfo);
            }
        }

        return infos;
    }
}
