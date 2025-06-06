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
    private var rowCellCount: Int = 3
    private var columnCellCount: Int = 3
    private var enableInteraction: Boolean = false
    private var cells: Array<Array<Cell>>? = null
    private var selectedRow: Int = -1
    private var selectedColumn: Int = -1
    private var cellTextColor: Int = Color.WHITE
    private var cellTextSize: Float = 30f
    private var gridLineColor: Int = Color.GRAY
    private var selectionBorderColor: Int = Color.RED
    private var gridLineWidth: Float = 2f
    private var selectionBorderWidth: Float = 5f
    private var downX: Float = 0f
    private var downY: Float = 0f
    private var isPotentialClick: Boolean = false
    private var horizontalSpacing: Float
    private var verticalSpacing: Float
    private var onCellSelectListener: OnCellSelectListener? = null

    init {
        val density = context.resources.displayMetrics.density
        horizontalSpacing = 10 * density
        verticalSpacing = 10 * density
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

    private fun computeCellSize(): Float {
        val availWidth = measuredWidth.toFloat() - 2 * horizontalSpacing
        val availHeight = measuredHeight.toFloat() - 2 * verticalSpacing
        val cellWidth  = availWidth / rowCellCount
        val cellHeight = availHeight / columnCellCount
        return minOf(cellWidth, cellHeight)
    }

    private fun initPaints() {
        gridLinePaint.strokeWidth = gridLineWidth
        gridLinePaint.color = gridLineColor

        gridBgPaint.style = Paint.Style.FILL

        textPaint.color = cellTextColor
        textPaint.color = cellTextColor
        textPaint.textSize = cellTextSize

        selectionPaint.color = selectionBorderColor
        selectionPaint.strokeWidth = selectionBorderWidth
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val defaultSizeDp = 380f
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
        for (i in 0 until columnCellCount) {
            for (j in 0 until rowCellCount) {
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
        for (i in 0..rowCellCount) {
            val x = horizontalSpacing + i * cellSize
            canvas.drawLine(x, verticalSpacing, x, verticalSpacing + totalHeight, gridLinePaint)
        }
        for (j in 0..columnCellCount) {
            val y = verticalSpacing + j * cellSize
            canvas.drawLine(horizontalSpacing, y, horizontalSpacing + totalWidth, y, gridLinePaint)
        }
    }

    private fun drawGridText(canvas: Canvas) {
        val cellSize = computeCellSize()
        val data = cells
        for (i in 0 until columnCellCount) {
            for (j in 0 until rowCellCount) {
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
            val left   = horizontalSpacing + selectedRow * cellSize
            val top    = verticalSpacing   + selectedColumn * cellSize
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
                    val rowIndex = ((event.x - horizontalSpacing) / cellSize).toInt()
                    val columnIndex    = ((event.y - verticalSpacing)   / cellSize).toInt()
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

    private fun setCells(cells: Array<Array<Cell>>, rowCellCount: Int? = null) {
        this.cells = cells
        if (rowCellCount != null) {
            this.rowCellCount = rowCellCount
            this.columnCellCount = if (cells.size % this.rowCellCount == 0) cells.size / this.rowCellCount + 1 else cells.size / this.rowCellCount
        } else {
            this.rowCellCount = cells.size
            this.columnCellCount = if (cells.isNotEmpty()) cells[0].size else 0
        }
        requestLayout()
        invalidate()
    }

    /**
     * 设置数据。
     * @since 1.2
     */
    fun setData(vararg rows: Array<Cell>) {
        val arr = Array(rows.size) { i -> rows[i] }
        setCells(arr)
    }

    /**
     * 设置数据，扁平化数组或集合，指定每行 itemCount 个 Cell，
     * 如果总数不能被 itemCount 整除，则回退到默认每行 3 个。
     * @since 1.2
     */
    fun setData(flatCells: Array<Cell>, itemsPerRow: Int) {
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
        setCells(matrix, perRow)
    }

    /**
     * 设置数据，接受 List<Cell>，并指定每行 itemCount 个 Cell，
     * 如果总数不能被 itemCount 整除，则回退到默认每行 3 个。
     * @since 1.2
     */
    fun setData(flatList: List<Cell>, itemsPerRow: Int) {
        // 转成 Array<Cell> 并复用上面的方法
        setData(flatList.toTypedArray(), itemsPerRow)
    }

    /**
     * 更新单个格子：若坐标合法，则覆盖。
     */
    fun updateData(row: Int, column: Int, newCell: Cell) {
        val data = cells ?: return
        if (row in 0 until rowCellCount && column in 0 until columnCellCount) {
            data[row][column] = newCell
            invalidate()
        }
    }

    /**
     * 批量更新格子：传入一个 List，每个 Triple 的格式为 (rowIndex, columnIndex, newCell)。
     * 遍历时只更新那些坐标合法的项，最后统一 invalidate()。
     */
    fun updateData(updates: List<Triple<Int, Int, Cell>>) {
        val data = cells ?: return
        var didChange = false
        for ((r, c, newCell) in updates) {
            if (r in 0 until rowCellCount && c in 0 until columnCellCount) {
                data[r][c] = newCell
                didChange = true
            }
        }
        if (didChange) invalidate()
    }

    /**
     * 批量更新格子（可变参数版），相当于把 vararg 转成 List 然后调用上面方法。
     * 例如： updateCells(Triple(0,1,Cell(...)), Triple(2,2,Cell(...)))
     */
    fun updateData(vararg updates: Triple<Int, Int, Cell>) {
        updateData(updates.toList())
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
