[![Java CI with Gradle](https://github.com/e-soul/surpass/actions/workflows/gradle.yml/badge.svg)](https://github.com/e-soul/surpass/actions/workflows/gradle.yml)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/e-soul/surpass)
# Surpass

This is a simple, secure and easy to use password manager.

### Main features
 - Password-based encryption with **HMAC-SHA-512** and **AES-128** via Java Cryptography Architecture ([JCA](https://docs.oracle.com/en/java/javase/26/security/java-cryptography-architecture-jca-reference-guide.html))
 - Modular implementation via [jigsaw](http://openjdk.java.net/projects/jigsaw/). This allows creating an optimized image with [jlink](https://docs.oracle.com/en/java/javase/26/docs/specs/man/jlink.html). Such an image contains just enough of the Java runtime to execute the application and nothing more. The entire image is around *50 megabytes* (on Windows) which makes it easy to carry on any type of media.
 - Very simple and clean graphical user interface.
 - Very small code-base with focus on simplicity.

###### Downloads:
See [Releases](https://github.com/e-soul/surpass/releases)

### Build instructions
 - Install JDK 26, JavaFX SDK 26, and Gradle 9.7 or later.
 - Set `JAVA_HOME`, `JAVAFX_HOME`, and `GRADLE_HOME`. On Windows, `D:\dev\tool\devenv_surpass.bat` configures the expected build environment.
 - Clone this repository.
 - Run `gradle build generateAppDist`

The JavaFX desktop client lives in the `surpass.gui.jfx` module. Run it during development with:

```powershell
gradle :surpass.gui.jfx:run
```

#### Regenerate Eclipse project files

First, clean up Eclipse project files.

```powershell
Remove-Item -Recurse -Force -ErrorAction SilentlyContinue -Path '.\.project', '.\.settings', 'surpass.*\.classpath', 'surpass.*\.project', 'surpass.*\.settings'
```

or

```bash
rm -rf .project .settings surpass.*/.classpath surpass.*/.project surpass.*/.settings
```

Then:
 - Start Eclipse. Go to Window -> Preferences -> Gradle. Setup Gradle dist, Java home, and enable module support. Import Gradle project.
 - In case of Eclipse build or run issues, run `gradle eclipse` and refresh the Eclipse projects.
