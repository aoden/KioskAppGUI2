package com.tdt.kioskapp.config;

import com.tdt.kioskapp.Application;
import com.tdt.kioskapp.service.BaseService;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * @author aoden
 */
@Configuration
@ComponentScan(basePackages = "com.tdt.kioskapp")
@EnableJpaRepositories(basePackages = "com.tdt.kioskapp.repository", entityManagerFactoryRef = "entityManagerFactoryBean")
public class AppConfig {

    @Bean
    public JpaTransactionManager transactionManager() throws URISyntaxException, UnsupportedEncodingException {

        JpaTransactionManager manager = new JpaTransactionManager();
        manager.setEntityManagerFactory(entityManagerFactoryBean().getObject());
        return manager;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactoryBean() throws URISyntaxException, UnsupportedEncodingException {

        LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setDataSource(dataSource());
        factoryBean.setJpaDialect(new HibernateJpaDialect());
        factoryBean.setPackagesToScan("com.tdt.kioskapp.model");
        factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        Properties properties = new Properties();
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.dialect", "com.tdt.kioskapp.repository.SQLiteDialect");
        factoryBean.setJpaProperties(properties);
        return factoryBean;
    }

    @Bean
    javax.sql.DataSource dataSource() throws URISyntaxException, UnsupportedEncodingException {

        String path = Application.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String decodedPath = URLDecoder.decode(path, "UTF-8");
        if (decodedPath.endsWith("/")) {
            decodedPath = decodedPath.substring(0, decodedPath.length() - 1);
        }
        DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource();
        driverManagerDataSource.setDriverClassName("org.sqlite.JDBC");
        driverManagerDataSource.setUrl("jdbc:sqlite:" + BaseService.reformatPath(decodedPath + "/../db.db"));
        return driverManagerDataSource;
    }

    @Bean
    public RestTemplate restTemplate() {

        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        messageConverters.add(new MappingJackson2HttpMessageConverter());
        ByteArrayHttpMessageConverter converter = new ByteArrayHttpMessageConverter();
        converter.setSupportedMediaTypes(Arrays.asList(MediaType.ALL));
        messageConverters.add(converter);
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(messageConverters);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        // this is setting for reading zip file timeout
        requestFactory.setReadTimeout(10000);
        restTemplate.setRequestFactory(requestFactory);
        return restTemplate;
    }

    @Bean
    public PropertyPlaceholderConfigurer propertyPlaceholderConfigurer() {

        PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
        configurer.setLocation(new ClassPathResource("app_config.properties"));
        return configurer;
    }
}
