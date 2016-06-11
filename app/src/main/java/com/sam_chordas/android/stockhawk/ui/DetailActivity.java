package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.db.chart.Tools;
import com.db.chart.model.LineSet;
import com.db.chart.view.AxisController;
import com.db.chart.view.ChartView;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class  DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    //Variable Declaration
    private final String TAG = DetailActivity.class.getSimpleName();

    String quoteID="";
    private static final int CURSOR_LOADER_ID = 1;

    LineChartView chartView;
    private ArrayList<String> labels;
    private ArrayList<Float> values;
    private String quoteSymbol;
    private String quoteBidPrice;
    private int rangeMin, rangeMax;

    private String[] mLabels;
    private float[] mValues;

    //Variable Declaration

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);



        quoteID= getIntent().getStringExtra("stock_id");

        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        chartView = (LineChartView) findViewById(R.id.linechart);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns.SYMBOL,
                        QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE,
                        QuoteColumns.CHANGE,
                        QuoteColumns.ISUP,
                        QuoteColumns.CREATED,
                        QuoteColumns.ISCURRENT},
                QuoteColumns._ID + " = ?",
                new String[]{quoteID},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        data.moveToFirst();
          quoteSymbol = data.getString(data.getColumnIndex(QuoteColumns.SYMBOL));
        quoteBidPrice = data.getString(data.getColumnIndex(QuoteColumns.BIDPRICE));
        TextView textView = (TextView) findViewById(R.id.stocktitle);
        textView.setAllCaps(true);
        textView.setText(quoteSymbol +"      "+quoteBidPrice);
        textView.setTextSize(20);


        updateChart();

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        loader.reset();

    }

    private void updateChart() {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("http://chartapi.finance.yahoo.com/instrument/1.0/" + quoteSymbol + "/chartdata;type=close;range=1d/json")
                .build();

        client.newCall(request).enqueue(new Callback() {

                                            @Override
                                            public void onResponse(Response response) throws IOException {
                                                if (response.code() == 200) {
                                                    try {
                                                        String RANGE = "ranges";
                                                        String CLOSE = "close";
                                                        String MIN = "min";
                                                        String MAX = "max";
                                                        String SERIES = "series";
                                                        String TIMESTAMP = "Timestamp";
                                                        String result = response.body().string();
                                                        if (result.startsWith("finance_charts_json_callback( ")) {
                                                            result = result.substring(29, result.length() - 2);
                                                        }
                                                        JSONObject object = new JSONObject(result);

                                                        JSONObject range = object.getJSONObject(RANGE);
                                                        JSONObject close = range.getJSONObject(CLOSE);
                                                        rangeMin = close.getInt(MIN);
                                                        rangeMax = close.getInt(MAX);
                                                        labels = new ArrayList<>();
                                                        values = new ArrayList<>();
                                                        JSONArray series = object.getJSONArray(SERIES);
                                                        for (int i = 0; i < series.length(); i++) {
                                                            JSONObject temp = series.getJSONObject(i);

                                                            int unixTime = temp.getInt(TIMESTAMP);
                                                            long timestamp = unixTime * 1000;

                                                            Date d = new Date(timestamp);
                                                            SimpleDateFormat localDateFormat = new SimpleDateFormat("HH:mm");
                                                            String time = localDateFormat.format(d);
                                                            labels.add(time);
                                                            values.add(Float.parseFloat(temp.getString(CLOSE)));
                                                        }
                                                        CreateChart();

                                                    } catch (Exception e) {
                                                        Log.d(TAG, "Failed To Fetch Results. Please Try Again.");
                                                        e.printStackTrace();
                                                    }
                                                } else {
                                                    Log.d(TAG, "Failed To Fetch Results. Please Try Again.");
                                                }
                                            }

                                            private void CreateChart() {
                                                    DetailActivity.this.runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        mLabels = new String[labels.size()];
                                                        mValues = new float[values.size()];
                                                        labels.toArray(mLabels);
                                                        int i = 0;
                                                        for (Float f : values) {
                                                            if(f==null)
                                                                f=Float.NaN;
                                                            mValues[i++] = f;
                                                        }

                                                        LineSet set = new LineSet(mLabels, mValues);
                                                        set.setColor(Color.parseColor("#53c1bd"))
                                                                .setThickness(3);
                                                        chartView.addData(set);


                                                        Paint gridPaint = new Paint();
                                                        gridPaint.setColor(Color.parseColor("#ffffff"));
                                                        gridPaint.setStyle(Paint.Style.STROKE);
                                                        gridPaint.setAntiAlias(true);
                                                        gridPaint.setStrokeWidth(Tools.fromDpToPx(0));

                                                        chartView.setBorderSpacing(1)
                                                                .setXLabels(AxisController.LabelPosition.NONE)
                                                                .setYLabels(AxisController.LabelPosition.OUTSIDE)
                                                                .setXAxis(false)
                                                                .setYAxis(false)
                                                                .setGrid(ChartView.GridType.FULL, gridPaint)
                                                                .setAxisBorderValues(rangeMin - 1, rangeMax + 1)
                                                                .setBorderSpacing(Tools.fromDpToPx(1));



                                                        chartView.show();
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onFailure (Request request, IOException e){
                                                Log.d(TAG, "Some Error Occurred. Please Try Later.");
                                            }
                                        }

        );
    }
}
