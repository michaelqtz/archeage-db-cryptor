package aaemu.tools.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * @author Shannon
 */
@Data
public class FunctionInfo {
    private final String name;
    private List<String> lines;
    private boolean countFunction;

    public FunctionInfo(String name) {
        this.name = name;
        this.lines = new ArrayList<>();
    }
}
