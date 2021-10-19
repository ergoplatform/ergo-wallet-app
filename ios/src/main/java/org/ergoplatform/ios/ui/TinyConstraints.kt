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
    widthMatchesSuperview(useSafeArea, leadingInset, trailingInset, maxWidth)

    val layoutGuide = getSuperviewLayoutGuide(useSafeArea)

    val topConstraint = this.topAnchor.equalTo(
        layoutGuide.topAnchor,
        topInset
    )

    val bottomConstraint = this.bottomAnchor.equalTo(
        layoutGuide.bottomAnchor,
        bottomInset * -1.0
    )

    NSLayoutConstraint.activateConstraints(
        NSArray(
            topConstraint,
            bottomConstraint
        )
    )
}

private fun UIView.getSuperviewLayoutGuide(useSafeArea: Boolean) =
    if (useSafeArea) superview.safeAreaLayoutGuide else superview.layoutMarginsGuide

fun UIView.topToSuperview(
    useSafeArea: Boolean = false,
    topInset: Double = 0.0
): UIView {
    setTranslatesAutoresizingMaskIntoConstraints(false)

    val topConstraint = this.topAnchor.equalTo(
        getSuperviewLayoutGuide(useSafeArea).topAnchor,
        topInset
    )
    NSLayoutConstraint.activateConstraints(NSArray(topConstraint))

    return this
}

fun UIView.topToBottomOf(
    sibling: UIView,
    inset: Double = 0.0
): UIView {
    setTranslatesAutoresizingMaskIntoConstraints(false)

    val topConstraint = this.topAnchor.equalTo(
        sibling.bottomAnchor,
        inset
    )
    NSLayoutConstraint.activateConstraints(NSArray(topConstraint))

    return this
}

fun UIView.leftToRightOf(
    sibling: UIView,
    inset: Double = 0.0
): UIView {
    setTranslatesAutoresizingMaskIntoConstraints(false)

    val topConstraint = this.leftAnchor.equalTo(
        sibling.rightAnchor,
        inset
    )
    NSLayoutConstraint.activateConstraints(NSArray(topConstraint))

    return this
}

fun UIView.bottomToSuperview(
    useSafeArea: Boolean = false,
    bottomInset: Double = 0.0
): UIView {
    setTranslatesAutoresizingMaskIntoConstraints(false)

    val topConstraint = this.bottomAnchor.equalTo(
        getSuperviewLayoutGuide(useSafeArea).bottomAnchor,
        bottomInset * -1.0
    )
    NSLayoutConstraint.activateConstraints(NSArray(topConstraint))

    return this
}

fun UIView.leftToSuperview(
    useSafeArea: Boolean = false,
    inset: Double = 0.0
): UIView {
    setTranslatesAutoresizingMaskIntoConstraints(false)

    val topConstraint = this.leftAnchor.equalTo(
        getSuperviewLayoutGuide(useSafeArea).leftAnchor,
        inset
    )
    NSLayoutConstraint.activateConstraints(NSArray(topConstraint))

    return this
}

fun UIView.rightToSuperview(
    useSafeArea: Boolean = false,
    inset: Double = 0.0
): UIView {
    setTranslatesAutoresizingMaskIntoConstraints(false)

    val topConstraint = this.rightAnchor.equalTo(
        getSuperviewLayoutGuide(useSafeArea).rightAnchor,
        inset * -1.0
    )
    NSLayoutConstraint.activateConstraints(NSArray(topConstraint))

    return this
}

fun UIView.superViewWrapsHeight(useSafeArea: Boolean = false, multiplier: Double = 1.0, constant: Double = 0.0) {
    setTranslatesAutoresizingMaskIntoConstraints(false)

    val layoutGuide = getSuperviewLayoutGuide(useSafeArea)

    val topConstraint =
        this.topAnchor.constraintEqualToSystemSpacingBelowAnchor(layoutGuide.topAnchor, multiplier)
    topConstraint.constant = constant

    val bottomConstraint =
        layoutGuide.bottomAnchor.constraintEqualToSystemSpacingBelowAnchor(this.bottomAnchor, multiplier)
    bottomConstraint.constant = constant

    NSLayoutConstraint.activateConstraints(
        NSArray(
            topConstraint,
            bottomConstraint,
        )
    )
}

fun UIView.widthMatchesSuperview(
    useSafeArea: Boolean = false,
    leadingInset: Double = 0.0,
    trailingInset: Double = 0.0,
    maxWidth: Double = 0.0
): UIView {
    setTranslatesAutoresizingMaskIntoConstraints(false)
    val layoutGuide = getSuperviewLayoutGuide(useSafeArea)

    val leadingConstraint = this.leadingAnchor.equalTo(
        layoutGuide.leadingAnchor,
        leadingInset
    )
    val trailingConstraint = this.trailingAnchor.equalTo(
        layoutGuide.trailingAnchor,
        trailingInset * -1.0
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
            leadingConstraint,
            trailingConstraint,
        )
    )

    return this
}

fun UIView.addSubviews(viewsToAdd: List<UIView>) {
    viewsToAdd.forEach { this.addSubview(it) }
}