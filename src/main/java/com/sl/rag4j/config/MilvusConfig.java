package com.sl.rag4j.config;

import io.milvus.client.MilvusClient;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class MilvusConfig {

    @Value("${milvus.host:localhost}")
    private String milvusHost;

    @Value("${milvus.port:19530}")
    private int milvusPort;

    @Value("${milvus.username:root}")
    private String userName;

    @Value("${milvus.password:Milvus}")
    private String password;

    /**
     * 1. 标准远程连接 Bean
     * 仅在配置文件中设置 milvus.mode=remote (或未设置) 时生效
     */
    @Bean
    public MilvusClient remoteMilvusClient() {
        ConnectParam.Builder builder = ConnectParam.newBuilder()
                .withHost(milvusHost)
                .withPort(milvusPort);

        // 如果有用户名密码则添加鉴权
        if (StringUtils.hasText(userName)) {
            builder.withAuthorization(userName, password);
        }

        return new MilvusServiceClient(builder.build());
    }

      /**
     * 3. 静态工厂方法：支持传入自定义参数创建客户端
     * 适用于需要在运行时动态决定连接哪个库或维度的场景
     *
     * @param databaseName 向量数据库名 (Milvus 2.4+ 支持多数据库)
     * @param dimension    维度 (虽然不直接影响连接，但可用于后续业务逻辑校验)
     * @return MilvusClient 实例
     */
    public static MilvusClient createMilvusClient(String host, int port, String username, String password, String databaseName, int dimension) {
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port)
                .withAuthorization(username, password)
                .withDatabaseName(databaseName) // 指定具体的数据库名
                .build();

        // 注意：这里直接new出来，不由Spring管理生命周期
        return new MilvusServiceClient(connectParam);
    }

    public static MilvusServiceClient createMilvusClient(String host, int port,
                                                         String databaseName) {
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port)
                .withDatabaseName(databaseName)
                .build();
        return new MilvusServiceClient(connectParam);
    }

}