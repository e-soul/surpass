module surpass.google.drive.test {

    requires jdk.httpserver;
    requires jdk.unsupported;

    requires surpass.api;
    requires surpass.google.drive;

    requires org.junit.jupiter.api;

    requires org.mockito;
    requires com.google.api.client;

    exports org.esoul.surpass.google.drive.test to org.junit.platform.commons;
}
