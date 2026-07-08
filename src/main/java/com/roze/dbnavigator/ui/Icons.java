package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.DbObject;
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

    public static FontIcon forObject(DbObject obj, boolean connected) {
        return switch (obj.getKind()) {
            case CONNECTION -> of(FontAwesomeSolid.DATABASE, connected ? "#57965c" : "#868a91", 13);
            case DATABASE   -> of(FontAwesomeSolid.DATABASE, "#3592c4", 12);
            case SCHEMA     -> of(FontAwesomeSolid.FOLDER_OPEN, "#c77dbb", 12);
            case TABLES_FOLDER, VIEWS_FOLDER, PROCEDURES_FOLDER,
                 FUNCTIONS_FOLDER, SEQUENCES_FOLDER, COLLECTIONS_FOLDER
                            -> of(FontAwesomeSolid.FOLDER, "#e0a44c", 12);
            case TABLE      -> of(FontAwesomeSolid.TABLE, "#4a88c7", 12);
            case VIEW       -> of(FontAwesomeSolid.EYE, "#57965c", 12);
            case PROCEDURE  -> of(FontAwesomeSolid.COG, "#c77dbb", 12);
            case FUNCTION   -> of(FontAwesomeSolid.SQUARE_ROOT_ALT, "#c77dbb", 12);
            case SEQUENCE   -> of(FontAwesomeSolid.SORT_NUMERIC_DOWN, "#e0a44c", 12);
            case COLLECTION -> of(FontAwesomeSolid.LAYER_GROUP, "#57965c", 12);
            case COLUMN     -> of(FontAwesomeSolid.COLUMNS, "#868a91", 11);
            case MESSAGE    -> of(FontAwesomeSolid.INFO_CIRCLE, "#868a91", 11);
        };
    }
}
