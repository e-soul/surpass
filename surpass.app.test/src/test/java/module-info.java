module surpass.app.test {

    requires surpass.api;
    requires surpass.app;
    requires surpass.test;

    requires org.junit.jupiter.api;

    exports org.esoul.surpass.app.test to org.junit.platform.commons;
}
