package aaemu.tools.service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import aaemu.tools.config.ConfigProperties;
import aaemu.tools.model.QueryInfo;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Shannon
 */
public interface FileService {

    ConfigProperties readConfigProperties() throws IOException;

    List<ConfigProperties> readConfigPropertiesList() throws IOException;

    ByteBuffer readFile(String name) throws IOException;

    List<String> readAllLines(String name) throws IOException;

    void writeFile(String name, ByteBuffer buffer) throws IOException;

    void writeFile(String name, byte[] bytes) throws IOException;

    void writeJson(String name, Object object) throws IOException;

    ObjectNode readJson(Path path) throws IOException;

    List<String> readFunctionsFile() throws IOException;

    void writeDbShema(Set<QueryInfo> queryInfos) throws IOException;
}
