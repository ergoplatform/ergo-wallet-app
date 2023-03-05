package org.ergoplatform.appkit;

import java.util.List;

/**
 * EIP-41/EIP-11 compliant multi sig transaction
 */
public class MultisigTransaction {

    /**
     * @return transaction that is going to be signed
     */
    public Transaction getTransaction() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return multisig address this transaction was created for
     */
    public MultisigAddress getMultisigAddress() {
        throw new UnsupportedOperationException();
    }

    /**
     * adds a new commitment to this multisig transaction
     * @param prover to add commitment for
     */
    public void addCommitment(ErgoProver prover) {
        throw new UnsupportedOperationException();
    }

    /**
     * adds the commitments not present on this instance from another multisig transaction instance
     * for the same transaction.
     */
    public void addCommitments(MultisigTransaction other) {
        throw new UnsupportedOperationException();
    }

    /**
     * @return list of participants that added a commitment for the transaction
     */
    public List<Address> getCommitingParticipants() {
        throw new UnsupportedOperationException();
    }

    public boolean hasEnoughCommitments() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the signed transaction if enough commitments are available
     * @throws IllegalStateException if {@link #hasEnoughCommitments()} is false
     */
    public SignedTransaction toSignedTransaction() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return EIP-11 compliant json string to transfer the partially signed transaction to the
     * next particpant
     */
    public String toJson() {
        throw new UnsupportedOperationException();
    }

    /**
     * constructs a multi sig transaction from an unsigned transaction. The first multi sig address
     * in input boxes is used.
     */
    public static MultisigTransaction fromTransaction(UnsignedTransaction transaction) {
        throw new UnsupportedOperationException();
    }

    /**
     * constructs a multi sig transaction from a reduced transaction
     */
    public static MultisigTransaction fromTransaction(ReducedTransaction transaction, MultisigAddress address) {
        throw new UnsupportedOperationException();
    }

    /**
     * constructs a multi sig transaction from EIP-11 json string
     */
    public static MultisigTransaction fromJson(String json) {
        throw new UnsupportedOperationException();
    }
}
