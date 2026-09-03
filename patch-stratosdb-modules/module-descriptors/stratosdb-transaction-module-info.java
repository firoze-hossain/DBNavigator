module stratosdb.transaction {
    requires stratosdb.common;
    requires org.slf4j;

    exports com.stratosdb.transaction;
    exports com.stratosdb.transaction.locking;
    exports com.stratosdb.transaction.mvcc;
}
