package org.ergoplatform.desktop.wallet

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InputScreen(
    name: String,
    onGoClicked: (String) -> Unit,
    onTextChanged: (String) -> Unit
) {

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Greet Me!",
            fontSize = 30.sp
        )

        Spacer(modifier = Modifier.height(30.dp))
        TextField(
            value = name,
            maxLines = 1,
            label = { Text(text = "Enter your name") },

            onValueChange = onTextChanged
        )
        Spacer(modifier = Modifier.height(30.dp))
        Button(
            onClick = {
                onGoClicked(name)
            }
        ) {
            Text(text = "GO!")
        }
    }

}