package org.ergoplatform.api;

import com.google.gson.annotations.SerializedName;

import org.ergoplatform.explorer.client.model.Asset;

import java.util.List;

public class FullAddressTransactions {
    @SerializedName("confirmed")
    public Integer confirmed = null;

    @SerializedName("totalReceived")
    public Long totalReceived = null;

    @SerializedName("confirmedBalance")
    public Long confirmedBalance = null;

    @SerializedName("totalBalance")
    public Long totalBalance = null;

}
