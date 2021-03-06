package com.example.huangzilin.granddictionary;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public class DictCvSQL {
    private DBOpenHandler dbOpenHandler;
    public DictCvSQL(Context context) {
        this.dbOpenHandler = new DBOpenHandler(context, "dbDict.db3", null, 1);

    }

    public long insert(ContentValues cv) {
        SQLiteDatabase db = dbOpenHandler.getWritableDatabase();
        long ret = db.insert("dict", null, cv);
        db.close();
        return ret;
    }


    public int delete(String where, String[] whereArgs) {
        SQLiteDatabase db = dbOpenHandler.getWritableDatabase();
        int ret = db.delete("dict", where, whereArgs);
        db.close();
        return ret;//影响的记录数
    }

    public int update(ContentValues cv, String where, String[] whereArgs) {
        SQLiteDatabase db = dbOpenHandler.getWritableDatabase();
        int ret = db.update("dict", cv, where, whereArgs);
        db.close();
        return ret; //影响的记录数
    }

    public Cursor query( String[] projection, String where,
                        String[] whereArgs, String sortOrder){
        SQLiteDatabase db = dbOpenHandler.getReadableDatabase();
        Cursor cursor = db.query("dict", projection, where, whereArgs,null,null,sortOrder,null);
        // db.close();
        return cursor;
    }

    public Cursor rawQuery(String sql, String[] selction){
        SQLiteDatabase db = dbOpenHandler.getReadableDatabase();
        Cursor cursor = db.rawQuery(sql, selction);
        return cursor;
    }

    public long getCount() {//得到记录总数
        SQLiteDatabase db = dbOpenHandler.getReadableDatabase();
        Cursor cursor = db.rawQuery("select count(*) from dict", null);
        cursor.moveToFirst();
        db.close();
        return cursor.getLong(0);
    }
}
