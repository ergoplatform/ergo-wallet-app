import org.ergoplatform.appkit.Address;
import org.ergoplatform.appkit.NetworkType;

public class StageConstants {
    public static final NetworkType NETWORK_TYPE = NetworkType.TESTNET;

    public static final String EXPLORER_WEB_ADDRESS = "https://testnet.ergoplatform.com/";
    public static final String EXPLORER_API_ADDRESS = "https://api-testnet.ergoplatform.com/api/";

    public static final String NODE_API_ADDRESS = "http://213.239.193.208:9052/";

    public static boolean isValidNetworkTypeAddress(Address address) {
        return !address.isMainnet();
    }
}
