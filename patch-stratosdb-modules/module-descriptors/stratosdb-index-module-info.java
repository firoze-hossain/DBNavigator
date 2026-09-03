module stratosdb.index {
    requires stratosdb.common;
    requires stratosdb.storage;
    requires org.slf4j;

    exports com.stratosdb.index;
    exports com.stratosdb.index.bitmap;
    exports com.stratosdb.index.brin;
    exports com.stratosdb.index.btree;
    exports com.stratosdb.index.gin;
    exports com.stratosdb.index.gist;
    exports com.stratosdb.index.hash;
}
