module stratosdb.sql {
    requires stratosdb.common;
    requires stratosdb.transaction;
    requires stratosdb.storage;
    requires stratosdb.index;
    requires org.slf4j;
    requires org.antlr.antlr4.runtime;

    exports com.stratosdb.sql.ast;
    exports com.stratosdb.sql.executor;
    exports com.stratosdb.sql.extension;
    exports com.stratosdb.sql.parser;
    exports com.stratosdb.sql.plpgsql;
}
