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

    public ErgoDexSwapVolume baseVolume;

    public static class ErgoDexSwapVolume {
        long value;
    }
}
