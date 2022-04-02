package org.ergoplatform.api.ergodex;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ErgoDexApi {
    @GET("v1/amm/markets")
    Call<List<ErgoDexSwap>> getSwaps();

}
