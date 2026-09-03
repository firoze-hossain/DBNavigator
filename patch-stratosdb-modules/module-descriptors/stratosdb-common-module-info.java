/** See stratosdb-network's own module-info.java for the full account of why
 * this exists as a pure, post-build-injected packaging artifact rather than
 * a change to StratosDB's own source. This module has no cross-module or
 * third-party dependencies of its own - it sits at the bottom of the real
 * StratosDB dependency graph. */
module stratosdb.common {
    exports com.stratosdb.common.constants;
    exports com.stratosdb.common.exceptions;
    exports com.stratosdb.common.utils;
}
