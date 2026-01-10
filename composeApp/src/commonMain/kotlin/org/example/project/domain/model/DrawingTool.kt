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
import drawingthoughts.composeapp.generated.resources.ic_rectangle_rounded
import drawingthoughts.composeapp.generated.resources.ic_triangle_filled
import drawingthoughts.composeapp.generated.resources.ic_triangle_outline
import drawingthoughts.composeapp.generated.resources.ic_star_outline
import drawingthoughts.composeapp.generated.resources.ic_star_filled
import drawingthoughts.composeapp.generated.resources.ic_diamond
import drawingthoughts.composeapp.generated.resources.ic_pentagon
import drawingthoughts.composeapp.generated.resources.ic_hexagon
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
    RECTANGLE_ROUNDED(Res.drawable.ic_rectangle_rounded),
    SQUARE_OUTLINED(Res.drawable.ic_rectangle_outline),
    SQUARE_FILLED(Res.drawable.ic_rectangle_filled),
    TRIANGLE_OUTLINED(Res.drawable.ic_triangle_outline),
    TRIANGLE_FILLED(Res.drawable.ic_triangle_filled),
    STAR_OUTLINED(Res.drawable.ic_star_outline),
    STAR_FILLED(Res.drawable.ic_star_filled),
    PENTAGON(Res.drawable.ic_pentagon),
    HEXAGON(Res.drawable.ic_hexagon),
    DIAMOND(Res.drawable.ic_diamond),
    ELLIPSE_OUTLINED(Res.drawable.ic_circle_outline),
    ELLIPSE_FILLED(Res.drawable.ic_circle_filled),
    HAND(Res.drawable.ic_selector_cursor);

    fun isShape(): Boolean {
        return this == LINE_PLANE || this == LINE_DOTTED ||
               this == ARROW_ONE_SIDED || this == ARROW_TWO_SIDED ||
               this == CIRCLE_OUTLINED || this == CIRCLE_FILLED ||
               this == RECTANGLE_OUTLINED || this == RECTANGLE_FILLED ||
               this == RECTANGLE_ROUNDED || this == SQUARE_OUTLINED || this == SQUARE_FILLED ||
               this == TRIANGLE_OUTLINED || this == TRIANGLE_FILLED ||
               this == STAR_OUTLINED || this == STAR_FILLED ||
               this == PENTAGON || this == HEXAGON || this == DIAMOND ||
               this == ELLIPSE_OUTLINED || this == ELLIPSE_FILLED
    }

    fun getShapeFamily(): String? {
        return when (this) {
            RECTANGLE_OUTLINED, RECTANGLE_FILLED, RECTANGLE_ROUNDED, SQUARE_OUTLINED, SQUARE_FILLED -> "rectangle"
            CIRCLE_OUTLINED, CIRCLE_FILLED, ELLIPSE_OUTLINED, ELLIPSE_FILLED -> "circle"
            TRIANGLE_OUTLINED, TRIANGLE_FILLED -> "triangle"
            LINE_PLANE, LINE_DOTTED -> "line"
            ARROW_ONE_SIDED, ARROW_TWO_SIDED -> "arrow"
            STAR_OUTLINED, STAR_FILLED, PENTAGON, HEXAGON, DIAMOND -> "polygon"
            else -> null
        }
    }
}