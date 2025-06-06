package dora.widget.gridview

import androidx.annotation.ColorInt

data class Cell(
    val text: String? = null,
    @ColorInt val textColor: Int? = null,
    @ColorInt val bgColor: Int? = null
)