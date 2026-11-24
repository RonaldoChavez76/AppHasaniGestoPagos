package com.proyecto.servicios.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = {
                "com.proyecto.servicios.repositorys.gestopago",
                "com.proyecto.servicios.repositorys.onboarding",
                "com.proyecto.servicios.repositorys.seguridad"
        },
        transactionManagerRef = "sfTransactionManager",
        entityManagerFactoryRef = "sfEntityManagerFactory"
)
public class ConfigDB {
    @Autowired
    private Environment env;

    @Bean(name="sfDatasource")
    public DataSource sfDatasource(){
        HikariConfig config=new HikariConfig();
        try{
            String jdbcUrl = env.getProperty("spring.datasource.url");
            config.setJdbcUrl(jdbcUrl);
            config.setPassword(env.getProperty("spring.datasource.password"));
            config.setUsername(env.getProperty("spring.datasource.username"));
            config.setMaximumPoolSize(env.getProperty("app.db.pool.max-size", Integer.class, 5));
            config.setMaxLifetime(env.getProperty("app.db.pool.max-lifetime-ms", Long.class, 600000L));
            config.setIdleTimeout(env.getProperty("app.db.pool.idle-timeout-ms", Long.class, 300000L));
            config.setConnectionTimeout(env.getProperty("app.db.pool.connection-timeout-ms", Long.class, 15000L));
            config.setValidationTimeout(env.getProperty("app.db.pool.validation-timeout-ms", Long.class, 5000L));
            config.setMinimumIdle(env.getProperty("app.db.pool.min-idle", Integer.class, 1));
            config.setConnectionTestQuery("SELECT 1");
            config.setPoolName("sfDatasource");
            if (jdbcUrl != null && jdbcUrl.startsWith("jdbc:postgresql:")) {
                config.addDataSourceProperty("connectTimeout", env.getProperty("app.db.pg.connect-timeout-s", "10"));
                config.addDataSourceProperty("socketTimeout", env.getProperty("app.db.pg.socket-timeout-s", "30"));
                config.addDataSourceProperty("loginTimeout", env.getProperty("app.db.pg.login-timeout-s", "10"));
                config.addDataSourceProperty("tcpKeepAlive", "true");
            }

        }catch (Exception e){
            log.error("Ha ocurrido un error en la conexión a la base de datos, a causa de:", e);
            throw new IllegalStateException("Error al inicializar sfDatasource", e);
        }
        return new HikariDataSource(config);
    }

    @Bean(name="sfEntityManagerFactory")
    @DependsOn("flyway")
    public LocalContainerEntityManagerFactoryBean sfEntityManagerFactory(@Qualifier("sfDatasource") DataSource dataSource){
        LocalContainerEntityManagerFactoryBean em= new LocalContainerEntityManagerFactoryBean();
        try{
          em.setDataSource(dataSource);
          em.setPackagesToScan(
                  "com.proyecto.servicios.entity.gestopago",
                  "com.proyecto.servicios.entity.onboarding",
                  "com.proyecto.servicios.entity.seguridad"
          );
          em.setPersistenceUnitName("sfDatasource");
            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            em.setJpaVendorAdapter(vendorAdapter);
            String jdbcUrl = env.getProperty("spring.datasource.url", "");
            Map<String, Object> properties = new HashMap<>();
            properties.put("hibernate.hbm2ddl.auto", "none");
            properties.put("hibernate.show-sql", false);
            properties.put("jakarta.persistence.query.timeout", 600000);
            if (jdbcUrl.startsWith("jdbc:postgresql:")) {
                properties.put("hibernate.boot.allow_jdbc_metadata_access", false);
                properties.put("jakarta.persistence.database-product-name", "PostgreSQL");
                properties.put("jakarta.persistence.database-major-version", "16");
                properties.put("hibernate.jdbc.lob.non_contextual_creation", true);
            } else {
                properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            }
            em.setJpaPropertyMap(properties);


        } catch (Exception e) {
            log.error("Ha ocurrido un error en la conexion a base de datos, a causa de:",e);
            return null;

        }
        return em;
    }
 @Bean(name="sfTransactionManager")
 public PlatformTransactionManager sfTransactionManager(@Qualifier("sfEntityManagerFactory") EntityManagerFactory sfEntityManagerFactory){
        return new JpaTransactionManager(sfEntityManagerFactory);

 }

}
