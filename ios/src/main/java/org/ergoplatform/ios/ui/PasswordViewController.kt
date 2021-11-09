package org.ergoplatform.ios.ui

import org.ergoplatform.uilogic.*
import org.robovm.apple.foundation.NSAttributedString
import org.robovm.apple.uikit.*

object PasswordViewController {
    fun showDialog(
        parentViewController: UIViewController,
        onPasswordEntered: (String?) -> String?,
        showConfirmation: Boolean = false
    ) {
        showDialog(parentViewController, onPasswordEntered, showConfirmation, null, null, null)
    }

    private fun showDialog(
        parentViewController: UIViewController,
        onPasswordEntered: (String?) -> String?, showConfirmation: Boolean, errorMessage: String?,
        password: String?, confirm: String?
    ) {
        val textProvider = getAppDelegate().texts
        val message = if (errorMessage == null) textProvider.get(STRING_HINT_PASSWORD) else errorMessage
        val alertController = UIAlertController(
            textProvider.get(STRING_LABEL_PASSWORD),
            message,
            UIAlertControllerStyle.Alert
        )
        alertController.accessibilityAttributedValue = NSAttributedString()

        alertController.addTextField {
            it.isSecureTextEntry = true
            it.placeholder = textProvider.get(STRING_LABEL_PASSWORD)
            password?.let { pw -> it.text = pw }
        }

        if (showConfirmation) {
            alertController.addTextField {
                it.isSecureTextEntry = true
                it.placeholder = textProvider.get(STRING_LABEL_PASSWORD_CONFIRM)
                confirm?.let { pw -> it.text = pw }
            }
        }

        alertController.addAction(
            UIAlertAction(
                textProvider.get(STRING_LABEL_CANCEL), UIAlertActionStyle.Cancel
            ) {
                // nothing to do here
            }
        )
        if (!showConfirmation && errorMessage != null)
            alertController.addAction(
                // TODO place in strings
                UIAlertAction(
                    "Forgot your password?", UIAlertActionStyle.Default
                ) {
                    // TODO open web site and reopen dialog
                })
        alertController.addAction(
            UIAlertAction(textProvider.get(STRING_ZXING_BUTTON_OK), UIAlertActionStyle.Default) {
                val enteredPassword = alertController.textFields.get(0).text
                val enteredConfirmation = if (showConfirmation) alertController.textFields.get(1).text else null
                val newErrorMessage = if (showConfirmation && !enteredPassword.equals(enteredConfirmation))
                    textProvider.get(STRING_ERR_PASSWORD_CONFIRM) else onPasswordEntered.invoke(enteredPassword)
                if (newErrorMessage != null) {
                    showDialog(
                        parentViewController,
                        onPasswordEntered,
                        showConfirmation,
                        newErrorMessage,
                        enteredPassword,
                        enteredConfirmation
                    )
                }
            }
        )
        parentViewController.presentViewController(alertController, errorMessage == null) {}
    }
}