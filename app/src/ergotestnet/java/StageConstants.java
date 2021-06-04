import org.ergoplatform.appkit.Address;
import org.ergoplatform.appkit.NetworkType;

public class StageConstants {
    public static final NetworkType NETWORK_TYPE = NetworkType.TESTNET;

    public static boolean isValidNetworkTypeAddress(Address address) {
        return !address.isMainnet();
    }
}
