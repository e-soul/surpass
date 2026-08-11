[![Java CI with Gradle](https://github.com/e-soul/surpass/actions/workflows/gradle.yml/badge.svg)](https://github.com/e-soul/surpass/actions/workflows/gradle.yml)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/e-soul/surpass)
# Surpass

This is a simple, secure and easy to use password manager.

### Main features
 - Version-1 vault envelopes use **PBKDF2-HMAC-SHA-512** and **AES-256-GCM** envelope encryption via Java Cryptography Architecture ([JCA](https://docs.oracle.com/en/java/javase/26/security/java-cryptography-architecture-jca-reference-guide.html)). Existing version-0 vaults remain readable and are migrated with a recovery backup.
 - On Windows, an unlocked vault can optionally enroll **Windows Hello** for offline unlock. A device-local `KeyCredentialManager` key signs a stored challenge after Hello verification; the derived wrapping material and encrypted local binding never replace the portable master password.
 - Modular implementation via [jigsaw](http://openjdk.java.net/projects/jigsaw/). This allows creating an optimized image with [jlink](https://docs.oracle.com/en/java/javase/26/docs/specs/man/jlink.html). Such an image contains just enough of the Java runtime to execute the application and nothing more. The entire image is around *50 megabytes* (on Windows) which makes it easy to carry on any type of media.
 - Very simple and clean graphical user interface.
 - Very small code-base with focus on simplicity.

### Windows Hello recovery

Windows Hello is optional and can be enabled only after unlocking with the master password. Removing the Hello key, resetting the TPM, losing the local binding, or moving the vault to another computer does not change the vault password: choose **Master password** at unlock to recover access, then enroll Hello again if desired. Canceling a Hello prompt never changes the vault.

###### Downloads:
See [Releases](https://github.com/e-soul/surpass/releases)

### Build instructions
 - Install JDK 26 and Gradle 9.7 or later.
 - Clone this repository.
 - Run `gradle build generateAppDist`

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
