package com.example.android.notepad.contract;

import android.database.Cursor;
import android.net.Uri;

public interface NotesContract {
    interface NotesView {
        void showNotes(Cursor cursor);
        void showError(String message);
        void openNoteEditor(Uri uri);
        void finishView();
        void refreshView();
        void updateListView(Cursor cursor);
        void showDeleteButton(boolean show);
    }

    interface NotesPresenter {
        void loadNotes();
        void loadNotesInClassify(String classifyName); 
        void loadClassifies();
        void deleteNote(Uri noteUri);
        void createNote();
        void onDestroy();
    }

    interface NoteEditorView {
        void showNote(Cursor cursor);
        void showError(String message);
        void finishView();
        void updateTitle(String title);
        void updateText(String text);
        void showClassifyDialog(Cursor classifyCursor, String currentClassify);
    }

    interface NoteEditorPresenter {
        void loadNote(Uri uri);
        void saveNote(String text, String title);
        void deleteNote();
        void loadClassifies();
        void updateNoteClassify(String classify);
        void createNewClassify(String classifyName);
        void onDestroy();
    }
} 