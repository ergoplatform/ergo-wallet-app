package org.ergoplatform.api.ergodex;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class ErgoDexSwap {
    @SerializedName("id")
    public String id;

    @SerializedName("baseId")
    public String baseId;

    @SerializedName("baseSymbol")
    public String baseDisplayName;

    @SerializedName("quoteId")
    public String tokenId;

    @SerializedName("quoteSymbol")
    public String displayName;

    @SerializedName("lastPrice")
    public BigDecimal lastPrice;

    // traded volume for a given period of time - base symbol
    public ErgoDexSwapVolume baseVolume;

    // traded volume for a given period of time - quote symbol
    public ErgoDexSwapVolume quoteVolume;

    public static class ErgoDexSwapVolume {
        long value;
    }
}
