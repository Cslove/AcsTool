package com.example.acstool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.acstool.ui.theme.AcsToolTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AcsToolTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    VersionButton(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun VersionButton(modifier: Modifier = Modifier) {
    var showVersionDialog by remember { mutableStateOf(false) }
    val appVersion = "1.0"

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = { showVersionDialog = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .size(200.dp, 80.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF000000),
                            Color(0xFF1a1a1a),
                            Color(0xFFD4AF37),
                            Color(0xFF000000)
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Text(
                text = "查看版本",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD4AF37)
            )
        }
    }

    if (showVersionDialog) {
        AlertDialog(
            onDismissRequest = { showVersionDialog = false },
            title = {
                Text(
                    text = "应用版本",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "当前版本: $appVersion",
                    fontSize = 18.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showVersionDialog = false }
                ) {
                    Text("确定")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun VersionButtonPreview() {
    AcsToolTheme {
        VersionButton()
    }
}