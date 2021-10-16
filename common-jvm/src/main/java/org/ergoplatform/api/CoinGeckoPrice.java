package org.ergoplatform.api;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

public class CoinGeckoPrice {
    @SerializedName("ergo")
    public HashMap<String, Float> ergoPrice = new HashMap<>();
}
