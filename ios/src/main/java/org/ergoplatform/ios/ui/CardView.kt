package org.ergoplatform.ios.ui

import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.coregraphics.CGSize
import org.robovm.apple.foundation.NSCoder
import org.robovm.apple.uikit.UIColor
import org.robovm.apple.uikit.UIEdgeInsets
import org.robovm.apple.uikit.UIView

class CardView: UIView(CGRect.Zero()) {
    private lateinit var shadowView: UIView
    lateinit var contentView: UIView

    override fun init(p0: CGRect?): Long {
        val init = super.init(p0)
        setupView()
        return init
    }

    override fun init(p0: NSCoder?): Long {
        val init = super.init(p0)
        setupView()
        return init
    }

    private fun setupView() {
        shadowView = UIView(CGRect.Zero())
        contentView = UIView(CGRect.Zero())

        shadowView.apply {
            this.layer.setMasksToBounds(false)
            this.backgroundColor = UIColor.clear()
            this.layer.shadowColor = UIColor.black().cgColor
            this.layer.shadowOffset = CGSize(0.0, 0.0)
            this.layer.shadowOpacity = 0.1f
            this.layer.shadowRadius = 7.0
        }

        contentView.apply {
            this.backgroundColor = UIColor.white()
            this.layer.setMasksToBounds(true)
            this.layer.cornerRadius = 4.0
        }

        this.setClipsToBounds(false)

        this.addSubview(shadowView)
        layoutMargins = UIEdgeInsets.Zero()
        shadowView.addSubview(contentView)
        shadowView.widthMatchesSuperview().superViewWrapsHeight()
        contentView.widthMatchesSuperview().superViewWrapsHeight()
    }
}