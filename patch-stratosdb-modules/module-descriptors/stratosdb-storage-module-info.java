module stratosdb.storage {
    requires stratosdb.common;
    requires stratosdb.transaction;
    requires org.slf4j;
    requires java.sql;

    exports com.stratosdb.storage.buffer;
    exports com.stratosdb.storage.disk;
    exports com.stratosdb.storage.heap;
    exports com.stratosdb.storage.page;
    exports com.stratosdb.storage.wal;
}
