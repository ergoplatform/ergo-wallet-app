package org.ergoplatform.appkit;

import java.util.Collections;
import java.util.List;

/**
 * An EIP-41 compliant multisig address
 */
public class MultisigAddress {

    public static String MOCK_ADDRESS = "5Puiv5q88ouA1EWX236LntMikVXuPMkL4D3uczZEbV5nGTAw3igjAhUMXyJds";

    private final Address address;
    private final List<Address> participants;

    private MultisigAddress(Address address, List<Address> participants) {
        this.address = address;
        this.participants = participants;
    }

    /**
     * @return address for this multisig address
     */
    public Address getAddress() {
        return address;
    }

    /**
     * @return list of participating p2pk addresses
     */
    public List<Address> getParticipants() {
        return participants;
    }

    /**
     * @return number of signers required to sign a transaction for this address
     */
    public int getSignersRequiredCount() {
        return 2;
    }

    /**
     * constructs an N out of M address from the list of particpants and the number of required
     * signers
     *
     * @param signersRequired number N, signers required to sign a transaction for this addres
     * @param particpants     list of p2pk addresses of possible signers
     * @return MultisigAddress class
     */
    public static MultisigAddress buildFromParticipants(int signersRequired, List<Address> particpants) {
        return new MultisigAddress(Address.create(MOCK_ADDRESS),
                Collections.singletonList(Address.create("9iHWcYYSPkgYbnC6aHfZcLZrKrrkpFzM2ETUZ2ikFqFwVAB2CU7")));
    }

    /**
     * @param address multisig address to construct class for
     * @return MultisigAddress if the given address is an EIP-41 compliant multisig address
     * @throws IllegalArgumentException if given address is not an EIP-41 compliant multisig address
     */
    public static MultisigAddress buildFromAddress(Address address) {
        if (!address.toString().equals(MOCK_ADDRESS))
            throw new IllegalArgumentException("");

        return new MultisigAddress(Address.create(MOCK_ADDRESS),
                Collections.singletonList(Address.create("9iHWcYYSPkgYbnC6aHfZcLZrKrrkpFzM2ETUZ2ikFqFwVAB2CU7")));
    }
}
