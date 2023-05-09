package org.ergoplatform.ios.ui

import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.UIEdgeInsets
import org.robovm.apple.uikit.UISwitch
import org.robovm.apple.uikit.UIView

class UiSwitchWithLabel(labelText: String) : UIView() {
    val switch = UISwitch(CGRect.Zero())
    val label = Body1Label()

    init {
        layoutMargins = UIEdgeInsets.Zero()
        label.text = labelText
        addSubview(switch)
        addSubview(label)
        // Fixed width needed on UISwitch, does not support auto layout out of the box...
        switch.leftToSuperview().centerVertical().rightToLeftOf(label, DEFAULT_MARGIN * 2)
            .fixedWidth(50.0)
        label.topToSuperview().bottomToSuperview().rightToSuperview()
    }
}