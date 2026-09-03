package com.rero.miozicplayer.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.rero.miozicplayer.ui.theme.MiozicLatinFontFamily
import com.rero.miozicplayer.ui.theme.MiozicPersianFontFamily

private val persianScriptRegex =
    Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF\\uFB50-\\uFDFF\\uFE70-\\uFEFF]")

fun containsPersianScript(text: String): Boolean = persianScriptRegex.containsMatchIn(text)

@Composable
fun LocaleText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val isPersian = remember(text) { containsPersianScript(text) }
    val fontFamily = if (isPersian) MiozicPersianFontFamily else MiozicLatinFontFamily
    val resolvedAlign = textAlign ?: if (isPersian) TextAlign.Right else TextAlign.Start
    val textDirection = if (isPersian) TextDirection.Rtl else TextDirection.Ltr

    Text(
        text = text,
        modifier = modifier,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        style = TextStyle(
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            textAlign = resolvedAlign,
            textDirection = textDirection,
        ),
    )
}

@Composable
fun localeFontFamily(text: String): FontFamily {
    return if (remember(text) { containsPersianScript(text) }) {
        MiozicPersianFontFamily
    } else {
        MiozicLatinFontFamily
    }
}

@Composable
fun localeTextDirection(text: String): TextDirection {
    return if (remember(text) { containsPersianScript(text) }) {
        TextDirection.Rtl
    } else {
        TextDirection.Ltr
    }
}
