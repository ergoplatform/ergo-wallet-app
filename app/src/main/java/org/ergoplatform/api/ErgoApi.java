package org.ergoplatform.api;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ErgoApi {
    // we need this as long as there are bugs in AppKit's API, see https://github.com/ergoplatform/ergo-appkit/issues/80

    @GET("api/v0/addresses/{id}")
    Call<FullAddress> addressesIdGet(
            @retrofit2.http.Path("id") String id
    );
}
