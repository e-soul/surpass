package org.esoul.surpass.hello.api;

/** Describes whether a Windows Hello-backed key can be used on this machine. */
public enum HelloCapability {
    AVAILABLE,
    UNSUPPORTED_OS,
    NATIVE_ACCESS_DISABLED,
    DLL_UNAVAILABLE,
    PLATFORM_AUTHENTICATOR_UNAVAILABLE
}
