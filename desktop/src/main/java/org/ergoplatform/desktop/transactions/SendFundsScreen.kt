package org.ergoplatform.desktop.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GreetingScreen(
    greeting: String,
    onGoBackClicked: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // Greeting
        Text(
            text = greeting,
            fontSize = 40.sp
        )

        // Spacing between text and button
        Spacer(modifier = Modifier.height(30.dp))

        // Go back button
        Button(onClick = onGoBackClicked) {
            Text(text = "GO BACK!")
        }
    }
}