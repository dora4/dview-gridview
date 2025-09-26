dview-gridview
![Release](https://jitpack.io/v/dora4/dview-gridview.svg)
--------------------------------

#### 运行效果
![网格控件](https://github.com/user-attachments/assets/5177233e-87cf-41bd-bedd-f9a5c9fe22a7)

#### 卡片
![DORA视图 源极之芯](https://github.com/user-attachments/assets/f4dd4c27-9e6b-4bc5-99a7-b384e92f075c)

#### 规范标准
此控件遵循《Dora View规范手册》 https://github.com/dora4/dview-template/blob/main/Naming_Convention_Guide.md

#### Gradle依赖配置

```groovy
// 添加以下代码到项目根目录下的build.gradle
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
// 添加以下代码到app模块的build.gradle
dependencies {
    implementation 'com.github.dora4:dview-gridview:1.20'
}
```

#### 使用方式
activity_grid_view.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<layout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    tools:context=".ui.GridViewActivity">

    <data>

    </data>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical">

        <dora.widget.DoraTitleBar
            android:id="@+id/titleBar"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@color/colorPrimary"
            app:dview_title="@string/common_title" />

        <dora.widget.DoraGridView
            android:id="@+id/gridView"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            app:dview_gv_horizontalSpacing="20dp"
            app:dview_gv_verticalSpacing="20dp"
            app:dview_gv_cellBgColor="@color/gray"
            app:dview_gv_enableInteraction="true"
            app:dview_gv_gridLineColor="@color/gray"
            app:dview_gv_selectedTextColor="@color/gold_yellow"
            app:dview_gv_selectedTextSize="20sp"
            app:dview_gv_selectionBorderColor="@color/gold_yellow"
            app:dview_gv_selectionBorderWidth="1dp"
            app:dview_gv_textColor="@color/white"
            app:dview_gv_textSize="20sp" />
    </LinearLayout>
</layout>
```
Kotlin代码。
```kt
binding.gridView.setData(
            arrayOf(
                Cell("A"), Cell("B"), Cell("A"), Cell("D"),
                Cell("E"), Cell("F"), Cell("G")
            ), 3
        )
        // 狸猫换太子
        binding.gridView.updateData(2, 0, Cell("C", Color.RED, Color.GRAY))
        binding.gridView.setOnCellSelectListener(object : DoraGridView.OnCellSelectListener {

            override fun onCellSelected(rowIndex: Int, columnIndex: Int, cell: Cell?) {
                showShortToast("选择格子($rowIndex,$columnIndex,${cell?.text ?: "无"})")
            }
        })
```
