package com.caile.jczq.data.crawler;

import org.hibernate.dialect.PostgreSQL94Dialect;

import java.sql.Types;

public class StringToTextPostgreSQLDialect extends PostgreSQL94Dialect {

    public StringToTextPostgreSQLDialect() {
        this.registerColumnType(Types.VARCHAR, "TEXT");
    }
}
