module com.udacity.security {
    requires com.udacity.image;
    requires java.logging;
    requires java.desktop;
    requires java.prefs;
    requires com.google.common;
    requires com.google.gson;
    requires miglayout.swing;
    opens com.udacity.security.data to com.google.gson;
}
