package com.example.android.notepad.presenter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.example.android.notepad.NotePad;
import com.example.android.notepad.contract.NotesContract;

public class NotesPresenter implements NotesContract.NotesPresenter {
    private NotesContract.NotesView mView;
    private Context mContext;

    public NotesPresenter(NotesContract.NotesView view, Context context) {
        this.mView = view;
        this.mContext = context;
    }

    @Override
    public void loadNotes() {
        try {
            String[] projection = {
                NotePad.Notes._ID,
                NotePad.Notes.COLUMN_NAME_TITLE,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
                NotePad.Notes.COLUMN_NAME_STAR
            };
            
            Cursor cursor = mContext.getContentResolver().query(
                NotePad.Notes.CONTENT_URI,
                projection,
                null,
                null,
                NotePad.Notes.DEFAULT_SORT_ORDER
            );
            
            mView.updateListView(cursor);
        } catch (Exception e) {
            mView.showError("Error loading notes");
        }
    }

    @Override
    public void loadNotesInClassify(String classifyName) {
        try {
            String[] projection = {
                NotePad.Notes._ID,
                NotePad.Notes.COLUMN_NAME_TITLE,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
                NotePad.Notes.COLUMN_NAME_STAR
            };
            
            Cursor cursor = mContext.getContentResolver().query(
                NotePad.Notes.CONTENT_URI,
                projection,
                NotePad.Notes.COLUMN_NAME_CLASSIFY_NAME + "=?",
                new String[]{classifyName},
                NotePad.Notes.DEFAULT_SORT_ORDER
            );
            
            mView.updateListView(cursor);
        } catch (Exception e) {
            mView.showError("Error loading notes in classify");
        }
    }

    @Override
    public void loadClassifies() {
        try {
            Cursor cursor = mContext.getContentResolver().query(
                NotePad.Classify.CONTENT_URI,
                new String[]{NotePad.Classify._ID, NotePad.Classify.COLUMN_NAME_NAME},
                null,
                null,
                NotePad.Classify.DEFAULT_SORT_ORDER
            );
            mView.updateListView(cursor);
        } catch (Exception e) {
            mView.showError("Error loading classifies");
        }
    }

    @Override
    public void deleteNote(Uri noteUri) {
        try {
            mContext.getContentResolver().delete(noteUri, null, null);
            mView.refreshView();
        } catch (Exception e) {
            mView.showError("Error deleting note");
        }
    }

    @Override
    public void createNote() {
        try {
            ContentValues values = new ContentValues();
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, "");
            values.put(NotePad.Notes.COLUMN_NAME_NOTE, "");
            Uri uri = mContext.getContentResolver().insert(NotePad.Notes.CONTENT_URI, values);
            if (uri != null) {
                mView.openNoteEditor(uri);
            }
        } catch (Exception e) {
            mView.showError("Error creating note");
        }
    }

    @Override
    public void onDestroy() {
        mView = null;
    }
} 