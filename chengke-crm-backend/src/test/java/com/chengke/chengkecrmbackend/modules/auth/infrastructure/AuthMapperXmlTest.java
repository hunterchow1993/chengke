package com.chengke.chengkecrmbackend.modules.auth.infrastructure;

import com.chengke.chengkecrmbackend.shared.persistence.PostgresUuidTypeHandler;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证登录 MyBatis XML 可独立解析。
 */
class AuthMapperXmlTest {

    @Test
    void shouldParseAuthMappings() throws Exception {
        var configuration = new Configuration();
        configuration.getTypeHandlerRegistry().register(PostgresUuidTypeHandler.class);
        String resource = "mapper/auth/AuthMapper.xml";
        try (var input = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        String namespace = "com.chengke.chengkecrmbackend.modules.auth.infrastructure.persistence.mapper.AuthMapper";
        assertThat(configuration.hasStatement(namespace + ".findByUsername")).isTrue();
        assertThat(configuration.hasStatement(namespace + ".insertSession")).isTrue();
        assertThat(configuration.hasStatement(namespace + ".findActiveSession")).isTrue();
        assertThat(configuration.hasStatement(namespace + ".consumeCaptcha")).isTrue();
    }
}
