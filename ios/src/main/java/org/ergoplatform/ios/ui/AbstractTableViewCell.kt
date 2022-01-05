package org.ergoplatform.ios.ui

import org.robovm.apple.foundation.NSCoder
import org.robovm.apple.uikit.UITableViewCell
import org.robovm.apple.uikit.UITableViewCellStyle

abstract class AbstractTableViewCell(cellId: String) :
    UITableViewCell(UITableViewCellStyle.Default, cellId) {

    override fun init(p0: NSCoder?): Long {
        val init = super.init(p0)
        setupView()
        return init
    }

    override fun init(p0: UITableViewCellStyle?, p1: String?): Long {
        val init = super.init(p0, p1)
        setupView()
        return init
    }

    abstract fun setupView()

}