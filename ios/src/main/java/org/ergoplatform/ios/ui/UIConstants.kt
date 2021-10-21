package org.ergoplatform.ios.ui

import org.ergoplatform.ios.Main
import org.robovm.apple.uikit.UIApplication
import org.robovm.apple.uikit.UIColor

const val MAX_WIDTH = 500.0
const val DEFAULT_MARGIN = 6.0

const val IMAGE_WALLET = "rectangle.on.rectangle.angled"
const val IMAGE_SETTINGS = "gear"
const val IMAGE_CREATE_WALLET = "folder.badge.plus"
const val IMAGE_RESTORE_WALLET = "arrow.clockwise"
const val IMAGE_READONLY_WALLET = "magnifyingglass"

const val FONT_SIZE_BODY1 = 18.0

fun getAppDelegate() = UIApplication.getSharedApplication().delegate as Main

// See https://developer.apple.com/design/human-interface-guidelines/ios/visual-design/color/#system-colors

val uiColorErgo get() = UIColor.systemRed()