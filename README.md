[![Java CI with Gradle](https://github.com/e-soul/surpass/actions/workflows/gradle.yml/badge.svg)](https://github.com/e-soul/surpass/actions/workflows/gradle.yml)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/e-soul/surpass)
# Surpass

This is a simple, secure and easy to use password manager.

### Main features
 - Password-based encryption with **HMAC-SHA-512** and **AES-128** via Java Cryptography Architecture ([JCA](https://docs.oracle.com/en/java/javase/23/security/java-cryptography-architecture-jca-reference-guide.html))
 - Modular implementation via [jigsaw](http://openjdk.java.net/projects/jigsaw/). This allows creating an optimized image with [jlink](https://docs.oracle.com/en/java/javase/23/docs/specs/man/jlink.html). Such an image contains just enough of the Java runtime to execute the application and nothing more. The entire image is around *50 megabytes* (on Windows) which makes it easy to carry on any USB stick or SD card.
 - There are hard limits on the number and length of passwords, the intention is to maintain a constant-size data file (vault).
 - Very simple and clean graphical user interface.
 - Very small code-base with focus on simplicity.

###### Downloads:
See [Releases](https://github.com/e-soul/surpass/releases)

### Build instructions
 - Install JDK 24 and Gradle 8.14.2 or later.
 - Clone this repository.
 - Run `gradle build generateAppDist`
