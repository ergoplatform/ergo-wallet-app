package org.ergoplatform.ios.transactions

import com.badlogic.gdx.utils.I18NBundle
import org.ergoplatform.ios.tokens.GenuineImageContainer
import org.ergoplatform.ios.tokens.ThumbnailContainer
import org.ergoplatform.ios.tokens.TokenFilterButton
import org.ergoplatform.ios.ui.AbstractTableViewCell
import org.ergoplatform.ios.ui.Body1Label
import org.ergoplatform.ios.ui.Body2Label
import org.ergoplatform.ios.ui.DEFAULT_MARGIN
import org.ergoplatform.ios.ui.Headline2Label
import org.ergoplatform.ios.ui.IosStringProvider
import org.ergoplatform.ios.ui.addCloseButton
import org.ergoplatform.ios.ui.bottomToSuperview
import org.ergoplatform.ios.ui.centerHorizontal
import org.ergoplatform.ios.ui.getAppDelegate
import org.ergoplatform.ios.ui.leftToRightOf
import org.ergoplatform.ios.ui.leftToSuperview
import org.ergoplatform.ios.ui.rightToSuperview
import org.ergoplatform.ios.ui.topToBottomOf
import org.ergoplatform.ios.ui.topToSuperview
import org.ergoplatform.ios.ui.widthMatchesSuperview
import org.ergoplatform.persistance.TokenInformation
import org.ergoplatform.persistance.WalletToken
import org.ergoplatform.uilogic.STRING_TITLE_ADD_TOKEN
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.uilogic.tokens.TokenEntryViewUiLogic
import org.ergoplatform.uilogic.transactions.SendFundsUiLogic
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.foundation.NSIndexPath
import org.robovm.apple.uikit.NSLineBreakMode
import org.robovm.apple.uikit.NSTextAlignment
import org.robovm.apple.uikit.UIColor
import org.robovm.apple.uikit.UILayoutConstraintAxis
import org.robovm.apple.uikit.UIStackView
import org.robovm.apple.uikit.UITableView
import org.robovm.apple.uikit.UITableViewCell
import org.robovm.apple.uikit.UITableViewCellSeparatorStyle
import org.robovm.apple.uikit.UITableViewDataSourceAdapter
import org.robovm.apple.uikit.UITapGestureRecognizer
import org.robovm.apple.uikit.UIViewController

const val TOKEN_CELL = "TOKEN_CELL"

/**
 * Let the user choose one or more token(s) from a list of tokens
 */
class ChooseTokenListViewController(
    val uiLogic: SendFundsUiLogic,
    val onChoose: (String) -> Unit
) : UIViewController() {

    private val texts = getAppDelegate().texts
    private var tokensToChooseFrom = uiLogic.getTokensToChooseFrom()
    private val tokenInfoMap = uiLogic.tokensInfo

    override fun viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = UIColor.systemBackground()
        addCloseButton()

        val titleLabel = Body1Label().apply {
            text = texts.get(STRING_TITLE_ADD_TOKEN)
            textAlignment = NSTextAlignment.Center
        }

        val filterButton = TokenFilterButton(uiLogic)

        view.addSubview(titleLabel)
        view.addSubview(filterButton)

        titleLabel.topToSuperview(topInset = DEFAULT_MARGIN * 2)
            .leftToSuperview(inset = DEFAULT_MARGIN)
        filterButton.topToSuperview(topInset = DEFAULT_MARGIN).rightToSuperview()
            .leftToRightOf(titleLabel, inset = DEFAULT_MARGIN)

        val tableView = UITableView(CGRect.Zero())
        view.addSubview(tableView)

        tableView.apply {
            widthMatchesSuperview().bottomToSuperview().topToBottomOf(titleLabel, DEFAULT_MARGIN * 2)

            dataSource = TokenDataSource()
            separatorStyle = UITableViewCellSeparatorStyle.None
            registerReusableCellClass(TokenCell::class.java, TOKEN_CELL)
            rowHeight = UITableView.getAutomaticDimension()
            estimatedRowHeight = UITableView.getAutomaticDimension()
        }

        filterButton.onChange = {
            tokensToChooseFrom = uiLogic.getTokensToChooseFrom()
            tableView.reloadData()
        }
    }

    inner class TokenDataSource : UITableViewDataSourceAdapter() {
        override fun getNumberOfRowsInSection(p0: UITableView?, p1: Long): Long {
            return tokensToChooseFrom.size.toLong()
        }

        override fun getCellForRow(p0: UITableView, p1: NSIndexPath): UITableViewCell {
            val cell = p0.dequeueReusableCell(TOKEN_CELL)
            (cell as? TokenCell)?.let {
                val walletToken = tokensToChooseFrom[p1.row]
                it.bind(walletToken, texts, tokenInfoMap[walletToken.tokenId])
                it.clickListener = { tokenId ->
                    onChoose.invoke(tokenId)
                    dismissViewController(true) {}
                }
            }
            return cell
        }

        override fun getNumberOfSections(p0: UITableView?): Long {
            return 1
        }

        override fun canEditRow(p0: UITableView?, p1: NSIndexPath?): Boolean {
            return false
        }

        override fun canMoveRow(p0: UITableView?, p1: NSIndexPath?): Boolean {
            return false
        }
    }

    class TokenCell : AbstractTableViewCell(TOKEN_CELL) {
        var clickListener: ((tokenId: String) -> Unit)? = null
        private lateinit var nameLabel: Headline2Label
        private lateinit var tokenIdLabel: Body2Label
        private lateinit var genuineImageContainer: GenuineImageContainer
        private lateinit var thumbnailContainer: ThumbnailContainer
        private var token: WalletToken? = null

        override fun setupView() {
            nameLabel = Headline2Label().apply {
                numberOfLines = 1
                textAlignment = NSTextAlignment.Center
            }
            tokenIdLabel = Body2Label().apply {
                numberOfLines = 1
                textAlignment = NSTextAlignment.Center
                lineBreakMode = NSLineBreakMode.TruncatingMiddle
                textColor = UIColor.secondaryLabel()
            }
            genuineImageContainer = GenuineImageContainer()
            thumbnailContainer = ThumbnailContainer()
            val topStack = UIStackView(NSArray(thumbnailContainer, nameLabel, genuineImageContainer)).apply {
                axis = UILayoutConstraintAxis.Horizontal
            }
            contentView.addSubview(topStack)
            contentView.addSubview(tokenIdLabel)
            topStack.topToSuperview().centerHorizontal(true)
            tokenIdLabel.topToBottomOf(nameLabel, inset = DEFAULT_MARGIN / 2).widthMatchesSuperview().bottomToSuperview()
            contentView.addGestureRecognizer(UITapGestureRecognizer {
                token?.tokenId?.let { tokenId -> clickListener?.invoke(tokenId) }
            })
        }

        fun bind(walletToken: WalletToken, texts: I18NBundle, tokenInformation: TokenInformation?) {
            val viewLogic = object : TokenEntryViewUiLogic(
                walletToken, noIdWhenPriceAvail = false
            ) {
                override val texts: StringProvider
                    get() = IosStringProvider(texts)

                override fun setDisplayedTokenName(tokenName: String) {
                    nameLabel.text = tokenName
                }

                override fun setDisplayedTokenId(tokenId: String?) {
                    tokenIdLabel.text = tokenId ?: ""
                }

                override fun setDisplayedBalance(value: String?) {
                    // not shown
                }

                override fun setDisplayedPrice(price: String?) {
                    // not shown
                }

                override fun setGenuineFlag(genuineFlag: Int) {
                    genuineImageContainer.setGenuineFlag(genuineFlag)
                }

                override fun setThumbnail(thumbnailType: Int) {
                    thumbnailContainer.setThumbnail(thumbnailType)
                }

            }

            token = walletToken
            viewLogic.bind(tokenInformation)
        }
    }
}