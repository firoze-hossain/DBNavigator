package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

/** Central icon factory so the whole app uses a consistent icon language. */
public final class Icons {

    private Icons() {}

    public static FontIcon of(FontAwesomeSolid icon, String colorHex, int size) {
        FontIcon fi = new FontIcon(icon);
        fi.setIconColor(Color.web(colorHex));
        fi.setIconSize(size);
        return fi;
    }

    public static Node collapseAll() {
        javafx.scene.shape.SVGPath svg = new javafx.scene.shape.SVGPath();
        svg.setContent("M3.707 3.293a1 1 0 0 0-1.414 1.414L5.586 8l-3.293 3.293a1 1 0 1 0 1.414 1.414l4-4a1 1 0 0 0 0-1.414l-4-4zm8.586 0a1 1 0 0 1 1.414 1.414L10.414 8l3.293 3.293a1 1 0 0 1-1.414 1.414l-4-4a1 1 0 0 1 0-1.414l4-4z");
        svg.setFill(Color.web("#a9b7c6"));
        return svg;
    }

    public static Node forDatabase(ConnectionProfile.DatabaseType type) {
        return DatabaseIcon.create(type, 14, true);
    }

    public static Node forDatabase(ConnectionProfile.DatabaseType type, int size) {
        return DatabaseIcon.create(type, size, true);
    }

    public static Node forDatabase(ConnectionProfile.DatabaseType type, int size, boolean connected) {
        return DatabaseIcon.create(type, size, connected);
    }

    public static Node forObject(DbObject obj, boolean connected) {
        return forObject(obj, null, connected);
    }

    public static Node forObject(DbObject obj, ConnectionProfile profile, boolean connected) {
        if (obj.getKind() == DbObject.Kind.CONNECTION && profile != null) {
            return DatabaseIcon.create(profile.getType(), 14, connected);
        }
        return switch (obj.getKind()) {
            case CONNECTION -> of(FontAwesomeSolid.DATABASE, connected ? "#57965c" : "#868a91", 13);
            case DATABASE   -> {
                if (profile != null && (profile.getType() == ConnectionProfile.DatabaseType.MYSQL
                        || profile.getType() == ConnectionProfile.DatabaseType.MARIADB)) {
                    yield of(FontAwesomeSolid.PROJECT_DIAGRAM, "#c77dbb", 12);
                }
                yield of(FontAwesomeSolid.DATABASE, "#3592c4", 12);
            }
            case SCHEMA     -> of(FontAwesomeSolid.PROJECT_DIAGRAM, "#c77dbb", 12);
            case SERVER_OBJECTS -> of(FontAwesomeSolid.SERVER, "#3592c4", 12);
            case ROLES_FOLDER, TABLESPACES_FOLDER -> of(FontAwesomeSolid.FOLDER, "#3592c4", 12);
            case DATABASE_OBJECTS -> of(FontAwesomeSolid.FOLDER, "#3592c4", 12);
            case ACCESS_METHODS_FOLDER, CASTS_FOLDER, EXTENSIONS_FOLDER, LANGUAGES_FOLDER -> of(FontAwesomeSolid.FOLDER, "#3592c4", 12);
            case VIRTUAL_VIEWS_FOLDER -> of(FontAwesomeSolid.FOLDER, "#c77dbb", 12);
            case TABLES_FOLDER, VIEWS_FOLDER, PROCEDURES_FOLDER,
                 FUNCTIONS_FOLDER, SEQUENCES_FOLDER, COLLECTIONS_FOLDER,
                 COLUMNS_FOLDER, INDEXES_FOLDER, PARTITIONS_FOLDER, KEYS_FOLDER, FOREIGN_KEYS_FOLDER, FIELDS_FOLDER
                            -> of(FontAwesomeSolid.FOLDER, "#e0a44c", 12);
            case ROLE       -> of(FontAwesomeSolid.USER, "#868a91", 11);
            case TABLESPACE -> of(FontAwesomeSolid.HDD, "#3592c4", 11);
            case ACCESS_METHOD -> of(FontAwesomeSolid.AT, "#3592c4", 11);
            case CAST          -> of(FontAwesomeSolid.EXCHANGE_ALT, "#3592c4", 11);
            case EXTENSION     -> of(FontAwesomeSolid.PUZZLE_PIECE, "#3592c4", 11);
            case LANGUAGE      -> of(FontAwesomeSolid.CODE, "#3592c4", 11);
            case VIRTUAL_VIEW  -> of(FontAwesomeSolid.TABLE, "#c77dbb", 12);
            case TABLE      -> of(FontAwesomeSolid.TABLE, "#4a88c7", 12);
            case VIEW       -> of(FontAwesomeSolid.EYE, "#57965c", 12);
            case PROCEDURE  -> of(FontAwesomeSolid.COG, "#c77dbb", 12);
            case FUNCTION   -> of(FontAwesomeSolid.SQUARE_ROOT_ALT, "#c77dbb", 12);
            case SEQUENCE   -> of(FontAwesomeSolid.SORT_NUMERIC_DOWN, "#e0a44c", 12);
            case COLLECTION -> of(FontAwesomeSolid.LAYER_GROUP, "#57965c", 12);
            case COLUMN     -> of(FontAwesomeSolid.COLUMNS, "#868a91", 11);
            case INDEX      -> of(FontAwesomeSolid.BOLT, "#e0a44c", 11);
            case PARTITION  -> of(FontAwesomeSolid.TABLE, "#4a88c7", 12);
            case KEY         -> of(FontAwesomeSolid.KEY, "#e0a44c", 11);
            case FOREIGN_KEY -> of(FontAwesomeSolid.KEY, "#4a88c7", 11);
            // _id is the collection's de-facto primary key — same gold key
            // icon as a real primary key, matching the reference exactly;
            // every other (schema-less, inferred) field gets a generic
            // document-field icon instead.
            case FIELD -> "_id".equals(obj.getName())
                    ? of(FontAwesomeSolid.KEY, "#e0a44c", 11)
                    : of(FontAwesomeSolid.CODE, "#6897bb", 11);
            case MESSAGE    -> of(FontAwesomeSolid.INFO_CIRCLE, "#868a91", 11);
        };
    }
}
