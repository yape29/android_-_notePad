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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;

import com.example.android.notepad.contract.NotesContract;
import com.example.android.notepad.presenter.NoteEditorPresenter;

import java.util.ArrayList;
import java.util.List;

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
 * 此活动处理"编辑"注释，其中编辑是响应 {@link Intent#ACTION_VIEW}（查看数据的请求）、编辑注释 {@link Intent#ACTION_EDIT}、
 * 创建注释 {@link Intent#ACTION_INSERT} 或从剪贴板 {@link Intent#ACTION_PASTE} 的当前内容创建新注释。
 * 注意：请注意，此 Activity 中的提供程序操作在 UI 线程上进行。这不是一个好的做法。此处仅为了使代码更具可读性而执行此操作。
 * 实际应用程序应使用 {@link android.content.AsyncQueryHandler} 或 {@link android.os.AsyncTask} 对象在单独的线程上异步执行操作。
 */
public class NoteEditor extends Activity implements NotesContract.NoteEditorView {
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
    private NotesContract.NoteEditorPresenter mPresenter;

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

        mPresenter = new NoteEditorPresenter(this, this);

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

        setContentView(R.layout.note_editor);
        mText = (EditText) findViewById(R.id.note);
        mTitle = (EditText) findViewById(R.id.title);

        // 保存按钮
        FloatingActionButton fabSaveNote = (FloatingActionButton) findViewById(R.id.save_note);
        fabSaveNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.saveNote(mText.getText().toString(), mTitle.getText().toString());
                finish();
            }
        });

        // 删除按钮
        FloatingActionButton fabDeleteNote = (FloatingActionButton) findViewById(R.id.delete_note);
        fabDeleteNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.deleteNote();
            }
        });

        // 分类按钮
        FloatingActionButton fabAddClassify = (FloatingActionButton) findViewById(R.id.add_classify);
        fabAddClassify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.loadClassifies();
            }
        });

        /*
         * If this Activity had stopped previously, its state was written the ORIGINAL_CONTENT
         * location in the saved Instance state. This gets the state.
         */
        if (savedInstanceState != null) {
            mOriginalContent = savedInstanceState.getString(ORIGINAL_CONTENT);
        }

        // 加载笔记内容
        mPresenter.loadNote(mUri);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPresenter.loadNote(mUri);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mText != null) {
            String text = mText.getText().toString();
            String title = mTitle.getText().toString();
            mPresenter.saveNote(text, title);
        }
    }

    // 显示分类选择对话框
    @Override
    public void showClassifyDialog(Cursor classifyCursor, String currentClassify) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.dialog_style);
        View dialogView = getLayoutInflater().inflate(R.layout.classify_dialog, null);

        ListView listView = (ListView) dialogView.findViewById(R.id.classify_list);
        Button btnAddNew = (Button) dialogView.findViewById(R.id.btn_add_new);
        Button btnCancel = (Button) dialogView.findViewById(R.id.btn_cancel);

        final List<String> classifyList = new ArrayList<>();
        if (classifyCursor != null && classifyCursor.moveToFirst()) {
            do {
                classifyList.add(classifyCursor.getString(0));
            } while (classifyCursor.moveToNext());
            classifyCursor.close();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_single_choice,
                classifyList);
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        if (currentClassify != null && !currentClassify.isEmpty()) {
            int position = classifyList.indexOf(currentClassify);
            if (position != -1) {
                listView.setItemChecked(position, true);
                listView.setSelection(position);
            }
        }

        final AlertDialog dialog = builder.create();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedClassify = classifyList.get(position);
                mPresenter.updateNoteClassify(selectedClassify);
                dialog.dismiss();
            }
        });

        btnAddNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                showAddClassifyDialog();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
        dialog.setContentView(dialogView);
    }

    // 显示添加分类对话框
    private void showAddClassifyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.dialog_style);
        View dialogView = getLayoutInflater().inflate(R.layout.add_classify_dialog, null);
        final EditText editClassifyName = (EditText) dialogView.findViewById(R.id.edit_classify_name);
        Button btnConfirm = (Button) dialogView.findViewById(R.id.btn_confirm);
        Button btnCancel = (Button) dialogView.findViewById(R.id.btn_cancel);

        final AlertDialog dialog = builder.create();
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newClassify = editClassifyName.getText().toString().trim();
                if (!TextUtils.isEmpty(newClassify)) {
                    mPresenter.createNewClassify(newClassify);
                    dialog.dismiss();
                }
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
        dialog.setContentView(dialogView);
    }

    @Override
    public void showNote(Cursor cursor) {
        // 保存cursor引用以供后续使用
        mCursor = cursor;
        
        // 获取标题和内容的列索引
        int colTitleIndex = cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
        int colNoteIndex = cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
        
        // 获取标题和内容
        String title = cursor.getString(colTitleIndex);
        String note = cursor.getString(colNoteIndex);
        
        // 更新UI
        mText.setText(note);
        mTitle.setText(title);
        
        // 设置窗口标题
        if (mState == STATE_EDIT) {
            setTitle(getString(R.string.title_edit, title));
        } else if (mState == STATE_INSERT) {
            setTitle(getText(R.string.title_create));
        }
        
        // 保存原始内容用于比较
        if (mOriginalContent == null) {
            mOriginalContent = note;
        }
    }

    @Override
    public void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void finishView() {
        finish();
    }

    @Override
    public void updateTitle(String title) {
        setTitle(title);
    }

    @Override
    public void updateText(String text) {
        mText.setText(text);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPresenter != null) {
            mPresenter.onDestroy();
        }
    }
}
