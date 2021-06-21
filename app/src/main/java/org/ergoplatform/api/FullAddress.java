package org.ergoplatform.api;

import com.google.gson.annotations.SerializedName;

public class FullAddress {

    @SerializedName("transactions")
    public FullAddressTransactions transactions = null;
}
