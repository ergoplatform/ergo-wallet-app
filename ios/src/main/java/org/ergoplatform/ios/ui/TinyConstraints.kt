package org.ergoplatform.ios.ui

import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.NSLayoutConstraint
import org.robovm.apple.uikit.UIColor
import org.robovm.apple.uikit.UIScrollView
import org.robovm.apple.uikit.UIView

// https://github.com/roberthein/TinyConstraints

const val iosDefaultPriority = 1000f

/**
 * sizes the view to the superview (match_parent, match_parent)
 * If you want to wrap the content vertically, use topToSuperView().bottomToSuperView() concatenation
 * or superviewWrapsHeight()
 */
fun UIView.edgesToSuperview(
    useSafeArea: Boolean = false,
    inset: Double = 0.0,
    maxWidth: Double = 0.0,
    priority: Float = iosDefaultPriority
) {
    widthMatchesSuperview(useSafeArea, inset, maxWidth, priority)

    val layoutGuide = getSuperviewLayoutGuide(useSafeArea)

    val topConstraint = this.topAnchor.equalTo(
        layoutGuide.topAnchor,
        inset
    )

    val bottomConstraint = this.bottomAnchor.equalTo(
        layoutGuide.bottomAnchor,
        inset * -1.0
    )

    topConstraint.priority = priority
    bottomConstraint.priority = priority

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
    topInset: Double = 0.0,
    priority: Int? = null,
    canBeMore: Boolean = false,
): UIView {
    setTranslatesAutoresizingMaskIntoConstraints(false)

    val topConstraint =
        if (canBeMore)
            this.topAnchor.greaterThanOrEqualTo(
                getSuperviewLayoutGuide(useSafeArea).topAnchor,
                topInset
            )
        else
            this.topAnchor.equalTo(
                getSuperviewLayoutGuide(useSafeArea).topAnchor,
                topInset
            )
    priority?.let { topConstraint.priority = priority.toFloat() }
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

fun UIView.bottomToBottomOf(
    sibling: UIView,
    inset: Double = 0.0
): UIView {
    setTranslatesAutoresizingMaskIntoConstraints(false)

    val bottomConstraint = this.bottomAnchor.equalTo(
        sibling.bottomAnchor,
        inset
    )
    NSLayoutConstraint.activateConstraints(NSArray(bottomConstraint))

    return this
}

fun UIView.topToTopOf(
    sibling: UIView,
    inset: Double = 0.0
): UIView {
    setTranslatesAutoresizingMaskIntoConstraints(false)

    val topConstraint = this.topAnchor.equalTo(
        sibling.topAnchor,
        inset
    )
    NSLayoutConstraint.activateConstraints(NSArray(topConstraint))

    return this
}

fun UIView.centerVerticallyTo(
    sibling: UIView,
    inset: Double = 0.0
): UIView {
    setTranslatesAutoresizingMaskIntoConstraints(false)

    val centerConstraint = this.centerYAnchor.equalTo(
        sibling.centerYAnchor,
        inset
    )
    NSLayoutConstraint.activateConstraints(NSArray(centerConstraint))

    return this
}

fun UIView.centerHorizontallyTo(
    sibling: UIView,
    inset: Double = 0.0
): UIView {
    setTranslatesAutoresizingMaskIntoConstraints(false)

    val centerConstraint = this.centerXAnchor.equalTo(
        sibling.centerXAnchor,
        inset
    )
    NSLayoutConstraint.activateConstraints(NSArray(centerConstraint))

    return this
}

fun UIView.bottomToKeyboard(
    vc: ViewControllerWithKeyboardLayoutGuide,
    inset: Double = 0.0
): UIView {
    setTranslatesAutoresizingMaskIntoConstraints(false)

    val bottomConstraint = this.bottomAnchor.equalTo(
        vc.keyboardLayoutGuide.topAnchor,
        inset * -1.0
    )
    NSLayoutConstraint.activateConstraints(NSArray(bottomConstraint))

    return this
}

fun UIView.placeBelow(
    sibling: UIView,
): UIView {
    setTranslatesAutoresizingMaskIntoConstraints(false)

    val topConstraint = this.topAnchor.constraintGreaterThanOrEqualToSystemSpacingBelowAnchor(sibling.bottomAnchor, 1.0)
    NSLayoutConstraint.activateConstraints(NSArray(topConstraint))

    return this
}

fun UIView.leftToRightOf(
    sibling: UIView,
    inset: Double = 0.0
): UIView {
    setTranslatesAutoresizingMaskIntoConstraints(false)

    val leftConstraint = this.leftAnchor.equalTo(
        sibling.rightAnchor,
        inset
    )
    NSLayoutConstraint.activateConstraints(NSArray(leftConstraint))

    return this
}

fun UIView.leftToLeftOf(
    sibling: UIView,
    inset: Double = 0.0
): UIView {
    setTranslatesAutoresizingMaskIntoConstraints(false)

    val leftConstraint = this.leftAnchor.equalTo(
        sibling.leftAnchor,
        inset
    )
    NSLayoutConstraint.activateConstraints(NSArray(leftConstraint))

    return this
}

fun UIView.rightToLeftOf(
    sibling: UIView,
    inset: Double = 0.0
): UIView {
    setTranslatesAutoresizingMaskIntoConstraints(false)

    val topConstraint = this.rightAnchor.equalTo(
        sibling.leftAnchor,
        -inset
    )
    NSLayoutConstraint.activateConstraints(NSArray(topConstraint))

    return this
}

fun UIView.rightToRightOf(
    sibling: UIView,
    inset: Double = 0.0
): UIView {
    setTranslatesAutoresizingMaskIntoConstraints(false)

    val topConstraint = this.rightAnchor.equalTo(
        sibling.rightAnchor,
        -inset
    )
    NSLayoutConstraint.activateConstraints(NSArray(topConstraint))

    return this
}

fun UIView.bottomToSuperview(
    useSafeArea: Boolean = false,
    bottomInset: Double = 0.0,
    canBeLess: Boolean = false
): UIView {
    setTranslatesAutoresizingMaskIntoConstraints(false)

    val topConstraint =
        if (canBeLess)
            this.bottomAnchor.lessThanOrEqualTo(
                getSuperviewLayoutGuide(useSafeArea).bottomAnchor,
                bottomInset * -1.0
            )
        else
            this.bottomAnchor.equalTo(
                getSuperviewLayoutGuide(useSafeArea).bottomAnchor,
                bottomInset * -1.0
            )
    NSLayoutConstraint.activateConstraints(NSArray(topConstraint))

    return this
}

fun UIView.leftToSuperview(
    useSafeArea: Boolean = false,
    inset: Double = 0.0,
    canBeMore: Boolean = false
): UIView {
    setTranslatesAutoresizingMaskIntoConstraints(false)

    val topConstraint =
        if (canBeMore)
            this.leftAnchor.greaterThanOrEqualTo(
                getSuperviewLayoutGuide(useSafeArea).leftAnchor,
                inset
            )
        else
            this.leftAnchor.equalTo(
                getSuperviewLayoutGuide(useSafeArea).leftAnchor,
                inset
            )
    NSLayoutConstraint.activateConstraints(NSArray(topConstraint))

    return this
}

fun UIView.rightToSuperview(
    useSafeArea: Boolean = false,
    inset: Double = 0.0,
    canBeLess: Boolean = false
): UIView {
    setTranslatesAutoresizingMaskIntoConstraints(false)

    val rightConstraint =
        if (canBeLess)
            this.rightAnchor.lessThanOrEqualTo(
                getSuperviewLayoutGuide(useSafeArea).rightAnchor,
                inset * -1.0
            )
        else
            this.rightAnchor.equalTo(
                getSuperviewLayoutGuide(useSafeArea).rightAnchor,
                inset * -1.0
            )
    NSLayoutConstraint.activateConstraints(NSArray(rightConstraint))

    return this
}

fun UIView.centerInSuperviewWhenSmaller(useSafeArea: Boolean = false, multiplier: Double = 1.0) {
    centerVertical()
    val layoutGuide = getSuperviewLayoutGuide(useSafeArea)
    val topConstraint =
        this.topAnchor.greaterThanOrEqualTo(layoutGuide.topAnchor, multiplier)
    val bottomConstraint =
        layoutGuide.bottomAnchor.greaterThanOrEqualTo(this.bottomAnchor, multiplier)
    NSLayoutConstraint.activateConstraints(
        NSArray(
            topConstraint,
            bottomConstraint,
        )
    )
}

fun UIView.superViewWrapsHeight(
    useSafeArea: Boolean = false, multiplier: Double = 1.0, constant: Double = 0.0
): UIView {
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

    return this
}

fun UIView.centerVertical(priority: Float = iosDefaultPriority): UIView {
    setTranslatesAutoresizingMaskIntoConstraints(false)
    val centerConstraint = this.centerYAnchor.equalTo(superview.centerYAnchor)
    centerConstraint.priority = priority
    NSLayoutConstraint.activateConstraints(NSArray(centerConstraint))
    return this
}

fun UIView.centerHorizontal(superViewRestrictsWidth: Boolean = false): UIView {
    setTranslatesAutoresizingMaskIntoConstraints(false)
    val centerConstraint = this.centerXAnchor.equalTo(superview.centerXAnchor)
    centerConstraint.priority = iosDefaultPriority
    if (superViewRestrictsWidth) {
        val widthConstraint = this.widthAnchor.lessThanOrEqual(superview.layoutMarginsGuide.widthAnchor)
        widthConstraint.priority = iosDefaultPriority
        NSLayoutConstraint.activateConstraints(NSArray(centerConstraint, widthConstraint))
    } else {
        NSLayoutConstraint.activateConstraints(NSArray(centerConstraint))
    }
    return this
}

fun UIView.fixedWidth(c: Double, priority: Float? = null): UIView {
    setTranslatesAutoresizingMaskIntoConstraints(false)
    val widthConstraint = this.widthAnchor.equalTo(c)
    priority?.let { widthConstraint.priority = priority }
    NSLayoutConstraint.activateConstraints(NSArray(widthConstraint))
    return this
}

fun UIView.fixedHeight(c: Double, priority: Float? = null): UIView {
    setTranslatesAutoresizingMaskIntoConstraints(false)
    val heightConstraint = this.heightAnchor.equalTo(c)
    priority?.let { heightConstraint.priority = priority }
    NSLayoutConstraint.activateConstraints(NSArray(heightConstraint))
    return this
}

fun UIView.minHeight(c: Double): UIView {
    setTranslatesAutoresizingMaskIntoConstraints(false)
    NSLayoutConstraint.activateConstraints(NSArray(this.heightAnchor.greaterThanOrEqualTo(c)))
    return this
}

fun UIView.minWidth(c: Double): UIView {
    setTranslatesAutoresizingMaskIntoConstraints(false)
    NSLayoutConstraint.activateConstraints(NSArray(this.widthAnchor.greaterThanOrEqualTo(c)))
    return this
}

fun UIView.widthMatchesWidthOf(sibling: UIView, factor: Double): UIView {
    setTranslatesAutoresizingMaskIntoConstraints(false)
    NSLayoutConstraint.activateConstraints(
        NSArray(this.widthAnchor.equalTo(sibling.widthAnchor, factor))
    )
    return this
}

fun UIView.heightMatchesHeightOf(sibling: UIView, factor: Double): UIView {
    setTranslatesAutoresizingMaskIntoConstraints(false)
    NSLayoutConstraint.activateConstraints(
        NSArray(this.heightAnchor.equalTo(sibling.heightAnchor, factor))
    )
    return this
}

fun UIView.widthMatchesSuperview(
    useSafeArea: Boolean = false,
    inset: Double = 0.0,
    maxWidth: Double = 0.0,
    priority: Float = iosDefaultPriority
): UIView {
    setTranslatesAutoresizingMaskIntoConstraints(false)
    val layoutGuide = getSuperviewLayoutGuide(useSafeArea)

    val leadingConstraint = this.leadingAnchor.equalTo(
        layoutGuide.leadingAnchor,
        inset
    )
    val trailingConstraint = this.trailingAnchor.equalTo(
        layoutGuide.trailingAnchor,
        inset * -1.0
    )


    if (maxWidth > 0) {
        val widthConstraint = this.widthAnchor.lessThanOrEqualTo(maxWidth)
        widthConstraint.priority = priority
        leadingConstraint.priority = priority - 50f
        trailingConstraint.priority = priority - 50f
        val centerConstraint = this.centerXAnchor.equalTo(superview.centerXAnchor)
        centerConstraint.priority = priority - 1f
        NSLayoutConstraint.activateConstraints(NSArray(widthConstraint, centerConstraint))
    } else {
        leadingConstraint.priority = priority
        trailingConstraint.priority = priority
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

fun UIView.wrapInVerticalScrollView(centerContent: Boolean = false): UIScrollView {
    val scrollView = UIScrollView(CGRect.Zero())
    scrollView.addSubview(this)
    this.setTranslatesAutoresizingMaskIntoConstraints(false)
    NSLayoutConstraint.activateConstraints(
        NSArray(
            this.widthAnchor.equalTo(scrollView.widthAnchor),
            this.trailingAnchor.equalTo(scrollView.trailingAnchor),
            this.leadingAnchor.equalTo(scrollView.leadingAnchor),
            this.topAnchor.equalTo(scrollView.topAnchor),
            this.bottomAnchor.equalTo(scrollView.bottomAnchor),
        )
    )

    if (centerContent) {
        this.centerVertical(250f)
    }

    return scrollView
}

fun UIView.wrapInHorizontalPager(width: Double): UIScrollView {
    val scrollView = UIScrollView(CGRect.Zero())
    scrollView.addSubview(this)
    scrollView.isPagingEnabled = true
    scrollView.fixedWidth(width)
    this.setTranslatesAutoresizingMaskIntoConstraints(false)
    NSLayoutConstraint.activateConstraints(
        NSArray(
            this.heightAnchor.equalTo(scrollView.heightAnchor),
            this.trailingAnchor.equalTo(scrollView.trailingAnchor),
            this.leadingAnchor.equalTo(scrollView.leadingAnchor),
            this.topAnchor.equalTo(scrollView.topAnchor),
            this.bottomAnchor.equalTo(scrollView.bottomAnchor),
        )
    )

    return scrollView
}

fun createHorizontalSeparator(): UIView {
    val separator = UIView(CGRect.Zero())
    separator.backgroundColor = UIColor.systemGray()
    NSLayoutConstraint.activateConstraints(NSArray(separator.heightAnchor.equalTo(1.0)))
    return separator
}