package com.example.android.notepad.presenter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import com.example.android.notepad.NotePad;
import com.example.android.notepad.contract.NotesContract;

public class NoteEditorPresenter implements NotesContract.NoteEditorPresenter {
    private NotesContract.NoteEditorView mView;
    private Context mContext;
    private Uri mUri;

    public NoteEditorPresenter(NotesContract.NoteEditorView view, Context context) {
        this.mView = view;
        this.mContext = context;
    }

    @Override
    public void loadNote(Uri uri) {
        this.mUri = uri;
        try {
            String[] projection = {
                NotePad.Notes._ID,
                NotePad.Notes.COLUMN_NAME_TITLE,
                NotePad.Notes.COLUMN_NAME_NOTE,
                NotePad.Notes.COLUMN_NAME_CLASSIFY_NAME
            };
            
            Cursor cursor = mContext.getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                mView.showNote(cursor);
            }
        } catch (Exception e) {
            e.printStackTrace();
            mView.showError("Error loading note");
        }
    }

    @Override
    public void saveNote(String text, String title) {
        try {
            ContentValues values = new ContentValues();
            values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());
            values.put(NotePad.Notes.COLUMN_NAME_NOTE, text);
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, title.isEmpty() ? "空标题" : title);
            
            mContext.getContentResolver().update(mUri, values, null, null);
            mView.updateTitle(title);
        } catch (Exception e) {
            mView.showError("Error saving note");
        }
    }

    @Override
    public void deleteNote() {
        try {
            mContext.getContentResolver().delete(mUri, null, null);
            mView.finishView();
        } catch (Exception e) {
            mView.showError("Error deleting note");
        }
    }

    @Override
    public void loadClassifies() {
        try {
            Cursor classifyCursor = mContext.getContentResolver().query(
                NotePad.Classify.CONTENT_URI,
                new String[]{NotePad.Classify.COLUMN_NAME_NAME},
                null,
                null,
                NotePad.Classify.DEFAULT_SORT_ORDER
            );

            String currentClassify = null;
            Cursor noteCursor = mContext.getContentResolver().query(mUri,
                new String[]{NotePad.Notes.COLUMN_NAME_CLASSIFY_NAME},
                null, null, null);

            if (noteCursor != null && noteCursor.moveToFirst()) {
                int columnIndex = noteCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_CLASSIFY_NAME);
                if (columnIndex != -1) {
                    currentClassify = noteCursor.getString(columnIndex);
                }
                noteCursor.close();
            }

            mView.showClassifyDialog(classifyCursor, currentClassify);
        } catch (Exception e) {
            mView.showError("Error loading classifies");
        }
    }

    @Override
    public void updateNoteClassify(String classify) {
        try {
            ContentValues values = new ContentValues();
            values.put(NotePad.Notes.COLUMN_NAME_CLASSIFY_NAME, classify);
            mContext.getContentResolver().update(mUri, values, null, null);
        } catch (Exception e) {
            mView.showError("Error updating classify");
        }
    }

    @Override
    public void createNewClassify(String classifyName) {
        try {
            ContentValues values = new ContentValues();
            values.put(NotePad.Classify.COLUMN_NAME_NAME, classifyName);
            mContext.getContentResolver().insert(NotePad.Classify.CONTENT_URI, values);
            updateNoteClassify(classifyName);
        } catch (SQLException e) {
            mView.showError("该分类已存在");
        } catch (Exception e) {
            mView.showError("Error creating classify");
        }
    }

    @Override
    public void onDestroy() {
        mView = null;
    }
} 