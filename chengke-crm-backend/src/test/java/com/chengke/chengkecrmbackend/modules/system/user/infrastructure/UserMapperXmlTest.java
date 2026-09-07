package com.chengke.chengkecrmbackend.modules.system.user.infrastructure;

import com.chengke.chengkecrmbackend.shared.persistence.PostgresUuidTypeHandler;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证用户读写 MyBatis XML 可独立解析且绑定到分离的 Mapper 命名空间。
 */
class UserMapperXmlTest {

    @Test
    void shouldParseSeparatedQueryAndCommandMappings() throws Exception {
        var configuration = new Configuration();
        configuration.getTypeHandlerRegistry().register(PostgresUuidTypeHandler.class);

        parse(configuration, "mapper/user/UserQueryMapper.xml");
        parse(configuration, "mapper/user/UserCommandMapper.xml");

        assertThat(configuration.hasStatement(
                "com.chengke.chengkecrmbackend.modules.system.user.infrastructure.persistence.mapper"
                        + ".UserQueryMapper.selectPage")).isTrue();
        assertThat(configuration.hasStatement(
                "com.chengke.chengkecrmbackend.modules.system.user.infrastructure.persistence.mapper"
                        + ".UserCommandMapper.insertUser")).isTrue();
    }

    /**
     * 解析一个 Mapper XML 到共享 MyBatis 配置。
     *
     * @param configuration MyBatis 测试配置
     * @param resource 类路径 XML
     * @throws Exception 资源读取或 XML 绑定失败时抛出
     */
    private void parse(Configuration configuration, String resource) throws Exception {
        try (var input = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }
}
