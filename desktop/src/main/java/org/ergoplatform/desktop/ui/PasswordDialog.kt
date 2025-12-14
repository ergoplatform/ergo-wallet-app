package org.ergoplatform.desktop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ergoplatform.Application
import org.ergoplatform.SigningSecrets
import org.ergoplatform.URL_FORGOT_PASSWORD_HELP
import org.ergoplatform.api.AesEncryptionManager
import org.ergoplatform.appkit.SecretString
import org.ergoplatform.compose.settings.appTextFieldColors
import org.ergoplatform.compose.settings.primaryButtonColors
import org.ergoplatform.compose.settings.secondaryButtonColors
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.uilogic.*
import java.util.*

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PasswordDialog(
    showConfirmation: Boolean = false,
    onDismissRequest: () -> Unit,
    onPasswordEntered: (SecretString?) -> String?
) {
    val passwordFieldState = remember { mutableStateOf(TextFieldValue()) }
    val confirmationFieldState = remember { mutableStateOf(TextFieldValue()) }
    val errorString = remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    val isProcessing = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val passwordVisible = remember { mutableStateOf(false) }
    val confirmPasswordVisible = remember { mutableStateOf(false) }

    val onDone = {
        val passwordString = passwordFieldState.value.text
        val password = if (passwordString.isNotEmpty()) SecretString.create(passwordString) else null

        val proceed = if (showConfirmation) {
            val confirmPassword =
                SecretString.create(confirmationFieldState.value.text)

            if (password == null || password != confirmPassword) {
                confirmPassword?.erase()
                password?.erase()
                passwordFieldState.value = TextFieldValue()
                confirmationFieldState.value = TextFieldValue()
                errorString.value =
                    Application.texts.getString(STRING_ERR_PASSWORD_CONFIRM)
                false
            } else {
                confirmPassword.erase()
                true
            }
        } else true

        if (proceed) {
            isProcessing.value = true
            coroutineScope.launch {
                val error = withContext(Dispatchers.IO) {
                    onPasswordEntered(password)
                }
                password?.erase()

                isProcessing.value = false
                if (error != null) {
                    errorString.value = error
                } else {
                    onDismissRequest()
                }
            }
        }
    }

    AppDialog({}) {
        Column(Modifier.fillMaxWidth().padding(defaultPadding)) {

            Text(
                Application.texts.getString(STRING_HINT_PASSWORD),
                style = labelStyle(LabelStyle.BODY1)
            )

            if (!showConfirmation)
                Text(
                    Application.texts.getString(STRING_LABEL_FORGOT_PASSWORD),
                    Modifier.align(Alignment.End).padding(top = defaultPadding / 2).clickable {
                        openBrowser(URL_FORGOT_PASSWORD_HELP)
                    },
                    style = labelStyle(LabelStyle.BODY1)
                )

            val hasError = errorString.value != null
            OutlinedTextField(
                passwordFieldState.value,
                onValueChange = {
                    passwordFieldState.value = it
                },
                Modifier.fillMaxWidth().padding(
                    top = defaultPadding,
                    bottom = if (hasError) 0.dp else defaultPadding
                ).focusRequester(focusRequester).onKeyEvent {
                    if (!showConfirmation && it.type == KeyEventType.KeyUp && (it.key == Key.Enter || it.key == Key.NumPadEnter)) {
                        onDone()
                        true
                    } else false
                },
                visualTransformation = if (passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
                maxLines = 1,
                isError = hasError,
                singleLine = true,
                label = { Text(Application.texts.getString(STRING_LABEL_PASSWORD)) },
                colors = appTextFieldColors(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible.value = !passwordVisible.value }) {
                        Icon(
                            imageVector = if (passwordVisible.value) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible.value) "Hide password" else "Show password"
                        )
                    }
                }
            )

            if (hasError)
                Text(
                    errorString.value ?: "",
                    Modifier.padding(bottom = defaultPadding),
                    color = uiErgoColor,
                    style = labelStyle(LabelStyle.BODY2)
                )

            if (showConfirmation)
                OutlinedTextField(
                    confirmationFieldState.value,
                    onValueChange = {
                        confirmationFieldState.value = it
                    },
                    Modifier.fillMaxWidth().padding(bottom = defaultPadding),
                    visualTransformation = if (confirmPasswordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
                    maxLines = 1,
                    singleLine = true,
                    label = { Text(Application.texts.getString(STRING_LABEL_PASSWORD_CONFIRM)) },
                    colors = appTextFieldColors(),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible.value = !confirmPasswordVisible.value }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible.value) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (confirmPasswordVisible.value) "Hide password" else "Show password"
                            )
                        }
                    }
                )

            Row(Modifier.align(Alignment.End).padding(top = defaultPadding)) {
                if (isProcessing.value) {
                    CircularProgressIndicator(
                        Modifier.padding(end = defaultPadding)
                    )
                }
                
                Button(
                    onClick = {
                        onDismissRequest()
                    },
                    colors = secondaryButtonColors(),
                    modifier = Modifier.padding(end = defaultPadding * 2),
                    enabled = !isProcessing.value
                ) {
                    Text(Application.texts.getString(STRING_LABEL_CANCEL))
                }
                Button(
                    onClick = onDone,
                    colors = primaryButtonColors(),
                    enabled = !isProcessing.value
                ) {
                    Text(Application.texts.getString(STRING_BUTTON_DONE))
                }
            }
        }
    }

    SideEffect {
        focusRequester.requestFocus()
    }
}

/**
 * Called after password is entered. Password may be wrong, so
 * @return false to show a warning about a wrong password
 */
fun proceedAuthFlowWithPassword(
    password: SecretString?,
    walletConfig: WalletConfig,
    proceedFromAuthFlow: (SigningSecrets) -> Unit
): String? {
    password?.let {
        if (!proceedAuthFlowWithPasswordIntn(password, walletConfig, proceedFromAuthFlow)) {
            return Application.texts.getString(STRING_ERROR_PASSWORD_WRONG)
        } else {
            return null
        }
    }
    return Application.texts.getString(STRING_ERROR_PASSWORD_EMPTY)
}

private fun proceedAuthFlowWithPasswordIntn(
    password: SecretString,
    walletConfig: WalletConfig,
    proceedFromAuthFlow: (SigningSecrets) -> Unit
): Boolean {
    walletConfig.secretStorage?.let {
        val secrets: SigningSecrets?
        var decryptData: ByteArray? = null
        try {
            decryptData = AesEncryptionManager.decryptData(password, it)
            secrets = SigningSecrets.fromBytes(decryptData!!)
        } catch (t: Throwable) {
            // Password wrong
            decryptData?.let {
                Arrays.fill(decryptData, 0)
            }
            return false
        }

        if (secrets == null) {
            // deserialization error, corrupted db data
            return false
        }

        proceedFromAuthFlow(secrets)

        return true
    }

    return false
}