module surpass.google.drive.test {

    requires jdk.httpserver;
    requires jdk.unsupported;

    requires surpass.api;
    requires surpass.core;
    requires surpass.google.drive;
    requires surpass.test;

    requires org.junit.jupiter.api;

    requires org.mockito;
    requires com.google.api.client;

    exports org.esoul.surpass.google.drive.test to org.junit.platform.commons;
}
