package org.ergoplatform.api.ergodex;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ErgoDexApi {
    @GET("v1/price-tracking/markets")
    Call<List<ErgoDexSwap>> getSwaps();

}
