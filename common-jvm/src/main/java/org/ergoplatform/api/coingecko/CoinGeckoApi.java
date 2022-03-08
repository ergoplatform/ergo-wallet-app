package org.ergoplatform.api.coingecko;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface CoinGeckoApi {
    @GET("api/v3/simple/price?ids=ergo")
    Call<CoinGeckoPrice> currencyGetPrice(
            @Query("vs_currencies") String currency
    );

    @GET("api/v3/simple/supported_vs_currencies")
    Call<List<String>> getCurrencies();
}
