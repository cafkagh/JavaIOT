package com.spike.mercury.config;

import com.alibaba.druid.filter.Filter;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.wall.WallConfig;
import com.alibaba.druid.wall.WallFilter;
// import org.beetl.core.resource.WebAppResourceLoader;
// import org.beetl.ext.spring.BeetlGroupUtilConfiguration;
// import org.beetl.ext.spring.BeetlSpringViewResolver;
import org.beetl.sql.core.ClasspathLoader;
import org.beetl.sql.core.DefaultNameConversion;
import org.beetl.sql.core.Interceptor;
import org.beetl.sql.core.db.MySqlStyle;
import org.beetl.sql.ext.DebugInterceptor;
import org.beetl.sql.ext.spring4.BeetlSqlDataSource;
import org.beetl.sql.ext.spring4.BeetlSqlScannerConfigurer;
import org.beetl.sql.ext.spring4.SqlManagerFactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
// import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
// import org.springframework.core.io.DefaultResourceLoader;
// import org.springframework.core.io.support.ResourcePatternResolver;
// import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})
public class BeetlSqlConfig {
    //配置包扫描
    @Bean(name = "beetlSqlScannerConfigurer")
    public BeetlSqlScannerConfigurer getBeetlSqlScannerConfigurer() {
        BeetlSqlScannerConfigurer conf = new BeetlSqlScannerConfigurer();
        conf.setBasePackage("com.spike.mercury.dao");
        conf.setDaoSuffix("Dao");
        conf.setSqlManagerFactoryBeanName("sqlManagerFactoryBean");
        return conf;
    }

    @Bean(name = "sqlManagerFactoryBean")
    @Primary
    public SqlManagerFactoryBean getSqlManagerFactoryBean(@Qualifier("datasource") DataSource datasource) {
        SqlManagerFactoryBean factory = new SqlManagerFactoryBean();
        BeetlSqlDataSource source = new BeetlSqlDataSource();
        source.setMasterSource(datasource);
        factory.setCs(source);
        factory.setDbStyle(new MySqlStyle());
        factory.setInterceptors(new Interceptor[]{new DebugInterceptor()});
        factory.setNc(new DefaultNameConversion());
        factory.setSqlLoader(new ClasspathLoader("/sql"));
        //sql文件路径
        return factory;
    }

    //配置数据库
    @Bean(name = "datasource")
    public DataSource getDataSource(Environment env) {
        // String url = env.getProperty("spring.datasource.url");
        // String userName = env.getProperty("spring.datasource.username");
        // String password = env.getProperty("spring.datasource.password");
        // return DataSourceBuilder.create().url(url).username(userName).password(password).build();

        DruidDataSource dataSource = new DruidDataSource();

        dataSource.setUrl(env.getProperty("spring.datasource.url"));
        dataSource.setUsername(env.getProperty("spring.datasource.username"));
        dataSource.setPassword(env.getProperty("spring.datasource.password"));

        dataSource.setDriverClassName(env.getProperty("spring.datasource.driver-class-name"));
        dataSource.setInitialSize(Integer.parseInt(env.getProperty("spring.datasource.initialSize")));
        dataSource.setMinIdle(Integer.parseInt(env.getProperty("spring.datasource.minIdle")));
        dataSource.setMaxActive(Integer.parseInt(env.getProperty("spring.datasource.maxActive")));
        dataSource.setMaxWait(Integer.parseInt(env.getProperty("spring.datasource.maxWait")));

        // 配置间隔多久才进行一次检测，检测需要关闭的空闲连接，单位是毫秒
        dataSource.setTimeBetweenEvictionRunsMillis(Long.parseLong(env.getProperty("spring.datasource.timeBetweenEvictionRunsMillis")));

        // 配置一个连接在池中最小生存的时间，单位是毫秒
        dataSource.setMinEvictableIdleTimeMillis(Long.parseLong(env.getProperty("spring.datasource.minEvictableIdleTimeMillis")));
        dataSource.setValidationQuery(env.getProperty("spring.datasource.validationQuery"));
        dataSource.setTestWhileIdle(Boolean.valueOf(env.getProperty("spring.datasource.testWhileIdle")));
        dataSource.setTestOnBorrow(Boolean.valueOf(env.getProperty("spring.datasource.testOnBorrow")));
        dataSource.setTestOnReturn(Boolean.valueOf(env.getProperty("spring.datasource.testOnReturn")));

        // 打开PSCache，并且指定每个连接上PSCache的大小
        dataSource.setPoolPreparedStatements(Boolean.valueOf(env.getProperty("spring.datasource.poolPreparedStatements")));
        dataSource.setMaxPoolPreparedStatementPerConnectionSize(Integer.parseInt(env.getProperty("spring.datasource.maxOpenPreparedStatements")));

        try {
            List<Filter> proxyFilters = new ArrayList<>();
            WallFilter statFilter = new WallFilter();
            WallConfig config = new WallConfig();
            // 批量操作
            config.setMultiStatementAllow(Boolean.TRUE);
            statFilter.setConfig(config);
            proxyFilters.add(statFilter);
            dataSource.setProxyFilters(proxyFilters);
            dataSource.setFilters(env.getProperty("spring.datasource.filters"));
        } catch (SQLException e) {
            e.printStackTrace();
        }

        dataSource.setConnectionProperties("druid.stat.mergeSql=true;druid.stat.slowSqlMillis=5000");
        return dataSource;

    }

    //开启事务
    @Bean(name = "transactionManager")
    public DataSourceTransactionManager getDataSourceTransactionManager(@Qualifier("datasource") DataSource datasource) {
        DataSourceTransactionManager dsm = new DataSourceTransactionManager();
        dsm.setDataSource(datasource);
        return dsm;
    }
}
