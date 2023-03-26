package org.ergoplatform.appkit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.ergoplatform.ErgoFacade;
import org.ergoplatform.utils.Base64Coder;

import java.util.ArrayList;
import java.util.List;

/**
 * EIP-41/EIP-11 compliant multi sig transaction
 */
public class MockedMultisigTransaction extends MultisigTransaction {

    ReducedTransaction tx;

    @Override
    public ReducedTransaction getTransaction() {
        return tx;
    }

    @Override
    public void addHint(ErgoProver prover) {

    }

    @Override
    public void mergeHints(MultisigTransaction other) {

    }

    @Override
    public void mergeHints(String json) {

    }

    @Override
    public List<Address> getCommitingParticipants() {
        return new ArrayList<>();
    }

    @Override
    public boolean isHintBagComplete() {
        return false;
    }

    @Override
    public SignedTransaction toSignedTransaction() {
        return null;
    }

    @Override
    public String hintsToJson() {
        return null;
    }

    /**
     * @return EIP-11 compliant json string to transfer the partially signed transaction to the
     * next particpant
     */
    public String toJson() {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        JsonObject root = new JsonObject();
        root.addProperty("reduced", new String(Base64Coder.encode(tx.toBytes())));
        return gson.toJson(root);
    }

    /**
     * constructs a multi sig transaction from a reduced transaction
     */
    public static MockedMultisigTransaction fromTransaction(ReducedTransaction transaction, MultisigAddress address) {
        MockedMultisigTransaction mtx = new MockedMultisigTransaction();
        mtx.tx = transaction;
        return mtx;
    }

    public static MockedMultisigTransaction fromJson(String json) {
        JsonObject object = new JsonParser().parse(json).getAsJsonObject();
        MockedMultisigTransaction mtx = new MockedMultisigTransaction();
        mtx.tx = ErgoFacade.INSTANCE.deserializeUnsignedTxOffline(Base64Coder.decode(object.get("reduced").getAsString()));
        return mtx;
    }
}
