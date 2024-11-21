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

import static com.example.android.notepad.NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE;
import static com.example.android.notepad.NotePad.Notes.COLUMN_NAME_STAR;

import com.example.android.notepad.NotePad;
import com.getbase.floatingactionbutton.FloatingActionButton;

import android.app.ListActivity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Displays a list of notes. Will display notes from the {@link Uri}
 * provided in the incoming Intent if there is one, otherwise it defaults to displaying the
 * contents of the {@link NotePadProvider}.
 *
 * NOTE: Notice that the provider operations in this Activity are taking place on the UI thread.
 * This is not a good practice. It is only done here to make the code more readable. A real
 * application should use the {@link android.content.AsyncQueryHandler} or
 * {@link android.os.AsyncTask} object to perform operations asynchronously on a separate thread.
 *
 * 显示注释列表。将显示来自传入 Intent 中提供的 {@link Uri} 的注释（如果有），否则默认显示 {@link NotePadProvider} 的内容。
 * 注意：请注意，此 Activity 中的提供程序操作在 UI 线程上进行。这不是一个好的做法。此处仅为了使代码更具可读性而执行此操作。
 * 实际应用程序应使用 {@link android.content.AsyncQueryHandler} 或 {@link android.os.AsyncTask} 对象在单独的线程上异步执行操作。
 */
public class NotesList extends ListActivity {

    // For logging and debugging
    private static final String TAG = "NotesList";

    /**
     * The columns needed by the cursor adapter
     */
    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID, // 0
            NotePad.Notes.COLUMN_NAME_TITLE, // 1
            COLUMN_NAME_MODIFICATION_DATE,
            NotePad.Notes.COLUMN_NAME_STAR
    };

    /** The index of the title column */
    private static final int COLUMN_INDEX_TITLE = 1;

    /**
     * onCreate is called when Android starts this Activity from scratch.
     * onCreate 在 Android 从头开始启动此活动时调用
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // list布局加载
        setContentView(R.layout.notelist_main);
        // 新增note
        FloatingActionButton fabAddNote = (FloatingActionButton) findViewById(R.id.add_note);
        fabAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Launch NoteEditor to add a new note
                Intent intent = new Intent(Intent.ACTION_INSERT, NotePad.Notes.CONTENT_URI);
                startActivity(intent);
            }
        });

        // 搜索note
        // 显示搜索框
        final FloatingActionButton showSearchBtn = (FloatingActionButton) findViewById(R.id.search_note);
        final CardView searchCardView = (CardView) findViewById(R.id.search_bar);
        showSearchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (searchCardView.getVisibility() == View.GONE){
                    searchCardView.setVisibility(View.VISIBLE);  // 显示搜索框

                }else {
                    searchCardView.setVisibility(View.GONE);
                }
            }
        });

        // 设置搜索按钮点击事件
        final EditText searchEditText = (EditText) findViewById(R.id.title_search);
        ImageButton searchBtn = (ImageButton) findViewById(R.id.search_button);
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String query = searchEditText.getText().toString().trim();
                searchNotes(query); // 执行搜索
            }
        });

        // The user does not need to hold down the key to use menu shortcuts.
        // 用户无需按住该键即可使用菜单快捷方式。
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        /* If no data is given in the Intent that started this Activity, then this Activity
         * was started when the intent filter matched a MAIN action. We should use the default
         * provider URI.
         *
         * 如果启动此活动的 Intent 中没有提供任何数据，则表示当 Intent 筛选条件与 MAIN 操作匹配时，
         * 此 Activity 已启动。我们应该使用默认的提供者 URI。
         */
        // Gets the intent that started this Activity.
        Intent intent = getIntent();

        // If there is no data associated with the Intent, sets the data to the default URI, which
        // accesses a list of notes.
        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI);
        }

        /*
         * Sets the callback for context menu activation for the ListView. The listener is set
         * to be this Activity. The effect is that context menus are enabled for items in the
         * ListView, and the context menu is handled by a method in NotesList.
         */
        getListView().setOnCreateContextMenuListener(this);

        /* Performs a managed query. The Activity handles closing and requerying the cursor
         * when needed.
         *
         * Please see the introductory note about performing provider operations on the UI thread.
         */
        final Cursor[] cursor = {managedQuery(
                getIntent().getData(),            // Use the default content URI for the provider.
                PROJECTION,                       // Return the note ID and title for each note.
                null,                             // No where clause, return all records.
                null,                             // No where clause, therefore no where column values.
                NotePad.Notes.DEFAULT_SORT_ORDER  // Use the default sort order.
        )};

        /*
         * The following two arrays create a "map" between columns in the cursor and view IDs
         * for items in the ListView. Each element in the dataColumns array represents
         * a column name; each element in the viewID array represents the ID of a View.
         * The SimpleCursorAdapter maps them in ascending order to determine where each column
         * value will appear in the ListView.
         */

        // The names of the cursor columns to display in the view, initialized to the title column
        String[] dataColumns = { NotePad.Notes.COLUMN_NAME_TITLE,
                COLUMN_NAME_MODIFICATION_DATE } ;

        // The view IDs that will display the cursor columns, initialized to the TextView in
        int[] viewIDs = { R.id.textTitle,
                R.id.textDate};

        // Creates the backing adapter for the ListView.
        final MyAdapter adapter
            = new MyAdapter(
                      this,                             // The Context for the ListView
                      R.layout.notelist_item4,          // Points to the XML for a list item
                cursor[0],                           // The cursor to get items from
                      dataColumns,
                      viewIDs
              );

        // Sets the ListView's adapter to be the cursor adapter that was just created.
        setListAdapter(adapter);

        // 设置多选模式
        final ListView listView = getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

        listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                adapter.setItemSelected(position, checked);

                int checkedCount = listView.getCheckedItemCount();
                if (checkedCount > 0) {
                    findViewById(R.id.delete_notes).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.delete_notes).setVisibility(View.GONE);
                }

                mode.setTitle(checkedCount + " selected");
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return true; // 不需要显示菜单
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
            }
        });

        final FloatingActionButton fabDelete = (FloatingActionButton) findViewById(R.id.delete_notes);
        // 设置删除按钮点击事件
        fabDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
                for (int i = 0; i < checkedItems.size(); i++) {
                    int position = checkedItems.keyAt(i);
                    if (checkedItems.valueAt(i)) {
                        Cursor selectedCursor = (Cursor) adapter.getItem(position);
                        long noteId = selectedCursor.getLong(selectedCursor.getColumnIndex(NotePad.Notes._ID));
                        Uri uri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, noteId);
                        getContentResolver().delete(uri, null, null);
                    }
                }
                adapter.clearSelection(); // 清空选中状态
                fabDelete.setVisibility(View.GONE);
            }
        });
        // 添加一个变量来标识当前是否只显示收藏的 notes
        final boolean[] isShowingFavorites = {false};

        // 收藏按钮事件
        final FloatingActionButton showStarNote = (FloatingActionButton) findViewById(R.id.show_star_note);
        showStarNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isShowingFavorites[0]) {
                    // 如果当前显示的是收藏的笔记，点击后切换到显示全部笔记
                    isShowingFavorites[0] = false;
                    // 查询所有的 notes
                    cursor[0] = managedQuery(
                            getIntent().getData(), // 默认 URI
                            PROJECTION, // 显示的列
                            null, // 不加筛选条件，查询所有
                            null, // 参数为空
                            NotePad.Notes.DEFAULT_SORT_ORDER // 排序
                    );
                } else {
                    // 如果当前显示的是所有笔记，点击后切换到只显示收藏的笔记
                    isShowingFavorites[0] = true;
                    // 查询只显示收藏的 notes (假设 star = 1 表示已收藏)
                    cursor[0] = managedQuery(
                            getIntent().getData(), // 默认 URI
                            PROJECTION, // 显示的列
                            NotePad.Notes.COLUMN_NAME_STAR + " = ?", // 筛选条件，star = 1
                            new String[] { "1" }, // 只获取收藏的笔记
                            NotePad.Notes.DEFAULT_SORT_ORDER // 排序
                    );
                }

                // 更新 ListView 的数据
                adapter.changeCursor(cursor[0]);
            }
        });

        // 设置自定义视图绑定器，用于修改时间格式显示。这里使用内部类来实现SimpleCursorAdapter.ViewBinder接口。
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                // 检查视图是否是TextView类型，因为我们要修改的是文本显示。
                if (view instanceof TextView) {
                    // 获取当前列的列名，以便判断是否需要特殊处理。
                    String columnName = cursor.getColumnName(columnIndex);

                    // 判断当前列是否是修改日期列（假设NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE是修改日期的列名）。
                    if (columnName.equals(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE)) {
                        // 从Cursor中获取修改日期的时间戳（毫秒为单位）。
                        long dateInMillis = cursor.getLong(columnIndex);

                        // 将视图转换为TextView，以便设置文本。
                        TextView textView = (TextView) view;

                        // 创建一个SimpleDateFormat对象，用于格式化日期。这里使用"yyyy-MM-dd HH:mm:ss"格式，并指定默认语言环境。
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

                        // 使用SimpleDateFormat格式化时间戳，并将结果设置为TextView的文本。
                        textView.setText(dateFormat.format(new Date(dateInMillis)));

                        // 返回true表示我们已经处理了该视图的值，不需要进一步处理。
                        return true;
                    }
                }

                // 如果视图不是TextView或者当前列不是修改日期列，返回false表示我们没有处理该视图的值，可能需要其他处理。
                return false;
            }
        });
    }
    private void getNotes(String query) {
        String selection = null;
        String[] selectionArgs = null;

        if (query != null && !query.isEmpty()) {
            selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ?";
            selectionArgs = new String[] { "%" + query + "%" };
        }

        // 执行查询，获取所有笔记
        Cursor cursor = getContentResolver().query(
                NotePad.Notes.CONTENT_URI,
                PROJECTION,
                selection,
                selectionArgs,
                NotePad.Notes.DEFAULT_SORT_ORDER
        );

        // 更新适配器
        MyAdapter adapter = (MyAdapter) getListAdapter();
        adapter.changeCursor(cursor);
    }
    private void searchNotes(String query) {
        // 如果输入为空，显示所有笔记
        if (query.isEmpty()) {
            getNotes(null);
            return;
        }

        // 构建查询条件
        String selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ?";
        String[] selectionArgs = new String[] { "%" + query + "%" };

        // 执行查询，获取匹配的数据
        Cursor cursor = getContentResolver().query(
                NotePad.Notes.CONTENT_URI,
                PROJECTION,
                selection,
                selectionArgs,
                NotePad.Notes.DEFAULT_SORT_ORDER
        );

        // 更新适配器的数据
        MyAdapter adapter = (MyAdapter) getListAdapter();
        adapter.changeCursor(cursor);
    }
    /**
     * This method is called when the user clicks a note in the displayed list.
     *
     * This method handles incoming actions of either PICK (get data from the provider) or
     * GET_CONTENT (get or create data). If the incoming action is EDIT, this method sends a
     * new Intent to start NoteEditor.
     * @param l The ListView that contains the clicked item
     * @param v The View of the individual item
     * @param position The position of v in the displayed list
     * @param id The row ID of the clicked item
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        // Constructs a new URI from the incoming URI and the row ID
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);

        // Gets the action from the incoming Intent
        String action = getIntent().getAction();

        // Handles requests for note data
        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {

            // Sets the result to return to the component that called this Activity. The
            // result contains the new URI
            setResult(RESULT_OK, new Intent().setData(uri));
        } else {

            // Sends out an Intent to start an Activity that can handle ACTION_EDIT. The
            // Intent's data is the note ID URI. The effect is to call NoteEdit.
            startActivity(new Intent(Intent.ACTION_EDIT, uri));
        }
    }
}
