package org.ergoplatform.desktop.mosaik

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.defaultPadding
import org.ergoplatform.mosaik.MosaikViewTree
import org.ergoplatform.mosaik.ViewTree
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.uilogic.STRING_BUTTON_RETRY
import org.ergoplatform.uilogic.STRING_ERROR_NO_MOSAIK_APP

@Composable
fun MosaikAppScreen(
    viewTree: ViewTree,
    noAppLoadedErrorState: MutableState<Throwable?>,
    retryClicked: () -> Unit,
) {
    MosaikViewTree(viewTree)

    Box(Modifier.fillMaxSize()) {

        noAppLoadedErrorState.value?.let { error ->
            NoAppLoadedError(
                remember(error) {
                    Application.texts.getString(
                        STRING_ERROR_NO_MOSAIK_APP,
                        error.javaClass.simpleName + " " + error.message
                    )

                },
                retryClicked,
            )
        }

    }
}

@Composable
fun BoxScope.NoAppLoadedError(
    errorMsg: String,
    retryClicked: () -> Unit,
) {
    Column(Modifier.padding(defaultPadding).align(Alignment.Center)) {
        Text(
            errorMsg,
            Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = labelStyle(LabelStyle.BODY1),
        )

        Button(
            retryClicked,
            Modifier.padding(top = defaultPadding * 1.5f).align(Alignment.CenterHorizontally),
        ) {
            Text(remember { Application.texts.getString(STRING_BUTTON_RETRY) })
        }
    }
}