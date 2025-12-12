package org.example.project.domain.model


import drawingthoughts.composeapp.generated.resources.Res
import drawingthoughts.composeapp.generated.resources.ic_arrow_one_sided
import drawingthoughts.composeapp.generated.resources.ic_arrow_two_sided
import drawingthoughts.composeapp.generated.resources.ic_circle_filled
import drawingthoughts.composeapp.generated.resources.ic_circle_outline
import drawingthoughts.composeapp.generated.resources.ic_line_dotted
import drawingthoughts.composeapp.generated.resources.ic_line_plain
import drawingthoughts.composeapp.generated.resources.ic_rectangle_filled
import drawingthoughts.composeapp.generated.resources.ic_rectangle_outline
import drawingthoughts.composeapp.generated.resources.ic_triangle_filled
import drawingthoughts.composeapp.generated.resources.ic_triangle_outline
import drawingthoughts.composeapp.generated.resources.img_eraser
import drawingthoughts.composeapp.generated.resources.img_highlighter
import drawingthoughts.composeapp.generated.resources.ic_selector_cursor
import drawingthoughts.composeapp.generated.resources.img_laser_pen
import drawingthoughts.composeapp.generated.resources.img_pen
import org.jetbrains.compose.resources.DrawableResource

enum class DrawingTool(
    val res: DrawableResource
) {
    SELECTOR(Res.drawable.ic_selector_cursor),
    PEN(Res.drawable.img_pen),
    ERASER(Res.drawable.img_eraser),
    HIGHLIGHTER(Res.drawable.img_highlighter),
    LASER_PEN(Res.drawable.img_laser_pen),
    LINE_PLANE(Res.drawable.ic_line_plain),
    LINE_DOTTED(Res.drawable.ic_line_dotted),
    ARROW_ONE_SIDED(Res.drawable.ic_arrow_one_sided),
    ARROW_TWO_SIDED(Res.drawable.ic_arrow_two_sided),
    CIRCLE_OUTLINED(Res.drawable.ic_circle_outline),
    CIRCLE_FILLED(Res.drawable.ic_circle_filled),
    RECTANGLE_OUTLINED(Res.drawable.ic_rectangle_outline),
    RECTANGLE_FILLED(Res.drawable.ic_rectangle_filled),
    TRIANGLE_OUTLINED(Res.drawable.ic_triangle_outline),
    TRIANGLE_FILLED(Res.drawable.ic_triangle_filled),
    HAND(Res.drawable.ic_selector_cursor);
    
    fun isShape(): Boolean {
        return this == LINE_PLANE || this == LINE_DOTTED ||
               this == ARROW_ONE_SIDED || this == ARROW_TWO_SIDED ||
               this == CIRCLE_OUTLINED || this == CIRCLE_FILLED ||
               this == RECTANGLE_OUTLINED || this == RECTANGLE_FILLED ||
               this == TRIANGLE_OUTLINED || this == TRIANGLE_FILLED
    }
}