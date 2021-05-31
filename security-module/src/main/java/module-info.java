module com.udacity.security.module {
    requires com.udacity.image.module;
    requires java.desktop;
    requires java.prefs;
    requires com.google.common;
    requires miglayout;
    requires com.google.gson;
    exports com.udacity.security.service;
    exports com.udacity.security.data;
    opens com.udacity.security.data;
}