package za.co.cradle.PackhouseLabeling;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ListActivity extends AppCompatActivity {

    ListView listView;

    ArrayAdapter arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        listView = findViewById(R.id.listView);

        getData();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Object object = adapterView.getItemAtPosition(i);
                String solution = object.toString();

                SharedPreferences.Editor editor = getSharedPreferences("ListPrefs", MODE_PRIVATE).edit();
                editor.putString("solution", solution);
                editor.apply();

                finish();
            }
        });
    }

    public void getData() {
        File root = new File(Environment.getExternalStorageDirectory(), "CradleTechnologyServices" + File.separator + "CSV" + File.separator + "packing");
        File file = new File(root, "solutions.csv");

        try {
            CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(file)));
            reader.readNext();

            List<String> data = new ArrayList<>();
            String[] line;

            while ((line = reader.readNext()) != null) {
                data.add(line[0]);
            }

            reader.close();

            populateList(data);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void populateList(List<String> data) {
        Log.d("ListView", "" + data.toString());
        arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_expandable_list_item_1, data);
        listView.setAdapter(arrayAdapter);
        arrayAdapter.notifyDataSetChanged();
    }
}

