package org.ergoplatform.ios.wallet

import com.badlogic.gdx.utils.I18NBundle
import org.ergoplatform.ios.ui.*
import org.ergoplatform.uilogic.*
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.*
import org.robovm.objc.annotation.CustomClass

class AddWalletChooserViewController : UIViewController() {
    override fun viewDidLoad() {
        val texts = getAppDelegate().texts
        title = texts.get(STRING_MENU_ADD_WALLET)
        view.backgroundColor = UIColor.systemBackground()

        val cancelButton = UIBarButtonItem(UIBarButtonSystemItem.Cancel)
        cancelButton.setOnClickListener { this.dismissViewController(true) {} }
        navigationItem.leftBarButtonItem = cancelButton
        navigationController.navigationBar?.tintColor = uiColorErgo

        val chooserView = AddWalletChooserStackView(texts)
        val scrollView = chooserView.wrapInVerticalScrollView()
        view.addSubview(scrollView)
        scrollView.edgesToSuperview()

        chooserView.clickListener = {
            navigationController.pushViewController(RestoreWalletViewController(), true)
        }
    }
}

@CustomClass
class AddWalletChooserStackView(val texts: I18NBundle) : UIView(CGRect.Zero()) {
    var clickListener: ((String) -> Unit)? = null

    init {
        val createWalletCell =
            createCardView(STRING_LABEL_CREATE_WALLET, STRING_DESC_CREATE_WALLET, IMAGE_CREATE_WALLET)
        val restoreWalletCell =
            createCardView(STRING_LABEL_RESTORE_WALLET, STRING_DESC_RESTORE_WALLET, IMAGE_RESTORE_WALLET)
        val readOnlyWalletCell =
            createCardView(STRING_LABEL_READONLY_WALLET, STRING_DESC_READONLY_WALLET, IMAGE_READONLY_WALLET)

        restoreWalletCell.addGestureRecognizer(UITapGestureRecognizer {
            clickListener?.invoke(
                STRING_LABEL_RESTORE_WALLET
            )
        })

        val stackView = UIStackView(NSArray(createWalletCell, restoreWalletCell, readOnlyWalletCell))
        stackView.axis = UILayoutConstraintAxis.Vertical
        stackView.distribution = UIStackViewDistribution.EqualCentering
        addSubview(stackView)
        stackView.edgesToSuperview()
    }

    private fun createCardView(titleLabel: String, descLabelId: String, imageName: String): CardView {
        val cardView = CardView()

        val imageView = UIImageView(
            UIImage.getSystemImage(
                imageName,
                UIImageSymbolConfiguration.getConfigurationWithPointSizeWeightScale(
                    30.0,
                    UIImageSymbolWeight.Regular,
                    UIImageSymbolScale.Large
                )
            )
        )
        imageView.tintColor = UIColor.secondaryLabel()
        imageView.contentMode = UIViewContentMode.Center

        val headlineLabel = Headline2Label()
        headlineLabel.text = texts.get(titleLabel)
        headlineLabel.textColor = uiColorErgo
        val descLabel = Body1Label()
        descLabel.text = texts.get(descLabelId)
        val stackView = UIStackView(
            NSArray(
                headlineLabel,
                descLabel
            )
        )
        stackView.axis = UILayoutConstraintAxis.Vertical

        cardView.contentView.addSubviews(listOf(stackView, imageView))
        imageView.topToSuperview().leftToSuperview().bottomToSuperview()
        NSLayoutConstraint.activateConstraints(NSArray(imageView.widthAnchor.equalTo(75.0)))
        stackView.rightToSuperview(inset = DEFAULT_MARGIN).leftToRightOf(imageView)
            .superViewWrapsHeight(constant = DEFAULT_MARGIN)

        return cardView
    }
}