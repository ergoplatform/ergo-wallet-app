import org.ergoplatform.appkit.Address;
import org.ergoplatform.appkit.NetworkType;

public class StageConstants {
    public static final NetworkType NETWORK_TYPE = NetworkType.MAINNET;

    public static final String EXPLORER_WEB_ADDRESS = "https://www.ergoplatform.com/";
    public static final String EXPLORER_API_ADDRESS = "https://api.ergoplatform.com/api/";

    public static boolean isValidNetworkTypeAddress(Address address) {
        return address.isMainnet();
    }
}
