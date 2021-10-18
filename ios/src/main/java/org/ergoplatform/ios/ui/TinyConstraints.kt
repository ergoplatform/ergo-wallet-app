package org.ergoplatform.ios.ui

import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.NSLayoutConstraint
import org.robovm.apple.uikit.UIView

// https://github.com/roberthein/TinyConstraints

fun UIView.edgesToSuperview(
    useSafeArea: Boolean = false,
    topInset: Double = 0.0,
    leadingInset: Double = 0.0,
    trailingInset: Double = 0.0,
    bottomInset: Double = 0.0,
    maxWidth: Double = 0.0
) {
    setTranslatesAutoresizingMaskIntoConstraints(false)
    val layoutGuide =
        if (useSafeArea) superview.safeAreaLayoutGuide else superview.layoutMarginsGuide

    val topConstraint = this.topAnchor.equalTo(
        layoutGuide.topAnchor,
        topInset
    )

    val leadingConstraint = this.leadingAnchor.equalTo(
        layoutGuide.leadingAnchor,
        leadingInset
    )
    val trailingConstraint = this.trailingAnchor.equalTo(
        layoutGuide.trailingAnchor,
        trailingInset * -1.0
    )
    val bottomConstraint = this.bottomAnchor.equalTo(
        layoutGuide.bottomAnchor,
        bottomInset * -1.0
    )

    if (maxWidth > 0) {
        val widthConstraint = this.widthAnchor.lessThanOrEqualTo(maxWidth)
        widthConstraint.priority = 1000f
        leadingConstraint.priority = 950f
        trailingConstraint.priority = 950f
        val centerConstraint = this.centerXAnchor.equalTo(superview.centerXAnchor)
        centerConstraint.priority = 999f
        NSLayoutConstraint.activateConstraints(NSArray(widthConstraint, centerConstraint))
    }

    NSLayoutConstraint.activateConstraints(
        NSArray(
            topConstraint,
            leadingConstraint,
            trailingConstraint,
            bottomConstraint
        )
    )
}