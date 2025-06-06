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
import dora.widget.gridview.Cell
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
    private var horizontalSpacing: Float
    private var verticalSpacing: Float
    private var onCellSelectListener: OnCellSelectListener? = null

    init {
        val density = context.resources.displayMetrics.density
        horizontalSpacing = 5 * density
        verticalSpacing = 5 * density
        initAttrs(context, attrs)
        initPaints()
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
        horizontalSpacing = ta.getDimension(
            R.styleable.DoraGridView_dview_gv_horizontalSpacing,
            horizontalSpacing
        )
        verticalSpacing = ta.getDimension(
            R.styleable.DoraGridView_dview_gv_verticalSpacing,
            verticalSpacing
        )
        ta.recycle()
    }

    private fun computeCellSize(): Float {
        val availWidth = measuredWidth.toFloat() - 2 * horizontalSpacing
        val availHeight = measuredHeight.toFloat() - 2 * verticalSpacing
        val cellWidth  = availWidth / rowCellCount
        val cellHeight = availHeight / columnCellCount
        return minOf(cellWidth, cellHeight)
    }

    private fun initPaints() {
        gridLinePaint.strokeWidth = 2f
        gridLinePaint.color = gridLineColor

        gridBgPaint.style = Paint.Style.FILL

        textPaint.textSize = 30f
        textPaint.color = cellTextColor

        selectionPaint.color = selectionColor
        selectionPaint.strokeWidth = 5f
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val defaultSizeDp = 370f
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
        val cellSize = computeCellSize()
        val data = cells
        for (i in 0 until rowCellCount) {
            for (j in 0 until columnCellCount) {
                val left   = horizontalSpacing + j * cellSize
                val top    = verticalSpacing   + i * cellSize
                val right  = left + cellSize
                val bottom = top  + cellSize
                val bgColor = data?.getOrNull(i)?.getOrNull(j)?.bgColor
                gridBgPaint.color = bgColor ?: Color.BLACK
                canvas.drawRect(RectF(left, top, right, bottom), gridBgPaint)
            }
        }
    }

    private fun drawGridLines(canvas: Canvas) {
        val cellSize = computeCellSize()
        val totalWidth  = cellSize * rowCellCount
        val totalHeight = cellSize * columnCellCount
        // 垂直线：从最左边 horizontalSpacing 开始，每隔 cellSize 画一条
        for (i in 0..rowCellCount) {
            val x = horizontalSpacing + i * cellSize
            canvas.drawLine(x, verticalSpacing, x, verticalSpacing + totalHeight, gridLinePaint)
        }
        // 水平线：从最上方 verticalSpacing 开始，每隔 cellSize 画一条
        for (j in 0..columnCellCount) {
            val y = verticalSpacing + j * cellSize
            canvas.drawLine(horizontalSpacing, y, horizontalSpacing + totalWidth, y, gridLinePaint)
        }
    }

    private fun drawGridText(canvas: Canvas) {
        val cellSize = computeCellSize()
        val data = cells
        for (i in 0 until rowCellCount) {
            for (j in 0 until columnCellCount) {
                val text = data?.getOrNull(i)?.getOrNull(j)?.text
                if (!text.isNullOrEmpty()) {
                    val textWidth = textPaint.measureText(text)
                    val x = horizontalSpacing + j * cellSize + (cellSize - textWidth) / 2
                    val y = verticalSpacing + i * cellSize +
                            (cellSize + textPaint.textSize) / 2 -
                            textPaint.descent()
                    canvas.drawText(text, x, y, textPaint)
                }
            }
        }
    }

    private fun drawSelectionBorder(canvas: Canvas) {
        if (selectedRow in 0 until rowCellCount && selectedColumn in 0 until columnCellCount) {
            val cellSize = computeCellSize()
            val left   = horizontalSpacing + selectedColumn * cellSize
            val top    = verticalSpacing   + selectedRow    * cellSize
            val right  = left + cellSize
            val bottom = top  + cellSize
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
                    val cellSize = computeCellSize()
                    val columnIndex = ((event.x - horizontalSpacing) / cellSize).toInt()
                    val rowIndex    = ((event.y - verticalSpacing)   / cellSize).toInt()
                    if (rowIndex in 0 until rowCellCount && columnIndex in 0 until columnCellCount) {
                        selectedRow    = rowIndex
                        selectedColumn = columnIndex
                        invalidate()
                        val selectedCell = cells?.getOrNull(rowIndex)?.getOrNull(columnIndex)
                        onCellSelectListener?.onCellSelected(rowIndex, columnIndex, selectedCell)
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

    interface OnCellSelectListener {
        fun onCellSelected(rowIndex: Int, columnIndex: Int, cell: Cell?)
    }

    fun setOnCellSelectListener(listener: OnCellSelectListener) {
        this.onCellSelectListener = listener
    }

    fun setCells(cells: Array<Array<Cell>>) {
        this.cells = cells
        rowCellCount = cells.size
        columnCellCount = if (cells.isNotEmpty()) cells[0].size else 0
        requestLayout()
        invalidate()
    }

    /**
     * 设置数据。
     * @since 1.1
     */
    fun setCells(vararg rows: Array<Cell>) {
        val arr = Array(rows.size) { i -> rows[i] }
        setCells(arr)
    }

    /**
     * 设置数据。
     * @since 1.1
     */
    fun setCells(cellsList: List<List<Cell>>) {
        val arr = cellsList.map { it.toTypedArray() }.toTypedArray()
        setCells(arr)
    }

    /**
     * 设置数据。
     * @since 1.1
     */
    fun setCells(cellsList: List<Array<Cell>>) {
        setCells(cellsList.toTypedArray())
    }

    /**
     * 扁平化数组或集合，指定每行 itemCount 个 Cell，
     * 如果总数不能被 itemCount 整除，则回退到默认每行 3 个。
     * @since 1.1
     */
    fun setCells(flatCells: Array<Cell>, itemsPerRow: Int) {
        // 校验 itemsPerRow：必须大于 0，且 flatCells.size % itemsPerRow == 0
        val perRow = if (itemsPerRow > 0 && flatCells.size % itemsPerRow == 0) {
            itemsPerRow
        } else {
            3
        }
        val rowCount = flatCells.size / perRow
        // 构造二维数组：每行 perRow 个元素
        val matrix = Array(rowCount) { rowIndex ->
            flatCells.copyOfRange(rowIndex * perRow, (rowIndex + 1) * perRow)
        }
        setCells(matrix)
    }

    /**
     * 接受 List<Cell>，并指定每行 itemCount 个 Cell，
     * 如果总数不能被 itemCount 整除，则回退到默认每行 3 个。
     * @since 1.1
     */
    fun setCells(flatList: List<Cell>, itemsPerRow: Int) {
        // 转成 Array<Cell> 并复用上面的方法
        setCells(flatList.toTypedArray(), itemsPerRow)
    }

    fun resetSelection() {
        selectedRow    = -1
        selectedColumn = -1
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
}
