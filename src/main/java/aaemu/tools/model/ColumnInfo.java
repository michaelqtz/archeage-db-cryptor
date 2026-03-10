package aaemu.tools.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author Shannon
 */
@Data
@Accessors(fluent = true)
public class ColumnInfo {

    @JsonProperty("index")
    private String index;

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private String type;
}
