module stratosdb.core {
    requires stratosdb.common;
    requires stratosdb.transaction;
    requires stratosdb.storage;
    requires stratosdb.sql;
    requires org.slf4j;

    exports com.stratosdb.core;
}
