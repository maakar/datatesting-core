package org.qamethod.datatesting.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * Generic YAML config loader with explicit-path, local-file, and classpath resolution.
 */
public final class YamlConfigLoader {

    private static final Logger logger = Logger.getLogger(YamlConfigLoader.class.getName());

    private YamlConfigLoader() {}

    /**
     * Load a YAML config into the given class.
     */
    public static <T> T load(String explicitPath, String defaultFilename, Class<T> configClass) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        String classpathResource = "config/" + defaultFilename;

        if (explicitPath != null && !explicitPath.isBlank()) {
            Path p = Paths.get(explicitPath);
            if (!Files.exists(p)) {
                throw new IOException("Config file not found: " + p.toAbsolutePath());
            }
            logger.info("Loading config from explicit path: " + p.toAbsolutePath());
            return mapper.readValue(p.toFile(), configClass);
        }

        Path localFile = Paths.get(defaultFilename);
        if (Files.exists(localFile)) {
            logger.info("Loading config from working directory: " + localFile.toAbsolutePath());
            return mapper.readValue(localFile.toFile(), configClass);
        }

        try (InputStream is = YamlConfigLoader.class.getClassLoader().getResourceAsStream(classpathResource)) {
            if (is != null) {
                logger.info("Loading config from classpath: " + classpathResource);
                return mapper.readValue(is, configClass);
            }
        }

        throw new IOException(
                "No config file found. Searched:\n"
                        + "  1. --config <path>  (not provided)\n"
                        + "  2. ./" + defaultFilename + "  (not found)\n"
                        + "  3. classpath:" + classpathResource + "  (not found)\n"
                        + "Create one of the above or pass --config.");
    }
}

