package com.example.huangzilin.granddictionary;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SimpleAdapter;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    final static int MAX_PROGRESS = 100;
    String result;
    private  DictCvSQL dictCvSQL;
    private ListView listView;
    ArrayList<Map<String, Object>> list;
    private SimpleAdapter adapter;
    private ArrayList<String> words;
    private ArrayList<String> explanations;
    private TextView display;
    private Context mContext;
    private AlertDialog alertDialog = null;
    private AlertDialog.Builder dialogBuilder = null;
    String deleted_word;
    String modified_word;
    private ProgressDialog pd;
    public int progressStatus = 0;  // 记录进度对话框的完成百分比
    public int word_count = 100;
    public int has_word = 0;
    public int cur = 0;
    String cur_head;
    private boolean show_meaning;
    ArrayList<Integer> buttonId;

    //定义一个Handler用来更新页面：
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0x001:
                    showWords();
                    break;
                case 0x002:
                    pd.setProgress(progressStatus);
                    break;
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dictCvSQL = new DictCvSQL(this);
        //We have to tell the activity where the toolbar is
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setTitle("简明英文词典");
        actionBar.setSubtitle("中山大学");
        cur_head = "";
        show_meaning = false;
        mContext = this;
        display = (TextView)findViewById(R.id.display);
        long num = dictCvSQL.getCount();
        if(num != 0){
            getWords(cur_head);
            handler.sendEmptyMessage(0x001);
        }
        listView = (ListView) findViewById(R.id.listView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String[] cur_word = {(String) list.get(position).get("word")};
                Cursor cursor = dictCvSQL.query(null, "word=?", cur_word, null);
                cursor.moveToFirst();
                if(cursor.getCount() == 0){
                    display.setText("error");
                }
                else{
                    String cur_ex = cursor.getString(cursor.getColumnIndex("explanation"));
                    display.setText((String) list.get(position).get("word") + "\n" + cur_ex);
                }

            }
        });


        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, final View view, int position, long id) {
                PopupMenu popup = new PopupMenu(MainActivity.this, view);
                popup.getMenuInflater().inflate(R.menu.menu_pop, popup.getMenu());
                deleted_word = (String) list.get(position).get("word");
                modified_word = (String) list.get(position).get("word");
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){
                    @Override
                    public boolean onMenuItemClick(MenuItem item){
                        switch (item.getItemId()){
                            case R.id.delete:
                                delete_word(MainActivity.this.getWindow().getDecorView());
                                break;
                            case R.id.modify:
                                modify_word(MainActivity.this.getWindow().getDecorView());
                                break;
                        }
                        return true;
                    }
                });
                popup.show();
                return true;  //true: 只执行长按事件(ShortClick事件失效)
            }
        });

        LinearLayout ll = (LinearLayout)findViewById(R.id.letter);
        buttonId = new ArrayList<>();
        for(int i = 0;i<27;i++){
            final Button letter = new Button((MainActivity.this));
            final int id = Button.generateViewId();
            buttonId.add(id);
            letter.setId(id);
            if(i == 0){
                letter.setText("");
            }
            else{
                letter.setText(Character.toString((char) (i + 96)));
            }
            letter.setTextSize(30);
            letter.setTransformationMethod(null);
            letter.setBackgroundColor(Color.parseColor("#CD853F"));
            letter.setOnClickListener(new Button.OnClickListener(){
                @Override
                public void onClick(View view){
                    int cur_id = letter.getId();
                    for(int i=0;i<27;i++){
                        if(cur_id != buttonId.get(i)){
                            Button letter_ = (Button)findViewById(buttonId.get(i));
                            letter_.setBackgroundColor(Color.parseColor("#CD853F"));
                        }

                    }
                    letter.setBackgroundColor(Color.parseColor("#79CDCD"));
                    cur_head = letter.getText().toString();
                    if(show_meaning == false)
                        getWords(cur_head);
                    else
                        getWords_Meaning(cur_head);
                    handler.sendEmptyMessage(0x001);

                }
            });
            LinearLayout.LayoutParams textViewLP = new LinearLayout.LayoutParams(50, 120);
            textViewLP.setMargins(10,20,10,20);
            ll.addView(letter, textViewLP);
        }
    }

    public void resetButtonColor(){
        for(int i=0;i<27;i++){
            Button letter_ = (Button)findViewById(buttonId.get(i));
            letter_.setBackgroundColor(Color.parseColor("#CD853F"));
        }
    }

    public void add_word(View source){
        TableLayout studentForm = (TableLayout) getLayoutInflater()
                .inflate(R.layout.add_word, null);
        dialogBuilder = new android.app.AlertDialog.Builder(mContext);
        alertDialog = dialogBuilder
                // 设置图标
                .setIcon(R.mipmap.dict)
                // 设置对话框标题
                .setTitle("增加单词")
                // 设置对话框显示的View对象
                .setView(studentForm)
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EditText new_word = (EditText)alertDialog.findViewById(R.id.new_word);
                        EditText new_explanation = (EditText)alertDialog.findViewById(R.id.new_explanation);
                        EditText new_level = (EditText)alertDialog.findViewById(R.id.new_level);
                        CheckBox cover = (CheckBox)alertDialog.findViewById(R.id.cover);
                        String cur_word = new_word.getText().toString();
                        if(!cur_word.equals("")){
                            String cur_ex = new_explanation.getText().toString();
                            Integer cur_level = Integer.valueOf(new_level.getText().toString());
                            long time =  System.currentTimeMillis() / 1000;
                            ContentValues cv = new ContentValues();
                            cv.put("word", cur_word);
                            cv.put("explanation", cur_ex);
                            cv.put("level",cur_level);
                            cv.put("modified_time", time);
                            String[] cur_words = new String[]{cur_word};
                            Cursor cursor = dictCvSQL.query(null, "word=?", cur_words, null);
                            if(cursor.getCount() == 0 || (cursor.getCount() != 0 && cover.isChecked())){
                                dictCvSQL.insert(cv);
                                Toast.makeText(mContext, "成功添加", Toast.LENGTH_SHORT).show();
                                cur_head = "";
                                resetButtonColor();
                                if(show_meaning == false)
                                    getWords(cur_head);
                                else
                                    getWords_Meaning(cur_head);
                                handler.sendEmptyMessage(0x001);
                            }
                            else{
                                Toast.makeText(mContext, "单词已存在", Toast.LENGTH_SHORT).show();
                            }
                        }

                    }
                }).create();
        alertDialog.show();
    }

    public void modify_word(View source){
        dialogBuilder = new android.app.AlertDialog.Builder(mContext);
        dialogBuilder.setIcon(R.mipmap.dict)
                .setTitle("修改单词");
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.modify_word, null);
        dialogBuilder.setView(dialogView);

        final EditText new_word = (EditText)dialogView.findViewById(R.id.new_word);
        final EditText new_explanation = (EditText)dialogView.findViewById(R.id.new_explanation);
        final EditText new_level = (EditText)dialogView.findViewById(R.id.new_level);

        new_word.setText(modified_word);
        Cursor cursor = dictCvSQL.query(null, "word=?", new String[]{modified_word}, null);
        cursor.moveToFirst();
        new_explanation.setText(cursor.getString(cursor.getColumnIndex("explanation")));
        new_level.setText(cursor.getString(cursor.getColumnIndex("level")));
        dialogBuilder
                // 设置图标
                .setIcon(R.mipmap.dict)
                // 设置对话框标题
                .setTitle("修改单词")
                // 设置对话框显示的View对象
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String cur_word = new_word.getText().toString();
                        if(!cur_word.equals("")){
                            String cur_ex = new_explanation.getText().toString();
                            Integer cur_level = Integer.valueOf(new_level.getText().toString());
                            long time =  System.currentTimeMillis() / 1000;
                            ContentValues cv = new ContentValues();
                            cv.put("word", cur_word);
                            cv.put("explanation", cur_ex);
                            cv.put("level",cur_level);
                            cv.put("modified_time", time);
                            dictCvSQL.delete("word=?", new String[]{modified_word});
                            dictCvSQL.insert(cv);
                            if(show_meaning == false)
                                getWords(cur_head);
                            else
                                getWords_Meaning(cur_head);
                            handler.sendEmptyMessage(0x001);
                        }
                    }
                });
        alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    public void search_word(View source){
        TableLayout studentForm = (TableLayout) getLayoutInflater()
                .inflate(R.layout.search_word, null);
        dialogBuilder = new android.app.AlertDialog.Builder(mContext);
        alertDialog = dialogBuilder
                // 设置图标
                .setIcon(R.mipmap.dict)
                // 设置对话框标题
                .setTitle("查找单词")
                // 设置对话框显示的View对象add_word.xml
                .setView(studentForm)
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EditText new_word = (EditText)alertDialog.findViewById(R.id.searched_word);
                        String cur_word = new_word.getText().toString();
                        cur_head = "%" + cur_word;
                        resetButtonColor();
                        if(show_meaning == false)
                            getWords(cur_head);
                        else
                            getWords_Meaning(cur_head);
                        handler.sendEmptyMessage(0x001);
                    }
                }).create();
        alertDialog.show();
    }

    public void delete_word(View source){
        dialogBuilder = new android.app.AlertDialog.Builder(mContext);
        dialogBuilder.setIcon(R.mipmap.dict)
                     .setTitle("删除单词");
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.delete_word, null);
        dialogBuilder.setView(dialogView);

        TextView show = (TextView)dialogView.findViewById(R.id.deleted_infor);
        show.setText(show.getText().toString() + deleted_word + "?");
        dialogBuilder
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        dictCvSQL.delete("word=?", new String[]{deleted_word});
                        if(show_meaning == false)
                            getWords(cur_head);
                        else
                            getWords_Meaning(cur_head);
                        handler.sendEmptyMessage(0x001);
                    }
                }).create();
        alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.support, menu);
        return true;
    }

    public void showProgress(View source) {
        progressStatus = 0;   // 将进度条的完成进度重设为0
        has_word = 0;
        pd = new ProgressDialog(MainActivity.this);
        pd.setMax(MAX_PROGRESS);
        pd.setTitle("下载词典");   // 设置对话框的标题
        //pd.setMessage("耗时任务的完成百分比");  // 设置对话框显示的内容
        pd.setCancelable(false);   // 设置对话框不能用“取消”按钮关闭
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pd.setIndeterminate(false);          // 设置对话框的进度条是否显示进度
        pd.show(); // ③
        cur = 0;
        new Thread()   {
            public void run() {
                while (progressStatus < MAX_PROGRESS) {
                    progressStatus = MAX_PROGRESS  * has_word / word_count;   // 获取耗时操作的完成百分比
                    if(progressStatus > cur ){
                        cur = progressStatus;
                        handler.sendEmptyMessage(0x002); // 发送空消息到Handler
                    }

                }
                if (progressStatus >= MAX_PROGRESS) {  // 如果任务已经完成
                    pd.dismiss();   // 关闭对话框
                }
            }
        }.start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search:
                search_word(MainActivity.this.getWindow().getDecorView());
                break;
            case R.id.add:
                add_word(MainActivity.this.getWindow().getDecorView());
                break;
            case R.id.menu1:
                showProgress(MainActivity.this.getWindow().getDecorView());
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            result = getJson();
                            parserJSONtoDB(result);
                            cur_head = "";
                            getWords(cur_head);
                            handler.sendEmptyMessage(0x001);
                        }catch(Exception ex){
                            ex.printStackTrace();
                        }
                    }
                }.start();
                break;
            case R.id.menu2:
                if(item.isChecked()){
                    item.setChecked(false);
                    show_meaning = false;
                    display.setVisibility(View.VISIBLE);
                    getWords(cur_head);
                    handler.sendEmptyMessage(0x001);
                }
                else{
                    show_meaning = true;
                    item.setChecked((true));
                    display.setVisibility(View.GONE);
                    getWords_Meaning(cur_head);
                    handler.sendEmptyMessage(0x001);
                }
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public void showWords(){
        try {
            adapter = new SimpleAdapter(MainActivity.this, list,
                    R.layout.list_item, new String[] { "word" },
                    new int[] { R.id.word});
            listView.setAdapter(adapter);
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public void getWords(String filter){
        try {
            words = new ArrayList<>();
            Cursor cursor = dictCvSQL.query(null, "word like ?", new String[]{filter + "%"}, "word COLLATE NOCASE");
            while(cursor.moveToNext()){
                String word = cursor.getString(cursor.getColumnIndex("word"));
                words.add(word);
            }
            list = new  ArrayList<Map<String, Object>>();
            for (int i = 0; i < words.size(); i++) {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("word", words.get(i));
                list.add(map);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void getWords_Meaning(String filter){
        try {
            words = new ArrayList<>();
            explanations = new ArrayList<>();
            Cursor cursor = dictCvSQL.query(null, "word like ?", new String[]{filter + "%"}, "word COLLATE NOCASE");
            while(cursor.moveToNext()){
                String word = cursor.getString(cursor.getColumnIndex("word"));
                String explanation = cursor.getString(cursor.getColumnIndex("explanation"));
                words.add(word);
                explanations.add(explanation);
            }
            list = new  ArrayList<Map<String, Object>>();
            for (int i = 0; i < words.size(); i++) {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("word", words.get(i) + "\n" + explanations.get(i));
                list.add(map);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static String getInputStreamText(InputStream is) throws Exception {
        InputStreamReader isr = new InputStreamReader(is, "utf8");
        BufferedReader br = new BufferedReader(isr);
        StringBuilder sb=new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    public String getJson() throws Exception{
        String apiUrl = "http://172.18.187.9:8080/dict/";
        URL url= new URL(apiUrl);
        URLConnection open = url.openConnection();
        InputStream inputStream = open.getInputStream();
        return getInputStreamText(inputStream);
    }

    public void parserJSONtoDB(String str) {
        JSONObject wordObj = null;
        JSONArray wordArray = null;
        try{
            wordArray = new JSONArray(str);
        }catch (Exception ex){
            ex.printStackTrace();
        }
        int i;
        word_count = wordArray.length();
        for(i = 0; i < wordArray.length();i++){
            try {
                wordObj = wordArray.getJSONObject(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //对象转存到数据库
            long time =  System.currentTimeMillis() / 1000;
            ContentValues cv = new ContentValues();
            String[] cur_word = {wordObj.optString("word")};
            cv.put("word", wordObj.optString("word"));
            cv.put("explanation", wordObj.optString("explanation"));
            cv.put("level",wordObj.optInt("level"));
            cv.put("modified_time", time);
            if(dictCvSQL.getCount() == 0 ){
                dictCvSQL.insert(cv);
            }
            else{
                Cursor cursor = dictCvSQL.query(null, "word=?", cur_word, null);
                if(cursor.getCount()!=0)
                    dictCvSQL.update(cv, "word=?", cur_word);
                else
                    dictCvSQL.insert(cv);
            }
            has_word++;
        }

    }
}
