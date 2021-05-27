module com.udacity.security.module {
    requires com.udacity.image.module;
    requires java.desktop;
    requires com.google.gson;
    requires java.prefs;
    requires com.google.common;
    requires miglayout;
    exports com.udacity.security.service;
    exports com.udacity.security.data;
}