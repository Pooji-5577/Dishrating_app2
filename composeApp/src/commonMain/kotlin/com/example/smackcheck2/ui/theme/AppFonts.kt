package com.example.smackcheck2.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import smackcheck.composeapp.generated.resources.Res
import smackcheck.composeapp.generated.resources.newsreader_italic
import smackcheck.composeapp.generated.resources.newsreader_regular
import smackcheck.composeapp.generated.resources.newsreader_semibold
import smackcheck.composeapp.generated.resources.plus_jakarta_sans_bold
import smackcheck.composeapp.generated.resources.plus_jakarta_sans_extrabold
import smackcheck.composeapp.generated.resources.plus_jakarta_sans_medium
import smackcheck.composeapp.generated.resources.plus_jakarta_sans_regular
import smackcheck.composeapp.generated.resources.plus_jakarta_sans_semibold

@Composable
fun PlusJakartaSans(): FontFamily = FontFamily(
    Font(Res.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(Res.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(Res.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(Res.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(Res.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
)

@Composable
fun NewsreaderFontFamily(): FontFamily = FontFamily(
    Font(Res.font.newsreader_regular, FontWeight.Normal),
    Font(Res.font.newsreader_italic, FontWeight.Normal, FontStyle.Italic),
    Font(Res.font.newsreader_semibold, FontWeight.SemiBold),
)
