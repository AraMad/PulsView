package com.orynastark.pulsview

import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.core.graphics.PathParser

/**
 * Created by Oryna Starkina on 06.08.2019.
 */
class PulsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val PROPERTY_X = "puls_phase"

    private var viewFromId = 0
    private var viewToId = 0

    private var isMirrored = false

    private var bypass = 0f
    private var corners = 0f
    private var lineColor = 0
    private var lineWidth = 8f

    private val linePaint: Paint
    private var pulsPaint: Paint? = null
    private val path: Path = Path()

    init {

        attrs?.let {
            val typedArray = context.obtainStyledAttributes(
                it,
                R.styleable.PulsView,
                0, 0
            )

            viewFromId = typedArray.getResourceId(R.styleable.PulsView_viewFrom, 0)
            viewToId = typedArray.getResourceId(R.styleable.PulsView_viewTo, 0)

            isMirrored = typedArray.getBoolean(R.styleable.PulsView_isMirrored, false)
            bypass = typedArray.getDimensionPixelSize(R.styleable.PulsView_bypass, 0).toFloat()
            corners = typedArray.getDimensionPixelSize(R.styleable.PulsView_corners, 0).toFloat()
            lineColor = typedArray.getColor(
                R.styleable.PulsView_lineColor,
                ContextCompat.getColor(context, android.R.color.white)
            )

            lineWidth = typedArray.getDimensionPixelSize(R.styleable.PulsView_lineWidth, 8).toFloat()

            typedArray.recycle()
        }

        linePaint = Paint().apply {
            pathEffect = CornerPathEffect(corners)
            isAntiAlias = false
            color = lineColor
            strokeWidth = lineWidth
            style = Paint.Style.STROKE
        }
    }

    private val dashPath: Path =
        PathParser.createPathFromPathData(
            "M8.8978,5C9.3863,3 8.079,2 7,2C5.921,2 4.6527,2.9883 5.0926,5C5.9672,9 6,13 6,13H8C8,13 7.9209,9 8.8978,5Z" // todo: add flexibility
        ).apply {
            close()
            transform(Matrix().apply {
                setScale(lineWidth / 2f, lineWidth / 2f)
            })

            val bounds = RectF() // todo: edit drawable to remove this
            computeBounds(bounds, true)
            transform(Matrix().apply {
                postRotate(270f, bounds.centerX(), bounds.centerY())
            }) // end remove

            offset(0f, lineWidth * -3.75f)
        }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        calcPath()

        canvas?.drawPath(path, linePaint)

        pulsPaint?.let {
            canvas?.drawPath(path, it)
        }
    }

    private fun calcPointFrom(fromViewBounds: Rect): Point {
        val pointFrom = Point()

        pointFrom.x = fromViewBounds.right + (fromViewBounds.left - fromViewBounds.right) / 2
        pointFrom.y = fromViewBounds.top + (fromViewBounds.bottom - fromViewBounds.top) / 2

        return pointFrom
    }

    private fun calcPointTo(toViewBounds: Rect): Point {
        val pointTo = Point()

        pointTo.x = toViewBounds.left + (toViewBounds.right - toViewBounds.left) / 2
        pointTo.y = toViewBounds.top + (toViewBounds.bottom - toViewBounds.top) / 2

        return pointTo
    }

    private fun calcPointsFromTo(): Pair<Point, Point> {

        val fromView = (context as Activity).findViewById<View>(viewFromId)
        val toView = (context as Activity).findViewById<View>(viewToId)

        // find from and to views bounds
        val fromViewBounds = Rect()
        val toViewBounds = Rect()

        fromView.getDrawingRect(fromViewBounds)
        toView.getDrawingRect(toViewBounds)

        with(this.parent as ViewGroup) {
            offsetDescendantRectToMyCoords(
                fromView,
                fromViewBounds
            )

            offsetDescendantRectToMyCoords(
                toView,
                toViewBounds
            )
        }


        return Pair(calcPointFrom(fromViewBounds), calcPointTo(toViewBounds))
    }

    private fun calcPath() {

        if (path.isEmpty) {

            val (pointFrom, pointTo) = calcPointsFromTo()

            if (bypass != 0f && pointFrom.y == pointTo.y) {
                path.moveTo(pointFrom.x.toFloat(), pointFrom.y.toFloat())
                path.lineTo(pointFrom.x.toFloat(), pointFrom.y.toFloat() + bypass)
                path.lineTo(pointTo.x.toFloat(), pointTo.y.toFloat() + bypass)
                path.lineTo(pointTo.x.toFloat(), pointTo.y.toFloat())

            } else {
                if (isMirrored) {
                    path.moveTo(pointFrom.x.toFloat(), pointFrom.y.toFloat())
                    path.lineTo(pointTo.x.toFloat(), pointFrom.y.toFloat())
                    path.lineTo(pointTo.x.toFloat(), pointTo.y.toFloat())
                } else {

                    path.moveTo(pointFrom.x.toFloat(), pointFrom.y.toFloat())
                    path.lineTo(pointFrom.x.toFloat(), pointTo.y.toFloat())
                    path.lineTo(pointTo.x.toFloat(), pointTo.y.toFloat())
                }
            }
        }
    }

    fun animateArrows(duration: Int, repeatCount: Int = 0) {

        pulsPaint = Paint().apply {
            style = Paint.Style.STROKE
            isAntiAlias = false
            color = lineColor
            strokeWidth = lineWidth
        }

        val arrowAnimator = createArrowAnimator(duration, repeatCount)
        arrowAnimator.start()
    }

    private fun createArrowAnimator(duration: Int, repeatCount: Int): ValueAnimator {

        return ValueAnimator().apply {

            setValues(
                PropertyValuesHolder.ofFloat(
                    PROPERTY_X, 0f, PathMeasure()
                        .apply { setPath(path, false) }.length
                )
            )
            this.duration = duration.toLong()
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener { valueAnimator ->

                // apply new path effect to puls path - animation makes here
                pulsPaint?.apply {
                    this.pathEffect = ComposePathEffect(
                        PathDashPathEffect(
                            dashPath, PathMeasure().apply { setPath(path, false) }.length,
                            valueAnimator.animatedValue as Float, // phase of dash
                            PathDashPathEffect.Style.MORPH
                        ), CornerPathEffect(corners)
                    )
                }

                invalidate()
            }

            this.repeatCount = repeatCount
        }
    }
}