package dora.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.annotation.ColorInt
import dora.widget.gridview.R

class DoraGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val gridLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.RED
    }

    private var rowCellCount: Int = 1
    private var columnCellCount: Int = 1
    private var enableInteraction: Boolean = false
    private var cells: Array<Array<Cell>>? = null
    private var selectedRow: Int = -1
    private var selectedColumn: Int = -1
    private var cellTextColor: Int = Color.WHITE
    private var gridLineColor: Int = Color.GRAY
    private var selectionColor: Int = Color.RED
    private var downX: Float = 0f
    private var downY: Float = 0f
    private var isPotentialClick: Boolean = false

    private var cellClickListener: OnCellClickListener? = null

    init {
        initAttrs(context, attrs)
        initPaints(context)
    }


    private fun initAttrs(context: Context, attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.DoraGridView)
        enableInteraction = ta.getBoolean(
            R.styleable.DoraGridView_dview_gv_enableInteraction,
            false
        )
        val defaultTextColor = Color.WHITE
        val textColor = ta.getColor(
            R.styleable.DoraGridView_dview_gv_textColor,
            defaultTextColor
        )
        textPaint.color = textColor
        val defaultGridLineColor = Color.GRAY
        val gridLineColor = ta.getColor(
            R.styleable.DoraGridView_dview_gv_gridLineColor,
            defaultGridLineColor
        )
        gridLinePaint.color = gridLineColor
        val defaultSelection = Color.RED
        selectionColor = ta.getColor(
            R.styleable.DoraGridView_dview_gv_selectionColor,
            defaultSelection
        )
        ta.recycle()
    }

    private fun initPaints(context: Context) {
        gridLinePaint.strokeWidth = 2f
        gridLinePaint.color = gridLineColor

        gridBgPaint.style = Paint.Style.FILL

        textPaint.textSize = 30f
        textPaint.color = cellTextColor

        selectionPaint.color = selectionColor
        selectionPaint.strokeWidth = 5f
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val defaultSizeDp = 300
        val density = resources.displayMetrics.density
        val defaultSizePx = (defaultSizeDp * density).toInt()
        val width = resolveSize(defaultSizePx, widthMeasureSpec)
        val height = resolveSize(defaultSizePx, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawGridBackground(canvas)
        drawGridLines(canvas)
        drawGridText(canvas)
        drawSelectionBorder(canvas)
    }

    private fun drawGridBackground(canvas: Canvas) {
        val cellSize = measuredWidth.toFloat() / (rowCellCount + 2)
        val data = cells
        for (i in 0 until rowCellCount) {
            for (j in 0 until columnCellCount) {
                val left = (i + 1) * cellSize
                val top = (j + 1) * cellSize
                val right = left + cellSize
                val bottom = top + cellSize
                val bgColor = data?.getOrNull(i)?.getOrNull(j)?.bgColor
                gridBgPaint.color = bgColor ?: Color.BLACK
                canvas.drawRect(RectF(left, top, right, bottom), gridBgPaint)
            }
        }
    }

    private fun drawGridLines(canvas: Canvas) {
        val cellSize = measuredWidth.toFloat() / (rowCellCount + 2)
        // 垂直线
        for (i in 0..rowCellCount) {
            val x = cellSize * (i + 1)
            canvas.drawLine(x, cellSize, x, measuredHeight - cellSize, gridLinePaint)
        }
        // 水平线
        for (j in 0..columnCellCount) {
            val y = cellSize * (j + 1)
            canvas.drawLine(cellSize, y, measuredWidth - cellSize, y, gridLinePaint)
        }
    }

    private fun drawGridText(canvas: Canvas) {
        val cellSize = measuredWidth.toFloat() / (rowCellCount + 2)
        val data = cells
        for (i in 0 until rowCellCount) {
            for (j in 0 until columnCellCount) {
                val text = data?.getOrNull(i)?.getOrNull(j)?.text
                if (!text.isNullOrEmpty()) {
                    val textWidth = textPaint.measureText(text)
                    val x = cellSize * (i + 1) + (cellSize - textWidth) / 2
                    val y = cellSize * (j + 1) + (cellSize + textPaint.textSize) / 2 - textPaint.descent()
                    canvas.drawText(text, x, y, textPaint)
                }
            }
        }
    }

    private fun drawSelectionBorder(canvas: Canvas) {
        if (selectedRow in 0 until rowCellCount && selectedColumn in 0 until columnCellCount) {
            val cellSize = measuredWidth.toFloat() / (rowCellCount + 2)
            val left = cellSize * (selectedRow + 1)
            val top = cellSize * (selectedColumn + 1)
            val right = left + cellSize
            val bottom = top + cellSize
            canvas.drawRect(RectF(left, top, right, bottom), selectionPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!enableInteraction) {
            return true
        }
        val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                isPotentialClick = true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - downX
                val dy = event.y - downY
                if (dx * dx + dy * dy > touchSlop * touchSlop) {
                    isPotentialClick = false
                }
            }
            MotionEvent.ACTION_UP -> {
                if (isPotentialClick) {
                    val cellSize = measuredWidth.toFloat() / (rowCellCount + 2)
                    val rowIndex = ((event.x - cellSize) / cellSize).toInt()
                    val colIndex = ((event.y - cellSize) / cellSize).toInt()
                    if (rowIndex in 0 until rowCellCount && colIndex in 0 until columnCellCount) {
                        selectedRow = rowIndex
                        selectedColumn = colIndex
                        invalidate()
                        val clickedCell = cells?.getOrNull(rowIndex)?.getOrNull(colIndex)
                        cellClickListener?.onCellClick(rowIndex, colIndex, clickedCell)
                    }
                }
                isPotentialClick = false
            }
            MotionEvent.ACTION_CANCEL -> {
                isPotentialClick = false
            }
        }
        return true
    }

    interface OnCellClickListener {
        fun onCellClick(row: Int, column: Int, cell: Cell?)
    }

    fun setOnCellClickListener(listener: OnCellClickListener) {
        this.cellClickListener = listener
    }

    fun setCells(cells: Array<Array<Cell>>) {
        this.cells = cells
        rowCellCount = cells.size
        columnCellCount = if (cells.isNotEmpty()) cells[0].size else 0
        requestLayout()
        invalidate()
    }

    fun setRowColumnCount(rowCount: Int, columnCount: Int) {
        this.rowCellCount = rowCount
        this.columnCellCount = columnCount
        this.cells = null
        requestLayout()
        invalidate()
    }

    fun setEnableInteraction(enable: Boolean) {
        this.enableInteraction = enable
    }

    data class Cell(
        val row: Int,
        val column: Int,
        val text: String? = null,
        @ColorInt val bgColor: Int? = null
    )
}
