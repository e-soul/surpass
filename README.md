# Surpass

This is a simple, secure and easy to use password manager.

### Main features
 - Password-based encryption with **HMAC-SHA-512** and **AES-128** via Java Cryptography Architecture ([JCA](https://docs.oracle.com/en/java/javase/13/security/java-cryptography-architecture-jca-reference-guide.html))
 - Modular implementation via [jigsaw](http://openjdk.java.net/projects/jigsaw/). This allows creating an optimized image with [jlink](https://docs.oracle.com/en/java/javase/13/docs/specs/man/jlink.html). Such an image contains just enough of the Java runtime to execute the application and nothing more. The entire image is around *38 megabytes* (on Windows) which makes it easy to carry on any USB stick or SD card. This makes it similar to a hardware password manager.
 - There are hard limits to the number and length of passwords, the intention is to maintain a constant-size data file. This slightly mitigates some attacks.
 - Very simple and clean graphical user interface.
 - Very small code-base with focus on simplicity. Everyone should be able to review the code and see exactly how it works.

###### Downloads:
 - [Windows 7-10](https://bitbucket.org/e-soul/surpass/downloads/surpass-windows-0.6.zip)
 - [Linux](https://bitbucket.org/e-soul/surpass/downloads/surpass-linux-0.6.zip)
