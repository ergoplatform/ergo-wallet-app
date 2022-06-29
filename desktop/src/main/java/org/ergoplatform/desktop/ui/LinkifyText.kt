package org.ergoplatform.desktop.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextDecoration
import java.util.regex.Pattern

// https://stackoverflow.com/a/66235329/7487013
// https://github.com/DmytroShuba/DailyTags/ would be great to use, but is not released for Multiplatform

@Composable
fun LinkifyText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle,
    linkColor: Color = uiErgoColor,
    isHtml: Boolean = false
) {
    val layoutResult = remember {
        mutableStateOf<TextLayoutResult?>(null)
    }
    val annotatedString = if (isHtml)
        buildAnnotatedString {
            val matcher = htmlTagPattern.matcher(text)
            var matchStart: Int
            var matchEnd = 0
            var previousMatchStart = 0

            while (matcher.find()) {
                matchStart = matcher.start(1)
                matchEnd = matcher.end()
                val beforeMatch = text.substring(
                    startIndex = previousMatchStart,
                    endIndex = matchStart - 2
                )
                val tagMatch = text.substring(
                    startIndex = text.indexOf(
                        char = '>',
                        startIndex = matchStart
                    ) + 1,
                    endIndex = text.indexOf(
                        char = '<',
                        startIndex = matchStart + 1
                    ),
                )
                append(
                    beforeMatch
                )
                // attach a string annotation that stores a URL to the text
                val annotation = text.substring(
                    startIndex = matchStart + 7,//omit <a hreh =
                    endIndex = text.indexOf(
                        char = '"',
                        startIndex = matchStart + 7,
                    )
                )
                pushStringAnnotation(tag = "URL", annotation = annotation)
                withStyle(
                    SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(
                        tagMatch
                    )
                }
                pop() //don't forget to add this line after a pushStringAnnotation
                previousMatchStart = matchEnd
            }
            //append the rest of the string
            if (text.length > matchEnd) {
                append(
                    text.substring(
                        startIndex = matchEnd,
                        endIndex = text.length
                    )
                )
            }
        }
    else
        buildAnnotatedString {
            append(text)
            val linksList = extractUrls(text)
            linksList.forEach {
                addStyle(
                    style = SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    ),
                    start = it.start,
                    end = it.end
                )
                addStringAnnotation(
                    tag = "URL",
                    annotation = it.url,
                    start = it.start,
                    end = it.end
                )
            }
        }
    Text(text = annotatedString,
        style = style,
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { offsetPosition ->
                layoutResult.value?.let {
                    val position = it.getOffsetForPosition(offsetPosition)
                    annotatedString.getStringAnnotations(position, position).firstOrNull()
                        ?.let { result ->
                            if (result.tag == "URL") {
                                openBrowser(result.item)
                            }
                        }
                }
            }
        },
        onTextLayout = { layoutResult.value = it }
    )
}

private val urlPattern: Pattern = Pattern.compile(
    "(?:^|[\\W])((ht|f)tp(s?)://|www\\.)"
            + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+/?)*"
            + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]*$~@!:/{};']*)",
    Pattern.CASE_INSENSITIVE or Pattern.MULTILINE or Pattern.DOTALL
)

private val htmlTagPattern = Pattern.compile(
    "(?i)<a([^>]+)>(.+?)</a>",
    Pattern.CASE_INSENSITIVE or Pattern.MULTILINE or Pattern.DOTALL
)

fun extractUrls(text: String): List<LinkInfos> {
    val matcher = urlPattern.matcher(text)
    var matchStart: Int
    var matchEnd: Int
    val links = arrayListOf<LinkInfos>()

    while (matcher.find()) {
        matchStart = matcher.start(1)
        matchEnd = matcher.end()

        var url = text.substring(matchStart, matchEnd)
        if (!url.startsWith("http://") && !url.startsWith("https://"))
            url = "https://$url"

        links.add(LinkInfos(url, matchStart, matchEnd))
    }
    return links
}

data class LinkInfos(
    val url: String,
    val start: Int,
    val end: Int
)