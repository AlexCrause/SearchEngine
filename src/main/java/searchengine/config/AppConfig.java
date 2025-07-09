package searchengine.config;

import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private static final Properties props = new Properties();

    static {
        try (InputStream input = AppConfig.class
                .getClassLoader()
                .getResourceAsStream("config.properties")){

            if (input == null) {
                throw new RuntimeException("Конфигурационный файл не найден");
            }
            props.load(input);

        } catch (Exception e) {
            throw new RuntimeException("Ошибка загрузки конфигурации", e);
        }
    }

    public static String getUserAgent() {
        return props.getProperty("user_agent");
    }

    public static String getReferrer() {
        return props.getProperty("referrer");
    }
}
