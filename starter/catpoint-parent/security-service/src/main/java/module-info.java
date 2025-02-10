module com.udacity.security {
    requires com.udacity.image;
    requires java.logging;
    exports com.udacity.security.application;
    exports com.udacity.security.service;
    exports com.udacity.security.data;
    requires java.desktop;
    requires java.prefs;
    requires com.google.common;
    requires com.google.gson;
    requires miglayout.swing;
    opens com.udacity.security.data to com.google.gson;
}
