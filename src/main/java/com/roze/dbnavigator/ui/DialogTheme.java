package com.roze.dbnavigator.ui;

import javafx.scene.control.Dialog;

/**
 * JavaFX's built-in {@code Dialog}/{@code Alert}/{@code TextInputDialog}/
 * {@code ChoiceDialog} each create their own Scene, separate from the main
 * window's — so unless the app's stylesheet is attached explicitly, they
 * render with the plain native OS look (white background, black text)
 * instead of matching the rest of the app. This was the cause of the "New
 * Data Source" dialog (and every other built-in Alert/TextInputDialog in the
 * app) showing up unstyled.
 *
 * Call {@link #apply(Dialog)} right after constructing any such dialog.
 *
 * The generic bound here is deliberately {@code <D extends Dialog<?>>} — not
 * {@code <T> Dialog<T>} — so calling apply(new Alert(...)) returns Alert
 * itself rather than the supertype Dialog&lt;ButtonType&gt;. With the naive
 * signature, assigning the result to a variable of type Alert or
 * TextInputDialog requires an explicit cast (a real compile error otherwise,
 * not just a style nit); this signature infers the concrete subtype so no
 * cast is ever needed at the call site.
 */
public final class DialogTheme {

    private static final String STYLESHEET =
            DialogTheme.class.getResource("/css/app.css").toExternalForm();

    private DialogTheme() {}

    /** Attaches the app's dark theme; returns the same dialog for fluent chaining. */
    public static <D extends Dialog<?>> D apply(D dialog) {
        dialog.getDialogPane().getStylesheets().add(STYLESHEET);
        dialog.getDialogPane().getStyleClass().add("themed-dialog");
        return dialog;
    }
}
