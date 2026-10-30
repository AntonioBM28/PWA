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
                "com.proyecto.servicios.repositorys.onboarding"
        },
        transactionManagerRef = "sfTransactionManager",
        entityManagerFactoryRef = "sfEntityManagerFactory"
)
public class ConfigDB {
    @Autowired
    private Environment env;

    @Bean(name="sfDatasource")
    public DataSource sfDatasource(){
        String jdbcUrl = propiedadRequerida("spring.datasource.url", "SPRING_DATASOURCE_URL");
        String username = propiedadRequerida("spring.datasource.username", "SPRING_DATASOURCE_USERNAME");
        String password = propiedadRequerida("spring.datasource.password", "SPRING_DATASOURCE_PASSWORD");
        HikariConfig config=new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setPassword(password);
        config.setUsername(username);
        config.setMaximumPoolSize(10);
        config.setMaxLifetime(1800000);
        config.setConnectionTimeout(5000);
        config.setValidationTimeout(5000);
        config.setMinimumIdle(2);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("sfDatasource");
        return new HikariDataSource(config);
    }

    private String propiedadRequerida(String property, String variable) {
        String value = env.getProperty(property);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta configurar la variable de entorno " + variable);
        }
        return value;
    }

    @Bean(name="sfEntityManagerFactory")
    @DependsOn("flyway")
    public LocalContainerEntityManagerFactoryBean sfEntityManagerFactory(){
        LocalContainerEntityManagerFactoryBean em= new LocalContainerEntityManagerFactoryBean();
        try{
          em.setDataSource(sfDatasource());
          em.setPackagesToScan(
                  "com.proyecto.servicios.entity.gestopago",
                  "com.proyecto.servicios.entity.onboarding"
          );
          em.setPersistenceUnitName("sfDatasource");
            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            em.setJpaVendorAdapter(vendorAdapter);
          Map<String, Object> properties=new HashMap<>();
          // Flyway crea el esquema; Hibernate solo valida que las entidades coincidan con el.
          properties.put("hibernate.hbm2ddl.auto", "validate");
            properties.put("hibernate.show_sql", false);
            properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            properties.put("jakarta.persistence.query.timeout", 600000);
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
