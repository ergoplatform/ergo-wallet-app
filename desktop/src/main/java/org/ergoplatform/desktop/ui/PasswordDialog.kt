package org.ergoplatform.desktop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.ergoplatform.Application
import org.ergoplatform.URL_FORGOT_PASSWORD_HELP
import org.ergoplatform.appkit.SecretString
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.uilogic.*

@Composable
fun PasswordDialog(
    showConfirmation: Boolean = false,
    onDismissRequest: () -> Unit,
    onPasswordEntered: (SecretString) -> String?
) {
    val passwordFieldState = remember { mutableStateOf(TextFieldValue()) }
    val confirmationFieldState = remember { mutableStateOf(TextFieldValue()) }
    val errorString = remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

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
                ).focusRequester(focusRequester),
                visualTransformation = PasswordVisualTransformation(),
                maxLines = 1,
                isError = hasError,
                singleLine = true,
                label = { Text(Application.texts.getString(STRING_LABEL_PASSWORD)) },
                colors = appTextFieldColors(),
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
                    visualTransformation = PasswordVisualTransformation(),
                    maxLines = 1,
                    singleLine = true,
                    label = { Text(Application.texts.getString(STRING_LABEL_PASSWORD_CONFIRM)) },
                    colors = appTextFieldColors(),
                )

            Row(Modifier.align(Alignment.End).padding(top = defaultPadding)) {
                Button(
                    onClick = {
                        onDismissRequest()
                    },
                    colors = secondaryButtonColors(),
                    modifier = Modifier.padding(end = defaultPadding * 2),
                ) {
                    Text(Application.texts.getString(STRING_LABEL_CANCEL))
                }
                Button(
                    onClick = {
                        val password = SecretString.create(passwordFieldState.value.text)

                        if (showConfirmation) {
                            val confirmPassword =
                                SecretString.create(confirmationFieldState.value.text)

                            if (password == null || password != confirmPassword) {
                                confirmPassword?.erase()
                                password?.erase()
                                errorString.value =
                                    Application.texts.getString(STRING_ERR_PASSWORD_CONFIRM)
                                return@Button
                            }
                            confirmPassword.erase()
                        }

                        val error = onPasswordEntered(password)
                        password?.erase()

                        if (error != null)
                            errorString.value = error
                        else {
                            onDismissRequest()
                        }
                    },
                    colors = primaryButtonColors(),
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