/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.notepad;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.getbase.floatingactionbutton.FloatingActionButton;

/**
 * This Activity handles "editing" a note, where editing is responding to
 * {@link Intent#ACTION_VIEW} (request to view data), edit a note
 * {@link Intent#ACTION_EDIT}, create a note {@link Intent#ACTION_INSERT}, or
 * create a new note from the current contents of the clipboard {@link Intent#ACTION_PASTE}.
 * <p>
 * NOTE: Notice that the provider operations in this Activity are taking place on the UI thread.
 * This is not a good practice. It is only done here to make the code more readable. A real
 * application should use the {@link android.content.AsyncQueryHandler}
 * or {@link android.os.AsyncTask} object to perform operations asynchronously on a separate thread.
 * <p>
 * 此活动处理“编辑”注释，其中编辑是响应 {@link Intent#ACTION_VIEW}（查看数据的请求）、编辑注释 {@link Intent#ACTION_EDIT}、
 * 创建注释 {@link Intent#ACTION_INSERT} 或从剪贴板 {@link Intent#ACTION_PASTE} 的当前内容创建新注释。
 * 注意：请注意，此 Activity 中的提供程序操作在 UI 线程上进行。这不是一个好的做法。此处仅为了使代码更具可读性而执行此操作。
 * 实际应用程序应使用 {@link android.content.AsyncQueryHandler} 或 {@link android.os.AsyncTask} 对象在单独的线程上异步执行操作。
 */
public class NoteEditor extends Activity {
    // For logging and debugging purposes
    private static final String TAG = "NoteEditor";

    /*
     * Creates a projection that returns the note ID and the note contents.
     * 创建返回note ID 和note内容的投影。
     */
    private static final String[] PROJECTION =
            new String[]{
                    NotePad.Notes._ID,
                    NotePad.Notes.COLUMN_NAME_TITLE,
                    NotePad.Notes.COLUMN_NAME_NOTE
            };

    // A label for the saved state of the activity
    // Activity 的已保存状态的标签
    private static final String ORIGINAL_CONTENT = "origContent";

    // This Activity can be started by more than one action. Each action is represented
    // as a "state" constant
    // 此 Activity 可由多个操作启动。表示每个操作作为 “state” 常量
    private static final int STATE_EDIT = 0;
    private static final int STATE_INSERT = 1;

    // Global mutable variables
    private int mState;
    private Uri mUri;
    private Cursor mCursor;
    private EditText mText;
    private EditText mTitle;
    private String mOriginalContent;

    /**
     * Defines a custom EditText View that draws lines between each line of text that is displayed.
     * 定义一个自定义的 EditText View，用于在显示的每一行文本之间绘制线条。
     */
    public static class LinedEditText extends EditText {
        private Rect mRect;
        private Paint mPaint;

        // This constructor is used by LayoutInflater
        public LinedEditText(Context context, AttributeSet attrs) {
            super(context, attrs);

            // Creates a Rect and a Paint object, and sets the style and color of the Paint object.
            mRect = new Rect();
            mPaint = new Paint();
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(0x800000FF);
        }

        /**
         * This is called to draw the LinedEditText object
         * 调用此函数以绘制 LinedEditText 对象
         *
         * @param canvas The canvas on which the background is drawn.
         */
//        @Override
//        protected void onDraw(Canvas canvas) {
//
//            // Gets the number of lines of text in the View.
//            // 获取 View 中的文本行数。
//            int count = getLineCount();
//
//            // Gets the global Rect and Paint objects
//            // 获取全局 Rect 和 Paint 对象
//            Rect r = mRect;
//            Paint paint = mPaint;
//
//            /*
//             * Draws one line in the rectangle for every line of text in the EditText
//             * 在 EditText 中每行文本的矩形中绘制一行
//             */
//            for (int i = 0; i < count; i++) {
//
//                // Gets the baseline coordinates for the current line of text
//                // 获取当前文本行的基线坐标
//                int baseline = getLineBounds(i, r);
//
//                /*
//                 * Draws a line in the background from the left of the rectangle to the right,
//                 * at a vertical position one dip below the baseline, using the "paint" object
//                 * for details.
//                 * 在背景中从矩形左侧到右侧，在基线下方一个凹陷处的垂直位置绘制一条线，使用 “paint” 对象查看细节。
//                 */
//                canvas.drawLine(r.left, baseline + 1, r.right, baseline + 1, paint);
//            }
//
//            // Finishes up by calling the parent method
//            // 通过调用 parent 方法完成
//            super.onDraw(canvas);
//        }
    }

    /**
     * This method is called by Android when the Activity is first started. From the incoming
     * Intent, it determines what kind of editing is desired, and then does it.
     * 首次启动 Activity 时，Android 会调用此方法。从传入intent 中，它确定需要什么样的编辑，然后执行它。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * Creates an Intent to use when the Activity object's result is sent back to the
         * caller.
         */
        final Intent intent = getIntent();

        /*
         *  Sets up for the edit, based on the action specified for the incoming Intent.
         */

        // Gets the action that triggered the intent filter for this Activity
        final String action = intent.getAction();

        // For an edit action:
        if (Intent.ACTION_EDIT.equals(action)) {

            // Sets the Activity state to EDIT, and gets the URI for the data to be edited.
            // 将 Activity state 设置为 EDIT，并获取要编辑的数据的 URI。
            mState = STATE_EDIT;
            mUri = intent.getData();

            // For an insert or paste action:
        } else if (Intent.ACTION_INSERT.equals(action)
                || Intent.ACTION_PASTE.equals(action)) {

            // Sets the Activity state to INSERT, gets the general note URI, and inserts an
            // empty record in the provider
            mState = STATE_INSERT;
            mUri = getContentResolver().insert(intent.getData(), null);

            /*
             * If the attempt to insert the new note fails, shuts down this Activity. The
             * originating Activity receives back RESULT_CANCELED if it requested a result.
             * Logs that the insert failed.
             */
            if (mUri == null) {

                // Writes the log identifier, a message, and the URI that failed.
                Log.e(TAG, "Failed to insert new note into " + getIntent().getData());

                // Closes the activity.
                finish();
                return;
            }

            // Since the new entry was created, this sets the result to be returned
            // set the result to be returned.
            setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));

            // If the action was other than EDIT or INSERT:
        } else {

            // Logs an error that the action was not understood, finishes the Activity, and
            // returns RESULT_CANCELED to an originating Activity.
            Log.e(TAG, "Unknown action, exiting");
            finish();
            return;
        }

        /*
         * Using the URI passed in with the triggering Intent, gets the note or notes in
         * the provider.
         * Note: This is being done on the UI thread. It will block the thread until the query
         * completes. In a sample app, going against a simple provider based on a local database,
         * the block will be momentary, but in a real app you should use
         * android.content.AsyncQueryHandler or android.os.AsyncTask.
         * 使用与触发 Intent 一起传入的 URI，获取提供程序中的一个或多个 note。
         * 注意：这是在 UI 线程上完成的。它将阻塞线程，直到查询完成。
         * 在示例应用程序中，与基于本地数据库的简单提供程序相对，
         * 该块将是暂时的，但在实际应用程序中，您应该使用 android.content.AsyncQueryHandler 或 android.os.AsyncTask。
         */
        mCursor = managedQuery(
                mUri,         // The URI that gets multiple notes from the provider.
                PROJECTION,   // A projection that returns the note ID and note content for each note.
                null,         // No "where" clause selection criteria.
                null,         // No "where" clause selection values.
                null          // Use the default sort order (modification date, descending)
        );

        setContentView(R.layout.note_editor);

        // Gets a handle to the EditText in the the layout.
        mText = (EditText) findViewById(R.id.note);
        mTitle = (EditText) findViewById(R.id.title);
        // 保存按钮
        FloatingActionButton fabSaveNote = (FloatingActionButton) findViewById(R.id.save_note);
        fabSaveNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = mText.getText().toString();
                String title = mTitle.getText().toString();
                Log.v("title:", title);
                updateNote(text, title);
                finish();
            }
        });
        // 删除按钮
        FloatingActionButton fabDeleteNote = (FloatingActionButton) findViewById(R.id.delete_note);
        fabDeleteNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteNote();
                finish();
            }
        });
        /*
         * If this Activity had stopped previously, its state was written the ORIGINAL_CONTENT
         * location in the saved Instance state. This gets the state.
         */
        if (savedInstanceState != null) {
            mOriginalContent = savedInstanceState.getString(ORIGINAL_CONTENT);
        }
    }

    /**
     * This method is called when the Activity is about to come to the foreground. This happens
     * when the Activity comes to the top of the task stack, OR when it is first starting.
     * <p>
     * Moves to the first note in the list, sets an appropriate title for the action chosen by
     * the user, puts the note contents into the TextView, and saves the original text as a
     * backup.
     * 当 Activity 即将进入前台时，将调用此方法。
     * 当 Activity 到达任务堆栈的顶部或首次启动时，会发生这种情况。
     * <p> 移动到列表中的第一个注释，为用户选择的操作设置适当的标题，将注释内容放入 TextView 中，并将原始文本保存为备份。
     */
    @Override
    protected void onResume() {
        super.onResume();

        /*
         * mCursor is initialized, since onCreate() always precedes onResume for any running
         * process. This tests that it's not null, since it should always contain data.
         */
        if (mCursor != null) {
            // Requery in case something changed while paused (such as the title)
            mCursor.requery();

            /* Moves to the first record. Always call moveToFirst() before accessing data in
             * a Cursor for the first time. The semantics of using a Cursor are that when it is
             * created, its internal index is pointing to a "place" immediately before the first
             * record.
             */
            mCursor.moveToFirst();

            // Modifies the window title for the Activity according to the current Activity state.
            if (mState == STATE_EDIT) {
                // Set the title of the Activity to include the note title
                int colTitleIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
                String title = mCursor.getString(colTitleIndex);
                mTitle.setText(mCursor.getString(colTitleIndex));
                Resources res = getResources();
                String text = String.format(res.getString(R.string.title_edit), title);
                setTitle(text);
                // Sets the title to "create" for inserts
            } else if (mState == STATE_INSERT) {
                setTitle(getText(R.string.title_create));
            }

            /*
             * onResume() may have been called after the Activity lost focus (was paused).
             * The user was either editing or creating a note when the Activity paused.
             * The Activity should re-display the text that had been retrieved previously, but
             * it should not move the cursor. This helps the user to continue editing or entering.
             */

            // Gets the note text from the Cursor and puts it in the TextView, but doesn't change
            // the text cursor's position.
            int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
            String note = mCursor.getString(colNoteIndex);
            mText.setTextKeepState(note);

            // Stores the original note text, to allow the user to revert changes.
            if (mOriginalContent == null) {
                mOriginalContent = note;
            }

            /*
             * Something is wrong. The Cursor should always contain data. Report an error in the
             * note.
             */
        } else {
            setTitle(getText(R.string.error_title));
            mText.setText(getText(R.string.error_message));
        }
    }

    /**
     * This method is called when an Activity loses focus during its normal operation, and is then
     * later on killed. The Activity has a chance to save its state so that the system can restore
     * it.
     * <p>
     * Notice that this method isn't a normal part of the Activity lifecycle. It won't be called
     * if the user simply navigates away from the Activity.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save away the original text, so we still have it if the activity
        // needs to be killed while paused.
        outState.putString(ORIGINAL_CONTENT, mOriginalContent);
    }

    /**
     * This method is called when the Activity loses focus.
     * <p>
     * For Activity objects that edit information, onPause() may be the one place where changes are
     * saved. The Android application model is predicated on the idea that "save" and "exit" aren't
     * required actions. When users navigate away from an Activity, they shouldn't have to go back
     * to it to complete their work. The act of going away should save everything and leave the
     * Activity in a state where Android can destroy it if necessary.
     * <p>
     * If the user hasn't done anything, then this deletes or clears out the note, otherwise it
     * writes the user's work to the provider.
     */
    @Override
    protected void onPause() {
        super.onPause();

        /*
         * Tests to see that the query operation didn't fail (see onCreate()). The Cursor object
         * will exist, even if no records were returned, unless the query failed because of some
         * exception or error.
         *
         */
        if (mCursor != null) {

            // Get the current note text.
            String text = mText.getText().toString();
            String title = mTitle.getText().toString();
            int length = text.length();

            /*
             * If the Activity is in the midst of finishing and there is no text in the current
             * note, returns a result of CANCELED to the caller, and deletes the note. This is done
             * even if the note was being edited, the assumption being that the user wanted to
             * "clear out" (delete) the note.
             */
            if (isFinishing() && (length == 0)) {
                setResult(RESULT_CANCELED);
                deleteNote();

                /*
                 * Writes the edits to the provider. The note has been edited if an existing note was
                 * retrieved into the editor *or* if a new note was inserted. In the latter case,
                 * onCreate() inserted a new empty note into the provider, and it is this new note
                 * that is being edited.
                 */
            } else if (mState == STATE_EDIT) {
                // Creates a map to contain the new values for the columns
                updateNote(text, title);
            } else if (mState == STATE_INSERT) {
                updateNote(text, title);
                mState = STATE_EDIT;
            }
        }
    }

    /**
     * Replaces the current note contents with the text and title provided as arguments.
     *
     * @param text  The new note contents to use.
     * @param title The new note title to use
     */
    private final void updateNote(String text, String title) {

        // Sets up a map to contain values to be updated in the provider.
        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());

        // If the action is to insert a new note, this creates an initial title for it.
        if (title == null || title.equals("")) {
            Log.v("title:", title);
            title = "空标题";
        }
        values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
        // This puts the desired notes text into the map.
        values.put(NotePad.Notes.COLUMN_NAME_NOTE, text);

        /*
         * Updates the provider with the new values in the map. The ListView is updated
         * automatically. The provider sets this up by setting the notification URI for
         * query Cursor objects to the incoming URI. The content resolver is thus
         * automatically notified when the Cursor for the URI changes, and the UI is
         * updated.
         * Note: This is being done on the UI thread. It will block the thread until the
         * update completes. In a sample app, going against a simple provider based on a
         * local database, the block will be momentary, but in a real app you should use
         * android.content.AsyncQueryHandler or android.os.AsyncTask.
         */
        getContentResolver().update(
                mUri,    // The URI for the record to update.
                values,  // The map of column names and new values to apply to them.
                null,    // No selection criteria are used, so no where columns are necessary.
                null     // No where columns are used, so no where arguments are necessary.
        );


    }

    /**
     * This helper method cancels the work done on a note.  It deletes the note if it was
     * newly created, or reverts to the original text of the note i
     */
    private final void cancelNote() {
        if (mCursor != null) {
            if (mState == STATE_EDIT) {
                // Put the original note text back into the database
                mCursor.close();
                mCursor = null;
                ContentValues values = new ContentValues();
                values.put(NotePad.Notes.COLUMN_NAME_NOTE, mOriginalContent);
                getContentResolver().update(mUri, values, null, null);
            } else if (mState == STATE_INSERT) {
                // We inserted an empty note, make sure to delete it
                deleteNote();
            }
        }
        setResult(RESULT_CANCELED);
        finish();
    }

    /**
     * Take care of deleting a note.  Simply deletes the entry.
     */
    private final void deleteNote() {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
            getContentResolver().delete(mUri, null, null);
            mText.setText("");
        }
    }
}
