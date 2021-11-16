package org.ergoplatform.ios.ui

import org.ergoplatform.URL_FORGOT_PASSWORD_HELP
import org.ergoplatform.uilogic.*
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
        val message = errorMessage ?: textProvider.get(STRING_HINT_PASSWORD)
        val alertController = UIAlertController(
            textProvider.get(STRING_LABEL_PASSWORD),
            message,
            UIAlertControllerStyle.Alert
        )

        var textField1: UITextField? = null
        var textField2: UITextField? = null
        var defaultColor: UIColor? = null

        alertController.addTextField {
            it.isSecureTextEntry = true
            it.placeholder = textProvider.get(STRING_LABEL_PASSWORD)
            password?.let { pw -> it.text = pw }
            defaultColor = it.textColor
            if (errorMessage != null && !showConfirmation) {
                it.textColor = UIColor.systemRed()
            }
            textField1 = it
        }

        if (showConfirmation) {
            alertController.addTextField {
                it.isSecureTextEntry = true
                it.placeholder = textProvider.get(STRING_LABEL_PASSWORD_CONFIRM)
                confirm?.let { pw -> it.text = pw }
                if (errorMessage != null) {
                    it.textColor = UIColor.systemRed()
                }
                textField2 = it
            }
        }

        val changeListener: ((UIControl) -> Unit) = {
            (it as? UITextField)?.textColor = defaultColor ?: UIColor.label()
            alertController.actions.last().isEnabled =
                (textField1?.text?.isNotEmpty() ?: true) && (textField2?.text?.isNotEmpty() ?: true)
        }

        textField1?.addOnEditingChangedListener(changeListener)
        textField2?.addOnEditingChangedListener(changeListener)

        alertController.addAction(
            UIAlertAction(
                textProvider.get(STRING_LABEL_CANCEL), UIAlertActionStyle.Cancel
            ) {
                // nothing to do here
            }
        )
        if (!showConfirmation && errorMessage != null)
            alertController.addAction(
                UIAlertAction(
                    textProvider.get(STRING_LABEL_FORGOT_PASSWORD), UIAlertActionStyle.Default
                ) {
                    openUrlInBrowser(URL_FORGOT_PASSWORD_HELP)
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
        alertController.actions.last().isEnabled = false
        parentViewController.presentViewController(alertController, errorMessage == null) {}
    }
}