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
        assertEquals("false", props.getProperty("spring.jpa.open-in-view"));
        assertEquals("false", props.getProperty("spring.jmx.enabled"));
        assertEquals("${GESTOPAGO_TOKEN_INITIAL_DELAY_MS:15000}", props.getProperty("gestopago.auth.initial-delay-ms"));
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

    @Test
    void configuraPropiedadesHibernateYDriverPostgresParaEvitarBloqueoMetadataEnRender() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:postgresql://db.ejemplo.neon.tech/neondb?sslmode=require")
                .withProperty("spring.datasource.username", "neondb_owner")
                .withProperty("spring.datasource.password", "secreto");

        ConfigDB configDB = new ConfigDB();
        ReflectionTestUtils.setField(configDB, "env", env);

        var entityManagerFactoryBean = configDB.sfEntityManagerFactory(new org.springframework.jdbc.datasource.DriverManagerDataSource());
        assertNotNull(entityManagerFactoryBean);
        Map<String, Object> jpaProps = entityManagerFactoryBean.getJpaPropertyMap();
        assertEquals(false, jpaProps.get("hibernate.boot.allow_jdbc_metadata_access"));
        assertEquals("PostgreSQL", jpaProps.get("jakarta.persistence.database-product-name"));
        assertEquals("16", jpaProps.get("jakarta.persistence.database-major-version"));
        assertEquals(true, jpaProps.get("hibernate.jdbc.lob.non_contextual_creation"));
        assertEquals(null, jpaProps.get("hibernate.dialect"));
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
