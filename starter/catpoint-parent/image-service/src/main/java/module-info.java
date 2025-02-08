module com.udacity.image {
    // AWS SDK dependencies
    requires software.amazon.awssdk.core;
    requires software.amazon.awssdk.auth;
    requires software.amazon.awssdk.regions;
    requires software.amazon.awssdk.services.rekognition;

    // Logging dependency
    requires org.slf4j;

    // Java desktop module for image processing
    requires java.desktop;

    // Export service package
    exports com.udacity.image.service;
}
