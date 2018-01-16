package com.bin.david.smarttable;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.bin.david.form.core.SmartTable;
import com.bin.david.form.data.ArrayTableData;
import com.bin.david.form.data.CellRange;
import com.bin.david.form.data.format.draw.TextDrawFormat;
import com.bin.david.form.data.style.FontStyle;
import com.bin.david.form.utils.DensityUtils;
import com.bin.david.smarttable.adapter.SheetAdapter;
import com.chad.library.adapter.base.BaseQuickAdapter;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import jxl.Cell;
import jxl.Range;
import jxl.Sheet;
import jxl.Workbook;

public class ExcelModeActivity extends AppCompatActivity {

    private SmartTable<String> table;
    private SheetAsyncTask sheetTask;
    private ExcelAsyncTask excelTask;
    private RecyclerView recyclerView;
    private String fileName = "c.xls";
    private  CellRange[] cellRanges;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_excel_table);
        FontStyle.setDefaultTextSize(DensityUtils.sp2px(this,15));
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false));
        table = (SmartTable<String>) findViewById(R.id.table);
        table.getConfig().setFixedYSequence(true);
        table.getConfig().setFixedXSequence(true);
        table.getConfig().setShowTableTitle(false);
        table.getConfig().setHorizontalPadding(DensityUtils.dp2px(this,10));
        table.getConfig().setColumnTitleHorizontalPadding(DensityUtils.dp2px(this,5));
        table.getConfig().setXSequenceBackgroundColor(ContextCompat.getColor(this,R.color.arc_bg));
        table.getConfig().setYSequenceBackgroundColor(ContextCompat.getColor(this,R.color.arc_bg));
        table.setZoom(true,3,0.5f);
        sheetTask = new SheetAsyncTask();
        sheetTask.execute();

    }


    //读取sheet
    public class SheetAsyncTask extends AsyncTask<Void,Void,List<String>>{

        @Override
        protected List<String> doInBackground(Void... voids) {

            try {
                List<String> list = new ArrayList<>();
                InputStream is = getAssets().open(fileName);
                Workbook workbook = Workbook.getWorkbook(is);
                int sheetNum = workbook.getNumberOfSheets();
                for(int i = 0;i < sheetNum;i++){
                    Sheet sheet = workbook.getSheet(i);
                    list.add(sheet.getName());
                }
                workbook.close();
                return list;
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<String> sheets) {
            recyclerView.setHasFixedSize(true);
            if(sheets!=null && sheets.size() >0) {
                final SheetAdapter sheetAdapter = new SheetAdapter(sheets);
                sheetAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                        sheetAdapter.setSelectPosition(position);
                        excelTask = new ExcelAsyncTask();
                        excelTask.execute(position);


                    }
                });
                recyclerView.setAdapter(sheetAdapter);
                excelTask = new ExcelAsyncTask();
                excelTask.execute(0);
            }


        }
    }


    public class ExcelAsyncTask extends AsyncTask<Integer,Void,String[][]>{

        @Override
        protected String[][] doInBackground(Integer... position) {

            try {
                int maxRow, maxColumn;
                cellRanges = null;
                InputStream is = getAssets().open(fileName);
                Workbook workbook = Workbook.getWorkbook(is);
                Sheet sheet = workbook.getSheet(position[0]);
                Range[] ranges = sheet.getMergedCells();
                if(ranges !=null) {
                    cellRanges = new CellRange[ranges.length];
                    for (int i = 0;i < ranges.length;i++) {
                        Range range =ranges[i];
                        CellRange cellRange = new CellRange(range.getTopLeft().getRow(),
                                range.getBottomRight().getRow(),
                                range.getTopLeft().getColumn(),range.getBottomRight().getColumn());
                        cellRanges[i] = cellRange;
                    }

                }
                maxRow = sheet.getRows();
                maxColumn =  sheet.getColumns();
                String[][] data = new String[maxRow][];
                for (int i = 0; i < maxRow; i++) {
                    String[] rows = new String[maxColumn];
                    for(int j = 0;j < maxColumn;j++){
                        Cell cell = sheet.getCell(j, i);
                        if(cell !=null){
                            rows[j] = cell.getContents();
                        }else{
                            rows[j] = null;
                        }
                    }
                    data[i] = rows;
                }
                workbook.close();
                String[][] newData = new String[maxColumn][maxRow];
                for (int i = 0; i < data.length; i++) { //转换一下
                    for (int j = 0; j < data[i].length; j++) {
                        newData[j][i] = data[i][j]; // 转置核心
                    }
                }
                return newData;

            } catch (Exception e) {

            }

            return null;
        }

        @Override
        protected void onPostExecute(String[][] data) {
            if(data ==null || data.length==0) {
                data = new String[26][50]; //美观
            }
            ArrayTableData<String> tableData = ArrayTableData.create(table, "Excel表", data, new TextDrawFormat<String>());
            tableData.setCellRangeAddresses(cellRanges);
            table.setTableData(tableData);

        }
    }

    @Override
    protected void onDestroy() {
        if(sheetTask !=null && !sheetTask.isCancelled()){
            sheetTask.cancel(true);
        }
        if(excelTask !=null && !excelTask.isCancelled()){
            excelTask.cancel(true);
        }
        super.onDestroy();
    }

}
