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
    private val gridBgPaint   = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint     = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.RED
    }

    // “行/列数量” 的命名说明：
    //   rowCellCount    —— 每一行有多少个 Cell（也就是列的数量）
    //   columnCellCount —— 总共有多少行
    // 之所以这么命名是为了兼容原来的逻辑：cells[row][column]，cells.size == 行数，所以 columnCellCount 用来表示 cells.size。
    private var rowCellCount: Int = 0      // 实际上代表每行单元格的个数（列数）
    private var columnCellCount: Int = 0   // 实际上代表总共有多少行

    private var enableInteraction: Boolean = false
    private var cells: Array<Array<Cell>>? = null

    private var selectedRow: Int = -1      // 选中的“行”索引
    private var selectedColumn: Int = -1   // 选中的“列”索引

    private var cellTextColor: Int = Color.WHITE
    private var cellTextSize: Float = 30f
    private var gridLineColor: Int = Color.GRAY
    private var selectionBorderColor: Int = Color.RED
    private var gridLineWidth: Float = 2f
    private var selectionBorderWidth: Float = 5f
    private var downX: Float = 0f
    private var downY: Float = 0f
    private var isPotentialClick: Boolean = false

    private var horizontalSpacing: Float  // 左右留白
    private var verticalSpacing: Float    // 上下留白

    private var onCellSelectListener: OnCellSelectListener? = null

    init {
        val density = context.resources.displayMetrics.density
        horizontalSpacing = 10 * density
        verticalSpacing   = 10 * density
        initAttrs(context, attrs)
        initPaints()
    }

    private fun initAttrs(context: Context, attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.DoraGridView)
        enableInteraction = ta.getBoolean(
            R.styleable.DoraGridView_dview_gv_enableInteraction,
            false
        )
        cellTextColor = ta.getColor(
            R.styleable.DoraGridView_dview_gv_textColor,
            cellTextColor
        )
        cellTextSize = ta.getDimension(
            R.styleable.DoraGridView_dview_gv_textSize,
            cellTextSize
        )
        gridLineColor = ta.getColor(
            R.styleable.DoraGridView_dview_gv_gridLineColor,
            gridLineColor
        )
        gridLineWidth = ta.getDimension(
            R.styleable.DoraGridView_dview_gv_gridLineWidth,
            gridLineWidth
        )
        selectionBorderColor = ta.getColor(
            R.styleable.DoraGridView_dview_gv_selectionBorderColor,
            selectionBorderColor
        )
        selectionBorderWidth = ta.getDimension(
            R.styleable.DoraGridView_dview_gv_selectionBorderWidth,
            selectionBorderWidth
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

    private fun initPaints() {
        gridLinePaint.strokeWidth = gridLineWidth
        gridLinePaint.color = gridLineColor

        gridBgPaint.style = Paint.Style.FILL

        textPaint.color = cellTextColor
        textPaint.textSize = cellTextSize

        selectionPaint.color = selectionBorderColor
        selectionPaint.strokeWidth = selectionBorderWidth
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val defaultSizeDp = 360f + horizontalSpacing * 2
        val density = resources.displayMetrics.density
        val defaultSizePx = (defaultSizeDp * density).toInt()
        val w = resolveSize(defaultSizePx, widthMeasureSpec)
        val h = resolveSize(defaultSizePx, heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawGridBackground(canvas)
        drawGridLines(canvas)
        drawGridText(canvas)
        drawSelectionBorder(canvas)
    }

    /**
     * 计算单元格边长：取可用宽度/列数 和 可用高度/行数 的最小值
     */
    private fun computeCellSize(): Float {
        // 可用宽度 = 整体宽度 - 左右留白
        val availWidth  = measuredWidth.toFloat() - 2 * horizontalSpacing
        // 可用高度 = 整体高度 - 上下留白
        val availHeight = measuredHeight.toFloat() - 2 * verticalSpacing
        if (rowCellCount == 0 || columnCellCount == 0) {
            return 0f
        }
        val cellWidth  = availWidth / rowCellCount
        val cellHeight = availHeight / columnCellCount
        return minOf(cellWidth, cellHeight)
    }

    /**
     * 绘制背景色（背景色来自 cells\[row\]\[column\].bgColor）
     * 外层 i 对应 行（0 .. columnCellCount-1）
     * 内层 j 对应 列（0 .. rowCellCount-1）
     */
    private fun drawGridBackground(canvas: Canvas) {
        val cellSize = computeCellSize()
        val data = cells
        for (i in 0 until columnCellCount) {           // i = 行索引
            for (j in 0 until rowCellCount) {          // j = 列索引
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

    /**
     * 绘制网格线。
     *  - 垂直线要画 (rowCellCount + 1) 条，x = horizontalSpacing + index * cellSize
     *  - 水平线要画 (columnCellCount + 1) 条，y = verticalSpacing + index * cellSize
     */
    private fun drawGridLines(canvas: Canvas) {
        val cellSize = computeCellSize()
        val totalWidth  = cellSize * rowCellCount    // 总宽 = 列数 * 单元格宽度
        val totalHeight = cellSize * columnCellCount // 总高 = 行数 * 单元格高度

        // 画垂直线
        for (i in 0..rowCellCount) {
            val x = horizontalSpacing + i * cellSize
            canvas.drawLine(x, verticalSpacing, x, verticalSpacing + totalHeight, gridLinePaint)
        }
        // 画水平线
        for (j in 0..columnCellCount) {
            val y = verticalSpacing + j * cellSize
            canvas.drawLine(horizontalSpacing, y, horizontalSpacing + totalWidth, y, gridLinePaint)
        }
    }

    /**
     * 绘制文字：同样 i 对应 行，j 对应 列。
     */
    private fun drawGridText(canvas: Canvas) {
        val cellSize = computeCellSize()
        val data = cells
        for (i in 0 until columnCellCount) {             // 行
            for (j in 0 until rowCellCount) {            // 列
                val text = data?.getOrNull(i)?.getOrNull(j)?.text
                if (!text.isNullOrEmpty()) {
                    val textWidth = textPaint.measureText(text)
                    // 水平居中：左边起点 + 列索引 * 单元格宽度 + (单元格宽度 - 文本宽度)/2
                    val x = horizontalSpacing + j * cellSize + (cellSize - textWidth) / 2
                    // 垂直居中：上边起点 + 行索引 * 单元格高度 + (单元格高度 + 字体高度)/2 - descent
                    val y = verticalSpacing + i * cellSize +
                            (cellSize + textPaint.textSize) / 2 -
                            textPaint.descent()
                    canvas.drawText(text, x, y, textPaint)
                }
            }
        }
    }

    /**
     * 绘制选中框：selectedRow = 行索引，selectedColumn = 列索引。
     * left = horizontalSpacing + selectedColumn * cellSize
     * top  = verticalSpacing   + selectedRow * cellSize
     */
    private fun drawSelectionBorder(canvas: Canvas) {
        // row must be < columnCellCount，col must be < rowCellCount
        if (selectedRow in 0 until columnCellCount && selectedColumn in 0 until rowCellCount) {
            val cellSize = computeCellSize()
            val left   = horizontalSpacing + selectedColumn * cellSize
            val top    = verticalSpacing   + selectedRow * cellSize
            val right  = left + cellSize
            val bottom = top  + cellSize
            canvas.drawRect(RectF(left, top, right, bottom), selectionPaint)
        }
    }

    /**
     * 点击事件：判断触摸是否在某个单元格里，并触发 onCellSelectListener。
     *  - rowIndex = ((event.y - verticalSpacing) / cellSize).toInt()
     *  - columnIndex = ((event.x - horizontalSpacing) / cellSize).toInt()
     */
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
                    val rowIndex = ((event.x - horizontalSpacing) / cellSize).toInt()
                    val columnIndex    = ((event.y - verticalSpacing)   / cellSize).toInt()
                    if (rowIndex in 0 until rowCellCount && columnIndex in 0 until columnCellCount) {
                        selectedColumn    = rowIndex
                        selectedRow = columnIndex
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

    /**
     * 核心方法：直接设置一个二维数组，cells\[row\]\[column\]，
     * 并且根据数组自动计算行/列数量：
     *   columnCellCount = cellsMatrix.size         （行数）
     *   rowCellCount    = cellsMatrix[0].size      （列数）
     */
    private fun setCells(cellsMatrix: Array<Array<Cell>>, itemsPerRow: Int? = null) {
        if (cellsMatrix.isEmpty()) {
            this.cells = null
            this.rowCellCount = 0
            this.columnCellCount = 0
            requestLayout()
            invalidate()
            return
        }
        this.cells = cellsMatrix
        // 计算总元素数量
        val totalElements = cellsMatrix.sumOf { it.size }
        if (itemsPerRow != null) {
            // 指定每行元素数
            this.rowCellCount = itemsPerRow
            // 计算总行数（即需要多少行来容纳所有元素）
            this.columnCellCount = if (totalElements % itemsPerRow == 0) {
                totalElements / itemsPerRow + 1
            } else {
                totalElements / itemsPerRow
            }
        } else {
            // 不指定 itemsPerRow 时，按矩阵原有结构行列
            this.rowCellCount = cellsMatrix[0].size    // 第一行的元素数
            this.columnCellCount = cellsMatrix.size    // 总行数（外层数组长度）
        }
        requestLayout()
        invalidate()
    }

    /**
     * 设置数据：传入每行各自的 Cell 数组。
     * 例如：
     *   setData(
     *     arrayOf(Cell(...), Cell(...), Cell(...)),
     *     arrayOf(Cell(...), Cell(...), Cell(...))
     *   )
     */
    fun setData(vararg rows: Array<Cell>) {
        // 直接把 vararg rows 当成二维数组
        setCells(rows as Array<Array<Cell>>)
    }

    /**
     * 扁平化数组 + 指定每行 itemCount 个 Cell
     * 1) 先计算总行数：rowCount = 向上取整(flatCells.size / perRow)
     * 2) 最后一行如果不足 perRow，也能正确存放剩余元素
     */
    fun setData(flatCells: Array<Cell>, itemsPerRow: Int) {
        val perRow = if (itemsPerRow > 0) itemsPerRow else 3
        // 通过向上取整得到行数
        val rowCount = (flatCells.size + perRow - 1) / perRow
        val matrix = Array(rowCount) { rowIndex ->
            // 计算当前行起始、结束下标
            val start = rowIndex * perRow
            val end = minOf(start + perRow, flatCells.size)
            // 如果最后一行不足 perRow，就取剩余的
            flatCells.copyOfRange(start, end)
        }
        setCells(matrix, perRow)
    }

    /**
     * 扁平化列表版本，内部复用上面的方法。
     */
    fun setData(flatList: List<Cell>, itemsPerRow: Int) {
        setData(flatList.toTypedArray(), itemsPerRow)
    }

    /**
     * 更新单个格子：如果 (row, column) 合法，就覆盖并重绘。
     */
    fun updateData(rowIndex: Int, columnIndex: Int, newCell: Cell) {
        val data = cells ?: return
        if (rowIndex in 0 until columnCellCount && columnIndex in 0 until rowCellCount) {
            data[rowIndex][columnIndex] = newCell
            invalidate()
        }
    }

    /**
     * 批量更新格子：传入 List，每个 Triple 是 (rowIndex, columnIndex, newCell)。
     */
    fun updateData(newCells: List<Triple<Int, Int, Cell>>) {
        val data = cells ?: return
        var didChange = false
        for ((r, c, newCell) in newCells) {
            if (r in 0 until columnCellCount && c in 0 until rowCellCount) {
                data[r][c] = newCell
                didChange = true
            }
        }
        if (didChange) invalidate()
    }

    /**
     * 批量更新（可变参数版）。
     */
    fun updateData(vararg newCells: Triple<Int, Int, Cell>) {
        updateData(newCells.toList())
    }

    fun resetSelection() {
        selectedRow = -1
        selectedColumn = -1
        invalidate()
    }

    /**
     * 外部直接强制指定行数和列数时会清掉数据。
     */
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
