import org.esoul.surpass.crypto.api.ContextAwareCryptoServiceAbstractFactory;
import org.esoul.surpass.crypto.api.CryptoService;
import org.esoul.surpass.hello.api.DefaultVaultUnlockService;
import org.esoul.surpass.hello.api.VaultUnlockService;

module surpass.hello {
    requires transitive surpass.api;

    exports org.esoul.surpass.hello.api;

    uses CryptoService;
    uses ContextAwareCryptoServiceAbstractFactory;

    provides VaultUnlockService with DefaultVaultUnlockService;
}
