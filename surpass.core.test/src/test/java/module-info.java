module surpass.core.test {

    requires surpass.api;
    requires surpass.core;

    requires org.junit.jupiter.api;
    requires org.junit.jupiter.params;

    exports org.esoul.surpass.core.test to org.junit.platform.commons;
}
