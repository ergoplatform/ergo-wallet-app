package org.ergoplatform.api;

import com.google.gson.annotations.SerializedName;

import org.ergoplatform.explorer.client.model.FullAddressSummary;

public class FullAddress {
    @SerializedName("summary")
    public FullAddressSummary summary = null;

    @SerializedName("transactions")
    public FullAddressTransactions transactions = null;
}
