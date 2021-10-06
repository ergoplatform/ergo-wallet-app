package org.ergoplatform.ios;

import org.ergoplatform.appkit.*;
import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.foundation.Foundation;
import org.robovm.apple.foundation.NSString;
import org.robovm.apple.uikit.*;

public class MyViewController extends UIViewController {
    public static final NetworkType NETWORK_TYPE = NetworkType.TESTNET;

    public static final String EXPLORER_WEB_ADDRESS = "https://testnet.ergoplatform.com/";
    public static final String EXPLORER_API_ADDRESS = RestApiErgoClient.defaultTestnetExplorerUrl + "/";

    public static final String NODE_API_ADDRESS = "http://213.239.193.208:9052/";
    public static final String MNENOMIC = "slow silly start wash bundle suffer bulb ancient height spin express remind today effort helmet";

    private final UIButton button;
    private final UILabel label;
    private int clickCount;

    public MyViewController() {
        // Get the view of this view controller.
        UIView view = getView();

        // Setup background.
        view.setBackgroundColor(UIColor.white());

        // Setup label.
        label = new UILabel(new CGRect(20, 250, 280, 44));
        label.setFont(UIFont.getSystemFont(24));
        label.setTextAlignment(NSTextAlignment.Center);
        view.addSubview(label);

        // Setup button.
        button = new UIButton(UIButtonType.RoundedRect);
        button.setFrame(new CGRect(110, 150, 100, 40));
        button.setTitle("Click me!", UIControlState.Normal);
        button.getTitleLabel().setFont(UIFont.getBoldSystemFont(22));

        button.addOnTouchUpInsideListener((control, event) -> doStuff());
        view.addSubview(button);

    }

    private void doStuff() {
        clickCount++;
        Address address = Address.createEip3Address(0, NetworkType.TESTNET, SecretString.create(MNENOMIC), null);
        if (clickCount == 1) {
            label.setText(address.toString());
        } else {
            ErgoClient ergoClient = RestApiErgoClient.create(NODE_API_ADDRESS, NetworkType.TESTNET, "", EXPLORER_API_ADDRESS);
            ergoClient.execute(ctx -> {
                ErgoProver prover = ctx.newProverBuilder()
                        .withMnemonic(
                                SecretString.create(MNENOMIC),
                                SecretString.create("")
                        )
                        .withEip3Secret(0)
                        .build();
                String jsonTransaction = BoxOperations.send(ctx, prover, true, address, 1 * 1000L * 1000L * 1000L);
                Foundation.log("%@", new NSString(jsonTransaction));
                label.setText(jsonTransaction);
                return jsonTransaction;
            });
        }
    }
}
