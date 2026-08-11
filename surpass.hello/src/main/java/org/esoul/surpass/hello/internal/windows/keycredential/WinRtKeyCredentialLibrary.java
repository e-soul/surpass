package org.esoul.surpass.hello.internal.windows.keycredential;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;

import org.esoul.surpass.hello.api.UnlockException;

final class WinRtKeyCredentialLibrary {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final long POLL_NANOS = 20_000_000L;

    private static final int RO_INIT_MULTITHREADED = 1;
    private static final int RPC_E_CHANGED_MODE = 0x80010106;
    private static final int E_ABORT = 0x80004004;
    private static final int HRESULT_ERROR_CANCELLED = 0x800704c7;
    private static final int NTE_BAD_ALGID = 0x80090008;
    private static final int NTE_NOT_FOUND = 0x80090011;

    private static final int ASYNC_STARTED = 0;
    private static final int ASYNC_COMPLETED = 1;
    private static final int ASYNC_CANCELED = 2;
    private static final int ASYNC_ERROR = 3;

    private static final int KEY_SUCCESS = 0;
    private static final int KEY_NOT_FOUND = 2;
    private static final int KEY_USER_CANCELED = 3;
    private static final int KEY_USER_PREFERS_PASSWORD = 4;
    private static final int KEY_ALGORITHM_NOT_SUPPORTED = 7;

    private static final UUID IID_ASYNC_INFO = UUID.fromString(
            "00000036-0000-0000-c000-000000000046");
    private static final UUID IID_KEY_CREDENTIAL_MANAGER = UUID.fromString(
            "6aac468b-0ef1-4ce0-8290-4106da6a63b5");
    private static final UUID IID_BUFFER_FACTORY = UUID.fromString(
            "71af914d-c10f-484b-bc50-14bc623b3a27");
    private static final UUID IID_BUFFER_BYTE_ACCESS = UUID.fromString(
            "905a0fef-bc53-11df-8c49-001e4fc686da");

    private static final String KEY_MANAGER_CLASS =
            "Windows.Security.Credentials.KeyCredentialManager";
    private static final String BUFFER_CLASS = "Windows.Storage.Streams.Buffer";

    private static final FunctionDescriptor FD_INT_INT = FunctionDescriptor.of(JAVA_INT, JAVA_INT);
    private static final FunctionDescriptor FD_INT_ADDRESS = FunctionDescriptor.of(JAVA_INT,
            ADDRESS);
    private static final FunctionDescriptor FD_INT_ADDRESS_ADDRESS = FunctionDescriptor.of(JAVA_INT,
            ADDRESS, ADDRESS);
    private static final FunctionDescriptor FD_INT_ADDRESS_INT_ADDRESS = FunctionDescriptor.of(
            JAVA_INT, ADDRESS, JAVA_INT, ADDRESS);
    private static final FunctionDescriptor FD_INT_ADDRESS_ADDRESS_ADDRESS = FunctionDescriptor.of(
            JAVA_INT, ADDRESS, ADDRESS, ADDRESS);
    private static final FunctionDescriptor FD_INT_ADDRESS_ADDRESS_INT_ADDRESS =
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, ADDRESS);
    private static final FunctionDescriptor FD_INT_ADDRESS_INT = FunctionDescriptor.of(JAVA_INT,
            ADDRESS, JAVA_INT);
    private static final FunctionDescriptor FD_VOID = FunctionDescriptor.ofVoid();

    private final MethodHandle roInitialize;
    private final MethodHandle roUninitialize;
    private final MethodHandle windowsCreateString;
    private final MethodHandle windowsDeleteString;
    private final MethodHandle roGetActivationFactory;

    private volatile boolean cancellationRequested;

    WinRtKeyCredentialLibrary() {
        SymbolLookup combase = SymbolLookup.libraryLookup("combase.dll", Arena.global());
        roInitialize = downcall(combase, "RoInitialize", FD_INT_INT);
        roUninitialize = downcall(combase, "RoUninitialize", FD_VOID);
        windowsCreateString = downcall(combase, "WindowsCreateString",
                FD_INT_ADDRESS_INT_ADDRESS);
        windowsDeleteString = downcall(combase, "WindowsDeleteString", FD_INT_ADDRESS);
        roGetActivationFactory = downcall(combase, "RoGetActivationFactory",
                FD_INT_ADDRESS_ADDRESS_ADDRESS);
    }

    boolean isSupported() {
        try (Arena arena = Arena.ofConfined(); Apartment ignored = initializeApartment();
                ComPtr manager = activationFactory(arena, KEY_MANAGER_CLASS,
                        IID_KEY_CREDENTIAL_MANAGER);
                ComPtr operation = objectResult(manager, 6, arena, "Windows Hello capability")) {
            return awaitBoolean(operation, arena, "Windows Hello capability");
        } catch (UnlockException e) {
            return false;
        }
    }

    void create(String name) throws UnlockException {
        cancellationRequested = false;
        try (Arena arena = Arena.ofConfined(); Apartment ignored = initializeApartment();
                HString keyName = hstring(arena, name);
                ComPtr manager = activationFactory(arena, KEY_MANAGER_CLASS,
                        IID_KEY_CREDENTIAL_MANAGER);
                ComPtr operation = objectResult(manager, 8, keyName.value(), 0, arena,
                        "Windows Hello key creation");
                ComPtr retrieval = awaitObject(operation, arena, "Windows Hello key creation")) {
            int status = intProperty(retrieval, 7, arena, "Windows Hello key creation status");
            checkKeyStatus(status, "Windows Hello key creation");
        } finally {
            cancellationRequested = false;
        }
    }

    byte[] sign(String name, byte[] challenge) throws UnlockException {
        cancellationRequested = false;
        try (Arena arena = Arena.ofConfined(); Apartment ignored = initializeApartment();
                HString keyName = hstring(arena, name);
                ComPtr manager = activationFactory(arena, KEY_MANAGER_CLASS,
                        IID_KEY_CREDENTIAL_MANAGER);
                ComPtr openOperation = objectResult(manager, 9, keyName.value(), arena,
                        "Windows Hello key lookup");
                ComPtr retrieval = awaitObject(openOperation, arena, "Windows Hello key lookup")) {
            int retrievalStatus = intProperty(retrieval, 7, arena,
                    "Windows Hello key lookup status");
            checkKeyStatus(retrievalStatus, "Windows Hello key lookup");
            try (ComPtr credential = objectProperty(retrieval, 6, arena,
                    "Windows Hello credential"); ComPtr input = inputBuffer(arena, challenge);
                    ComPtr signOperation = objectResult(credential, 9, input.pointer(), arena,
                            "Windows Hello verification");
                    ComPtr signResult = awaitObject(signOperation, arena,
                            "Windows Hello verification")) {
                int signStatus = intProperty(signResult, 7, arena,
                        "Windows Hello verification status");
                checkKeyStatus(signStatus, "Windows Hello verification");
                try (ComPtr signature = objectProperty(signResult, 6, arena,
                        "Windows Hello signature")) {
                    return readBuffer(signature, arena);
                }
            }
        } finally {
            cancellationRequested = false;
        }
    }

    void delete(String name) throws UnlockException {
        cancellationRequested = false;
        try (Arena arena = Arena.ofConfined(); Apartment ignored = initializeApartment();
                HString keyName = hstring(arena, name);
                ComPtr manager = activationFactory(arena, KEY_MANAGER_CLASS,
                        IID_KEY_CREDENTIAL_MANAGER);
                ComPtr operation = objectResult(manager, 10, keyName.value(), arena,
                        "Windows Hello key removal")) {
            awaitAction(operation, arena, "Windows Hello key removal");
        } finally {
            cancellationRequested = false;
        }
    }

    void cancel() {
        cancellationRequested = true;
    }

    private ComPtr inputBuffer(Arena arena, byte[] input) throws UnlockException {
        try (ComPtr factory = activationFactory(arena, BUFFER_CLASS, IID_BUFFER_FACTORY)) {
            MemorySegment output = nullPointer(arena);
            check(callIntOut(factory, 6, input.length, output), "Windows buffer creation");
            ComPtr buffer = take(output, "Windows buffer creation");
            boolean success = false;
            try {
                check(callInt(buffer, 8, input.length), "Windows buffer length");
                try (ComPtr access = buffer.query(IID_BUFFER_BYTE_ACCESS, arena)) {
                    MemorySegment bytes = nullPointer(arena);
                    check(callObjectOut(access, 3, bytes), "Windows buffer access");
                    MemorySegment destination = address(bytes, "Windows buffer access")
                            .reinterpret(input.length);
                    destination.copyFrom(MemorySegment.ofArray(input));
                }
                success = true;
                return buffer;
            } finally {
                if (!success) {
                    buffer.close();
                }
            }
        }
    }

    private byte[] readBuffer(ComPtr buffer, Arena arena) throws UnlockException {
        int length = intProperty(buffer, 7, arena, "Windows signature length");
        if (length <= 0 || length > 8192) {
            throw new UnlockException(UnlockException.Reason.NATIVE_FAILURE,
                    "Windows returned an invalid signature length");
        }
        try (ComPtr access = buffer.query(IID_BUFFER_BYTE_ACCESS, arena)) {
            MemorySegment output = nullPointer(arena);
            check(callObjectOut(access, 3, output), "Windows signature access");
            return address(output, "Windows signature access").reinterpret(length)
                    .toArray(JAVA_BYTE);
        }
    }

    private boolean awaitBoolean(ComPtr operation, Arena arena, String description)
            throws UnlockException {
        try (ComPtr info = operation.query(IID_ASYNC_INFO, arena)) {
            try {
                await(info, arena, description);
                MemorySegment output = arena.allocate(JAVA_INT);
                check(callObjectOut(operation, 8, output), description);
                return output.get(JAVA_INT, 0) != 0;
            } finally {
                closeAsync(info);
            }
        }
    }

    private ComPtr awaitObject(ComPtr operation, Arena arena, String description)
            throws UnlockException {
        try (ComPtr info = operation.query(IID_ASYNC_INFO, arena)) {
            try {
                await(info, arena, description);
                MemorySegment output = nullPointer(arena);
                check(callObjectOut(operation, 8, output), description);
                return take(output, description);
            } finally {
                closeAsync(info);
            }
        }
    }

    private void awaitAction(ComPtr operation, Arena arena, String description)
            throws UnlockException {
        try (ComPtr info = operation.query(IID_ASYNC_INFO, arena)) {
            try {
                await(info, arena, description);
                check(callNoArg(operation, 8, description), description);
            } finally {
                closeAsync(info);
            }
        }
    }

    private void await(ComPtr info, Arena arena, String description) throws UnlockException {
        boolean cancellationSent = false;
        while (true) {
            int status = intProperty(info, 7, arena, description + " status");
            if (status == ASYNC_STARTED) {
                if (cancellationRequested && !cancellationSent) {
                    check(callNoArg(info, 9, "Windows Hello cancellation"),
                            "Windows Hello cancellation");
                    cancellationSent = true;
                }
                LockSupport.parkNanos(POLL_NANOS);
                continue;
            }
            if (status == ASYNC_COMPLETED) {
                return;
            }
            if (status == ASYNC_CANCELED) {
                throw new UnlockException(UnlockException.Reason.CANCELED,
                        description + " was canceled");
            }
            if (status == ASYNC_ERROR) {
                int error = intProperty(info, 8, arena, description + " error");
                throw nativeFailure(description, error);
            }
            throw new UnlockException(UnlockException.Reason.NATIVE_FAILURE,
                    description + " returned an invalid asynchronous status");
        }
    }

    private void closeAsync(ComPtr info) {
        try {
            callNoArg(info, 10, "Windows asynchronous operation cleanup");
        } catch (UnlockException ignored) {
            // Result and interface releases remain sufficient cleanup if Close itself fails.
        }
    }

    private Apartment initializeApartment() throws UnlockException {
        int result;
        try {
            result = (int) roInitialize.invokeExact(RO_INIT_MULTITHREADED);
        } catch (Throwable e) {
            throw nativeFailure("Windows Runtime initialization", e);
        }
        if (result < 0 && result != RPC_E_CHANGED_MODE) {
            throw nativeFailure("Windows Runtime initialization", result);
        }
        return new Apartment(result >= 0);
    }

    private ComPtr activationFactory(Arena arena, String className, UUID iid)
            throws UnlockException {
        try (HString runtimeClass = hstring(arena, className)) {
            MemorySegment output = nullPointer(arena);
            check(invokeRoGetActivationFactory(runtimeClass.value(), guid(arena, iid), output),
                    className + " activation");
            return take(output, className + " activation");
        }
    }

    private HString hstring(Arena arena, String value) throws UnlockException {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_16LE);
        MemorySegment source = arena.allocate(encoded.length, 2);
        source.copyFrom(MemorySegment.ofArray(encoded));
        MemorySegment output = nullPointer(arena);
        int result;
        try {
            result = (int) windowsCreateString.invokeExact(source, value.length(), output);
        } catch (Throwable e) {
            throw nativeFailure("Windows string creation", e);
        }
        check(result, "Windows string creation");
        return new HString(output.get(ADDRESS, 0));
    }

    private ComPtr objectResult(ComPtr object, int slot, Arena arena, String description)
            throws UnlockException {
        MemorySegment output = nullPointer(arena);
        check(callObjectOut(object, slot, output), description);
        return take(output, description);
    }

    private ComPtr objectResult(ComPtr object, int slot, MemorySegment argument, Arena arena,
            String description) throws UnlockException {
        MemorySegment output = nullPointer(arena);
        check(callAddressOut(object, slot, argument, output), description);
        return take(output, description);
    }

    private ComPtr objectResult(ComPtr object, int slot, MemorySegment argument, int option,
            Arena arena, String description) throws UnlockException {
        MemorySegment output = nullPointer(arena);
        check(callAddressIntOut(object, slot, argument, option, output), description);
        return take(output, description);
    }

    private ComPtr objectProperty(ComPtr object, int slot, Arena arena, String description)
            throws UnlockException {
        return objectResult(object, slot, arena, description);
    }

    private int intProperty(ComPtr object, int slot, Arena arena, String description)
            throws UnlockException {
        MemorySegment output = arena.allocate(JAVA_INT);
        check(callObjectOut(object, slot, output), description);
        return output.get(JAVA_INT, 0);
    }

    private int invokeRoGetActivationFactory(MemorySegment className, MemorySegment iid,
            MemorySegment output) throws UnlockException {
        try {
            return (int) roGetActivationFactory.invokeExact(className, iid, output);
        } catch (Throwable e) {
            throw nativeFailure("Windows activation factory lookup", e);
        }
    }

    private static int callObjectOut(ComPtr object, int slot, MemorySegment output)
            throws UnlockException {
        try {
            MethodHandle method = virtualMethod(object, slot, FD_INT_ADDRESS_ADDRESS);
            return (int) method.invokeExact(object.pointer(), output);
        } catch (Throwable e) {
            throw nativeFailure("Windows Runtime method", e);
        }
    }

    private static int callAddressOut(ComPtr object, int slot, MemorySegment argument,
            MemorySegment output) throws UnlockException {
        try {
            MethodHandle method = virtualMethod(object, slot, FD_INT_ADDRESS_ADDRESS_ADDRESS);
            return (int) method.invokeExact(object.pointer(), argument, output);
        } catch (Throwable e) {
            throw nativeFailure("Windows Runtime method", e);
        }
    }

    private static int callAddressIntOut(ComPtr object, int slot, MemorySegment argument,
            int option, MemorySegment output) throws UnlockException {
        try {
            MethodHandle method = virtualMethod(object, slot,
                    FD_INT_ADDRESS_ADDRESS_INT_ADDRESS);
            return (int) method.invokeExact(object.pointer(), argument, option, output);
        } catch (Throwable e) {
            throw nativeFailure("Windows Runtime method", e);
        }
    }

    private static int callIntOut(ComPtr object, int slot, int argument, MemorySegment output)
            throws UnlockException {
        try {
            MethodHandle method = virtualMethod(object, slot, FD_INT_ADDRESS_INT_ADDRESS);
            return (int) method.invokeExact(object.pointer(), argument, output);
        } catch (Throwable e) {
            throw nativeFailure("Windows Runtime method", e);
        }
    }

    private static int callInt(ComPtr object, int slot, int argument) throws UnlockException {
        try {
            MethodHandle method = virtualMethod(object, slot, FD_INT_ADDRESS_INT);
            return (int) method.invokeExact(object.pointer(), argument);
        } catch (Throwable e) {
            throw nativeFailure("Windows Runtime method", e);
        }
    }

    private static int callNoArg(ComPtr object, int slot, String description)
            throws UnlockException {
        try {
            MethodHandle method = virtualMethod(object, slot, FD_INT_ADDRESS);
            return (int) method.invokeExact(object.pointer());
        } catch (Throwable e) {
            throw nativeFailure(description, e);
        }
    }

    private static MethodHandle virtualMethod(ComPtr object, int slot,
            FunctionDescriptor descriptor) {
        MemorySegment vtable = object.pointer().reinterpret(ADDRESS.byteSize()).get(ADDRESS, 0)
                .reinterpret((long) (slot + 1) * ADDRESS.byteSize());
        MemorySegment address = vtable.getAtIndex(ADDRESS, slot);
        if (address.address() == 0) {
            throw new IllegalStateException("Null Windows Runtime vtable entry");
        }
        return LINKER.downcallHandle(address, descriptor);
    }

    private static ComPtr take(MemorySegment output, String description) throws UnlockException {
        return new ComPtr(address(output, description));
    }

    private static MemorySegment address(MemorySegment output, String description)
            throws UnlockException {
        MemorySegment pointer = output.get(ADDRESS, 0);
        if (pointer.address() == 0) {
            throw new UnlockException(UnlockException.Reason.NATIVE_FAILURE,
                    description + " returned no object");
        }
        return pointer;
    }

    private static MemorySegment nullPointer(Arena arena) {
        MemorySegment output = arena.allocate(ADDRESS);
        output.set(ADDRESS, 0, MemorySegment.NULL);
        return output;
    }

    private static MemorySegment guid(Arena arena, UUID value) {
        MemorySegment guid = arena.allocate(16, 4);
        long most = value.getMostSignificantBits();
        long least = value.getLeastSignificantBits();
        guid.set(JAVA_INT, 0, (int) (most >>> 32));
        guid.set(JAVA_SHORT, 4, (short) (most >>> 16));
        guid.set(JAVA_SHORT, 6, (short) most);
        for (int i = 0; i < 8; i++) {
            guid.set(JAVA_BYTE, 8L + i, (byte) (least >>> (56 - i * 8)));
        }
        return guid;
    }

    private static void checkKeyStatus(int status, String operation) throws UnlockException {
        if (status == KEY_SUCCESS) {
            return;
        }
        UnlockException.Reason reason = switch (status) {
            case KEY_NOT_FOUND -> UnlockException.Reason.CREDENTIAL_MISSING;
            case KEY_USER_CANCELED, KEY_USER_PREFERS_PASSWORD -> UnlockException.Reason.CANCELED;
            case KEY_ALGORITHM_NOT_SUPPORTED -> UnlockException.Reason.UNSUPPORTED;
            default -> UnlockException.Reason.NATIVE_FAILURE;
        };
        throw new UnlockException(reason,
                operation + " failed with Windows Hello status " + status);
    }

    private static void check(int hresult, String operation) throws UnlockException {
        if (hresult < 0) {
            throw nativeFailure(operation, hresult);
        }
    }

    private static UnlockException nativeFailure(String operation, int hresult) {
        UnlockException.Reason reason = switch (hresult) {
            case E_ABORT, HRESULT_ERROR_CANCELLED -> UnlockException.Reason.CANCELED;
            case NTE_NOT_FOUND -> UnlockException.Reason.CREDENTIAL_MISSING;
            case NTE_BAD_ALGID -> UnlockException.Reason.UNSUPPORTED;
            default -> UnlockException.Reason.NATIVE_FAILURE;
        };
        return new UnlockException(reason,
                operation + " failed (HRESULT 0x" + Integer.toHexString(hresult) + ")");
    }

    private static UnlockException nativeFailure(String operation, Throwable cause) {
        return new UnlockException(UnlockException.Reason.NATIVE_FAILURE,
                operation + " failed", cause);
    }

    private static MethodHandle downcall(SymbolLookup lookup, String symbol,
            FunctionDescriptor descriptor) {
        return LINKER.downcallHandle(lookup.findOrThrow(symbol), descriptor);
    }

    private final class HString implements AutoCloseable {
        private MemorySegment value;

        private HString(MemorySegment value) {
            this.value = value;
        }

        private MemorySegment value() {
            return value;
        }

        @Override
        public void close() {
            if (value.address() == 0) {
                return;
            }
            try {
                int ignored = (int) windowsDeleteString.invokeExact(value);
            } catch (Throwable ignored) {
                // Windows owns no Java-side resources if deleting an HSTRING unexpectedly fails.
            } finally {
                value = MemorySegment.NULL;
            }
        }
    }

    private final class Apartment implements AutoCloseable {
        private final boolean uninitialize;

        private Apartment(boolean uninitialize) {
            this.uninitialize = uninitialize;
        }

        @Override
        public void close() {
            if (!uninitialize) {
                return;
            }
            try {
                roUninitialize.invokeExact();
            } catch (Throwable ignored) {
                // RoUninitialize has no failure result and is best-effort during unwinding.
            }
        }
    }

    private static final class ComPtr implements AutoCloseable {
        private MemorySegment pointer;

        private ComPtr(MemorySegment pointer) {
            this.pointer = pointer;
        }

        private MemorySegment pointer() {
            return pointer;
        }

        private ComPtr query(UUID iid, Arena arena) throws UnlockException {
            MemorySegment output = nullPointer(arena);
            try {
                MethodHandle method = virtualMethod(this, 0,
                        FD_INT_ADDRESS_ADDRESS_ADDRESS);
                int result = (int) method.invokeExact(pointer, guid(arena, iid), output);
                check(result, "Windows Runtime interface lookup");
                return take(output, "Windows Runtime interface lookup");
            } catch (UnlockException e) {
                throw e;
            } catch (Throwable e) {
                throw nativeFailure("Windows Runtime interface lookup", e);
            }
        }

        @Override
        public void close() {
            if (pointer.address() == 0) {
                return;
            }
            try {
                MethodHandle method = virtualMethod(this, 2, FD_INT_ADDRESS);
                int ignored = (int) method.invokeExact(pointer);
            } catch (Throwable ignored) {
                // Releasing during unwinding cannot be recovered from safely.
            } finally {
                pointer = MemorySegment.NULL;
            }
        }
    }
}
