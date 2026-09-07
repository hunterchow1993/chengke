package com.chengke.chengkecrmbackend.shared.persistence;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

/**
 * 在 MyBatis 与 PostgreSQL uuid 类型之间执行安全双向转换。
 */
@MappedTypes(UUID.class)
@MappedJdbcTypes(value = JdbcType.OTHER, includeNullJdbcType = true)
public class PostgresUuidTypeHandler extends BaseTypeHandler<UUID> {

    /** {@inheritDoc} */
    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, UUID parameter, JdbcType jdbcType)
            throws SQLException {
        statement.setObject(index, parameter, Types.OTHER);
    }

    /** {@inheritDoc} */
    @Override
    public UUID getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        return toUuid(resultSet.getObject(columnName));
    }

    /** {@inheritDoc} */
    @Override
    public UUID getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        return toUuid(resultSet.getObject(columnIndex));
    }

    /** {@inheritDoc} */
    @Override
    public UUID getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
        return toUuid(statement.getObject(columnIndex));
    }

    /**
     * 转换 PostgreSQL 驱动返回的 UUID 或字符串对象。
     *
     * @param value JDBC 返回值
     * @return UUID；数据库值为空时返回 null
     */
    private UUID toUuid(Object value) {
        if (value == null) return null;
        return value instanceof UUID uuid ? uuid : UUID.fromString(value.toString());
    }
}
