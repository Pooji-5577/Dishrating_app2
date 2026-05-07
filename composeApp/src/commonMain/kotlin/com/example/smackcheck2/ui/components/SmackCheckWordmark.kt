package com.example.smackcheck2.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun SmackCheckWordmark(
    modifier: Modifier = Modifier,
    fontFamily: FontFamily,
    fontSize: TextUnit,
    smackColor: Color = Color(0xFF642223),
    checkColor: Color = Color(0xFF2D2F2F),
    letterSpacing: TextUnit = (-1.0).sp
) {
    BasicText(
        modifier = modifier,
        text = buildAnnotatedString {
            pushStyle(
                SpanStyle(
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize,
                    letterSpacing = letterSpacing,
                    color = smackColor
                )
            )
            append("Smack")
            pop()
            pushStyle(
                SpanStyle(
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize,
                    letterSpacing = letterSpacing,
                    color = checkColor
                )
            )
            append("Check")
            pop()
        }
    )
}
