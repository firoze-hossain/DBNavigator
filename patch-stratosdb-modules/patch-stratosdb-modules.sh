#!/bin/bash
# Patches the 8 StratosDB jars JitPack downloaded into your local ~/.m2 cache,
# giving each one a real, explicit JPMS module descriptor - the exact same
# technique already proven to work end-to-end (real live connect/CREATE
# TABLE/INSERT/SELECT through real JPMS module resolution). This never
# touches StratosDB's own repository or source - only the already-built
# jars sitting in your local Maven cache.
#
# Run this once after `mvn clean install` has successfully downloaded the
# StratosDB artifacts from JitPack, and again any time you bump the
# StratosDB version/tag (since a version bump means Maven downloads fresh,
# again-unpatched jars).
#
# Usage: ./patch-stratosdb-modules.sh [version] [release]
#   version defaults to 1.0.0 (your current tag) if not given.
#   release defaults to 21 (DBNavigator's own declared compiler target in
#   pom.xml) if not given - this is passed to javac's own --release flag so
#   the compiled module-info.class is guaranteed compatible with the JVM
#   that actually runs DBNavigator, regardless of what JDK version happens
#   to be the *default* `javac` on this machine's own PATH. A real, earlier
#   version of this script skipped this and produced a module-info.class
#   built for whatever newer JDK was actually the default `javac` here
#   (25, in one real, observed case) - incompatible with a JDK 21 runtime
#   ("Unsupported major.minor version 69.0" is exactly that mismatch).

set -e

VERSION="${1:-1.0.0}"
RELEASE="${2:-21}"
REPO="$HOME/.m2/repository/com/github/firoze-hossain/stratosdb"
DESC_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/module-descriptors"
WORK="$(mktemp -d)"

# Third-party modules some of the module-info.java files require - located
# dynamically since their exact versions can vary across machines.
SLF4J_JAR="$(find "$HOME/.m2/repository/org/slf4j/slf4j-api" -name '*.jar' | sort -V | tail -1)"
ANTLR_JAR="$(find "$HOME/.m2/repository/org/antlr/antlr4-runtime" -name '*.jar' | sort -V | tail -1)"
if [ -z "$SLF4J_JAR" ] || [ -z "$ANTLR_JAR" ]; then
    echo "ERROR: could not locate slf4j-api or antlr4-runtime in $HOME/.m2 - run mvn install first"
    exit 1
fi
THIRD_PARTY="$SLF4J_JAR:$ANTLR_JAR"

echo "Patching StratosDB $VERSION jars in $REPO (targeting --release $RELEASE) ..."
echo "Using javac: $(javac -version 2>&1) - compiling for --release $RELEASE regardless"

patch_module() {
    local artifact="$1"      # e.g. stratosdb-common
    local modname="$2"       # e.g. stratosdb.common
    local jar="$REPO/$artifact/$VERSION/$artifact-$VERSION.jar"
    local descriptor="$DESC_DIR/$artifact-module-info.java"

    if [ ! -f "$jar" ]; then
        echo "SKIP: $jar not found (did the build download it?)"
        return
    fi
    if [ ! -f "$descriptor" ]; then
        echo "ERROR: missing descriptor $descriptor"
        exit 1
    fi

    echo "  -> $artifact"
    mkdir -p "$WORK/$artifact/out" "$WORK/$artifact/src"
    cp "$descriptor" "$WORK/$artifact/src/module-info.java"
    javac --release "$RELEASE" \
          --module-path "$MODULE_PATH:$THIRD_PARTY" \
          --patch-module "$modname=$jar" \
          -d "$WORK/$artifact/out" \
          "$WORK/$artifact/src/module-info.java"
    (cd "$WORK/$artifact/out" && jar --update --file "$jar" module-info.class)
}

# Dependency order matters: each module's own module-info.java requires
# the ones below it, already patched, to be resolvable during its own
# --patch-module compile step.
MODULE_PATH=""
patch_module stratosdb-common stratosdb.common
MODULE_PATH="$REPO/stratosdb-common/$VERSION/stratosdb-common-$VERSION.jar"

patch_module stratosdb-transaction stratosdb.transaction
MODULE_PATH="$MODULE_PATH:$REPO/stratosdb-transaction/$VERSION/stratosdb-transaction-$VERSION.jar"

patch_module stratosdb-storage stratosdb.storage
MODULE_PATH="$MODULE_PATH:$REPO/stratosdb-storage/$VERSION/stratosdb-storage-$VERSION.jar"

patch_module stratosdb-index stratosdb.index
MODULE_PATH="$MODULE_PATH:$REPO/stratosdb-index/$VERSION/stratosdb-index-$VERSION.jar"

patch_module stratosdb-sql stratosdb.sql
MODULE_PATH="$MODULE_PATH:$REPO/stratosdb-sql/$VERSION/stratosdb-sql-$VERSION.jar"

patch_module stratosdb-core stratosdb.core
MODULE_PATH="$MODULE_PATH:$REPO/stratosdb-core/$VERSION/stratosdb-core-$VERSION.jar"

patch_module stratosdb-network stratosdb.network
MODULE_PATH="$MODULE_PATH:$REPO/stratosdb-network/$VERSION/stratosdb-network-$VERSION.jar"

patch_module stratosdb-jdbc stratosdb.jdbc

rm -rf "$WORK"
echo "Done. Verify with:"
echo "  java -p \"$REPO/stratosdb-network/$VERSION/stratosdb-network-$VERSION.jar\" --describe-module stratosdb.network"
echo "(should show real exports, no 'automatic' flag)"
