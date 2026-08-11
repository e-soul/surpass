module surpass.hello.test {
    requires surpass.api;
    requires surpass.hello;
    requires surpass.core;
    requires surpass.test;
    requires org.junit.jupiter.api;

    exports org.esoul.surpass.hello.test to org.junit.platform.commons;
}
