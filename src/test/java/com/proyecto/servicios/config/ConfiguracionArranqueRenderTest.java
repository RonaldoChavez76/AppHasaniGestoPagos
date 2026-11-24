package com.proyecto.servicios.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.net.URI;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConfiguracionArranqueRenderTest {

    @Test
    void definePropiedadesDeArranqueYActuatorEnApplicationProperties() throws Exception {
        Properties props = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"));

        assertEquals("${SPRING_CLOUD_CONFIG_ENABLED:false}", props.getProperty("spring.cloud.config.enabled"));
        assertEquals("false", props.getProperty("spring.data.redis.repositories.enabled"));
        assertEquals("${HEALTH_SHOW_COMPONENTS:always}", props.getProperty("management.endpoint.health.show-components"));
        assertEquals("false", props.getProperty("management.health.config.enabled"));
    }

    @Test
    void resuelveRedisUrlPorDefectoSinVariablesDeEntorno() throws Exception {
        RedisProperties redis = bindRedisProperties(Map.of());

        assertEquals("redis://localhost:6379", redis.getUrl());
        URI uri = URI.create(redis.getUrl());
        assertEquals("redis", uri.getScheme());
        assertEquals("localhost", uri.getHost());
        assertEquals(6379, uri.getPort());
    }

    @Test
    void resuelveRedisUrlDesdeVariableRedisUrl() throws Exception {
        RedisProperties redis = bindRedisProperties(Map.of("REDIS_URL", "redis://red-test:6379"));

        assertEquals("redis://red-test:6379", redis.getUrl());
    }

    @Test
    void priorizaSpringDataRedisUrlSobreRedisUrl() throws Exception {
        RedisProperties redis = bindRedisProperties(Map.of(
                "SPRING_DATA_REDIS_URL", "rediss://prioridad:6379",
                "REDIS_URL", "redis://secundario:6379"
        ));

        assertEquals("rediss://prioridad:6379", redis.getUrl());
    }

    @Test
    void configuraPoolHikariPorDefectoParaNubeEnConfigDB() throws Exception {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:h2:mem:configdbtest;DB_CLOSE_DELAY=-1")
                .withProperty("spring.datasource.username", "sa")
                .withProperty("spring.datasource.password", "");

        ConfigDB configDB = new ConfigDB();
        ReflectionTestUtils.setField(configDB, "env", env);

        DataSource dataSource = configDB.sfDatasource();
        try {
            HikariDataSource hikari = assertInstanceOf(HikariDataSource.class, dataSource);
            assertEquals(5, hikari.getMaximumPoolSize());
            assertEquals(1, hikari.getMinimumIdle());
            assertEquals(15000L, hikari.getConnectionTimeout());
            assertEquals(300000L, hikari.getIdleTimeout());
            assertEquals(600000L, hikari.getMaxLifetime());
        } finally {
            if (dataSource instanceof AutoCloseable closeable) {
                closeable.close();
            }
        }
    }

    private RedisProperties bindRedisProperties(Map<String, Object> envVars) throws Exception {
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        env.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        env.getPropertySources().addFirst(new SystemEnvironmentPropertySource("systemEnvironment", envVars));
        env.getPropertySources().addLast(new ResourcePropertySource("classpath:application.properties"));

        RedisProperties redis = Binder.get(env)
                .bind("spring.data.redis", RedisProperties.class)
                .orElse(null);
        assertNotNull(redis);
        return redis;
    }
}
