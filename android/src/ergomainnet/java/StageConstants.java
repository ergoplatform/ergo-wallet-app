import org.ergoplatform.appkit.Address;
import org.ergoplatform.appkit.NetworkType;
import org.ergoplatform.appkit.RestApiErgoClient;

public class StageConstants {
    public static final NetworkType NETWORK_TYPE = NetworkType.MAINNET;

    public static final String EXPLORER_WEB_ADDRESS = "https://explorer.ergoplatform.com/";
    public static final String EXPLORER_API_ADDRESS = RestApiErgoClient.defaultMainnetExplorerUrl + "/";

    public static final String NODE_API_ADDRESS = "http://213.239.193.208:9053/";
}
