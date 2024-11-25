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

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
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

    // 默认显示笔记视图
    private boolean isShowingClassify = false;

    // 添加一个变量来标识当前是否只显示收藏的 notes
    final boolean[] isShowingFavorites = {false};

    // 添加标志来跟踪当前是否在分类下的笔记列表中
    private boolean isInClassifyNotes = false;
    private String currentClassifyName = null;

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
        // 初始化标志
        isInClassifyNotes = false;
        currentClassifyName = null;
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
        searchNote();

        // 展示分类
        FloatingActionButton showClassifyButton = (FloatingActionButton) findViewById(R.id.show_classify);
        showClassifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleView();
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
                      viewIDs,
                      false
              );

        // Sets the ListView's adapter to be the cursor adapter that was just created.
        setListAdapter(adapter);

        final ListView listView = getListView();
        setupMultiChoiceMode(adapter); // 设置多选模式
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


        // 收藏按钮事件
        showStaredNotes(cursor, adapter, currentClassifyName);
        // 事件戳
        timeShow(adapter);
    }

    private void searchNote() {
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
    }

    private void showStaredNotes(final Cursor[] cursor, final MyAdapter adapter, final String currentClassifyName) {
        final FloatingActionButton showStarNote = (FloatingActionButton) findViewById(R.id.show_star_note);
        showStarNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isShowingFavorites[0]) {
                    // 切换到显示全部笔记
                    isShowingFavorites[0] = false;

                    if (currentClassifyName != null) {
                        // 在分类下显示所有笔记
                        cursor[0] = getContentResolver().query(
                                getIntent().getData(),
                                PROJECTION,
                                NotePad.Notes.COLUMN_NAME_CLASSIFY_NAME + "=?",
                                new String[]{currentClassifyName},
                                NotePad.Notes.DEFAULT_SORT_ORDER
                        );
                    } else {
                        // 显示所有笔记
                        cursor[0] = managedQuery(
                                getIntent().getData(),
                                PROJECTION,
                                null,
                                null,
                                NotePad.Notes.DEFAULT_SORT_ORDER
                        );
                    }
                } else {
                    // 切换到只显示收藏的笔记
                    isShowingFavorites[0] = true;

                    if (currentClassifyName != null) {
                        // 在分类下只显示收藏的笔记
                        String selection = NotePad.Notes.COLUMN_NAME_STAR + "=? AND "
                                + NotePad.Notes.COLUMN_NAME_CLASSIFY_NAME + "=?";
                        String[] selectionArgs = new String[]{"1", currentClassifyName};

                        cursor[0] = getContentResolver().query(
                                getIntent().getData(),
                                PROJECTION,
                                selection,
                                selectionArgs,
                                NotePad.Notes.DEFAULT_SORT_ORDER
                        );
                    } else {
                        // 显示所有收藏的笔记
                        cursor[0] = managedQuery(
                                getIntent().getData(),
                                PROJECTION,
                                NotePad.Notes.COLUMN_NAME_STAR + "=?",
                                new String[]{"1"},
                                NotePad.Notes.DEFAULT_SORT_ORDER
                        );
                    }
                }

                // 更新 ListView 的数据
                adapter.changeCursor(cursor[0]);
            }
        });
    }

    private static void timeShow(MyAdapter adapter) {
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

    // 修改 toggleView 方法
    private void toggleView() {
        if (isShowingClassify) {
            // 切换到笔记视图
            showNotes();
        } else {
            // 切换到分类视图
            showClassify();
        }
    }
    private void showClassify() {
        // 查询所有分类
        Cursor classifyCursor = getContentResolver().query(
                NotePad.Classify.CONTENT_URI,
                new String[]{NotePad.Classify._ID, NotePad.Classify.COLUMN_NAME_NAME},
                null,
                null,
                NotePad.Classify.DEFAULT_SORT_ORDER
        );

        // 创建一个新的 MatrixCursor 来存储带有笔记数量的分类数据
        String[] columns = new String[]{
                NotePad.Classify._ID,
                NotePad.Classify.COLUMN_NAME_NAME,
                "note_count" // 添加笔记数量列
        };
        MatrixCursor matrixCursor = new MatrixCursor(columns);

        if (classifyCursor != null && classifyCursor.moveToFirst()) {
            do {
                long classifyId = classifyCursor.getLong(
                        classifyCursor.getColumnIndex(NotePad.Classify._ID));
                String classifyName = classifyCursor.getString(
                        classifyCursor.getColumnIndex(NotePad.Classify.COLUMN_NAME_NAME));

                // 查询该分类下的笔记数量
                Cursor notesCountCursor = getContentResolver().query(
                        NotePad.Notes.CONTENT_URI,
                        new String[]{"COUNT(*) AS count"},
                        NotePad.Notes.COLUMN_NAME_CLASSIFY_NAME + "=?",
                        new String[]{classifyName},
                        null
                );

                int noteCount = 0;
                if (notesCountCursor != null && notesCountCursor.moveToFirst()) {
                    noteCount = notesCountCursor.getInt(0);
                    notesCountCursor.close();
                }

                // 将数据添加到 MatrixCursor
                matrixCursor.addRow(new Object[]{classifyId, classifyName, noteCount});
            } while (classifyCursor.moveToNext());
            classifyCursor.close();
        }

        // 创建适配器
        MyAdapter adapter = new MyAdapter(
                this,
                R.layout.classifylist_item,
                matrixCursor,
                new String[]{NotePad.Classify.COLUMN_NAME_NAME, "note_count"},
                new int[]{R.id.classify_name, R.id.classify_contain},
                true
        );

        // 设置 ViewBinder 来自定义显示格式
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (view.getId() == R.id.classify_contain) {
                    TextView textView = (TextView) view;
                    int count = cursor.getInt(columnIndex);
                    textView.setText(count + "个笔记");
                    return true;
                }
                return false;
            }
        });

        setListAdapter(adapter);
        setupClassifyMultiChoiceMode(adapter);
        // 设置列表项点击事件
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = (Cursor) parent.getItemAtPosition(position);
                String classifyName = cursor.getString(
                        cursor.getColumnIndex(NotePad.Classify.COLUMN_NAME_NAME));

                // 显示该分类下的笔记
                showNotesInClassify(classifyName);
            }
        });

        // 更新状态
        isShowingClassify = true;
    }
    private void showNotes() {
        // 查询所有笔记
        Cursor[] cursor = {getContentResolver().query(
                NotePad.Notes.CONTENT_URI,
                PROJECTION,
                null,
                null,
                NotePad.Notes.DEFAULT_SORT_ORDER
        )};

        String[] dataColumns = {NotePad.Notes.COLUMN_NAME_TITLE, COLUMN_NAME_MODIFICATION_DATE};
        int[] viewIDs = {R.id.textTitle, R.id.textDate};

        MyAdapter adapter = new MyAdapter(
                this,
                R.layout.notelist_item4,
                cursor[0],
                dataColumns,
                viewIDs,
                false
        );

        setListAdapter(adapter);
        timeShow(adapter);
        setupMultiChoiceMode(adapter); // 设置多选模式
        // 重置标题
        setTitle(R.string.app_name);

        // 设置笔记列表的点击事件
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 构造新的 URI，包含笔记 ID
                Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);

                // 获取当前 Intent 的 action
                String action = getIntent().getAction();

                // 处理不同的 action
                if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {
                    // 返回选中的笔记 URI
                    setResult(RESULT_OK, new Intent().setData(uri));
                } else {
                    // 打开笔记编辑界面
                    startActivity(new Intent(Intent.ACTION_EDIT, uri));
                }
            }
        });

        // 更新状态
        isShowingClassify = false;
        isInClassifyNotes = false;
        currentClassifyName = null;
        showStaredNotes(cursor, adapter, currentClassifyName);

    }
    // 显示指定分类下的笔记
    // 显示指定分类下的笔记
    private void showNotesInClassify(String classifyName) {
        // 设置标志，表示进入了分类下的笔记列表
        isInClassifyNotes = true;
        currentClassifyName = classifyName;
        setTitle("分类：" + classifyName);
        // 查询指定分类下的笔记
        Cursor[] cursor = {getContentResolver().query(
                NotePad.Notes.CONTENT_URI,
                PROJECTION,
                NotePad.Notes.COLUMN_NAME_CLASSIFY_NAME + "=?",
                new String[]{classifyName},
                NotePad.Notes.DEFAULT_SORT_ORDER
        )};

        // 创建适配器
        String[] dataColumns = {NotePad.Notes.COLUMN_NAME_TITLE, COLUMN_NAME_MODIFICATION_DATE};
        int[] viewIDs = {R.id.textTitle, R.id.textDate};

        MyAdapter adapter = new MyAdapter(
                this,
                R.layout.notelist_item4,
                cursor[0],
                dataColumns,
                viewIDs,
                false
        );

        setListAdapter(adapter);
        showStaredNotes(cursor, adapter, currentClassifyName);
        timeShow(adapter);
        setupMultiChoiceMode(adapter); // 设置多选模式
        // 更新状态
        isShowingClassify = false;

        // 设置列表项点击事件
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 构造新的 URI，包含笔记 ID
                Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);

                // 获取当前 Intent 的 action
                String action = getIntent().getAction();

                // 处理不同的 action
                if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {
                    // 返回选中的笔记 URI
                    setResult(RESULT_OK, new Intent().setData(uri));
                } else {
                    // 打开笔记编辑界面
                    startActivity(new Intent(Intent.ACTION_EDIT, uri));
                }
            }
        });
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
    @Override
    public void onBackPressed() {
        if (isInClassifyNotes) {
            // 如果当前在分类下的笔记列表中，返回到分类视图
            showClassify();
            isInClassifyNotes = false;
            currentClassifyName = null;
        } else {
            // 否则执行默认的返回操作
            super.onBackPressed();
        }
    }
    // 将多选模式设置抽取为一个方法
    private void setupMultiChoiceMode(final MyAdapter adapter) {
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
                return true;
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
                adapter.clearSelection();
                findViewById(R.id.delete_notes).setVisibility(View.GONE);
            }
        });
    }
    // 为分类视图添加多选模式
    private void setupClassifyMultiChoiceMode(final MyAdapter adapter) {
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
                return true;
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
                adapter.clearSelection();
                findViewById(R.id.delete_notes).setVisibility(View.GONE);
            }
        });

        // 设置删除按钮点击事件
        final FloatingActionButton fabDelete = (FloatingActionButton) findViewById(R.id.delete_notes);
        fabDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 显示确认对话框
                new AlertDialog.Builder(NotesList.this)
                        .setTitle("删除分类")
                        .setMessage("确定要删除选中的分类吗？这将同时删除分类下的所有笔记。")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteSelectedClassifies(listView, adapter);
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });
    }
    private void deleteSelectedClassifies(ListView listView, MyAdapter adapter) {
        SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
        for (int i = 0; i < checkedItems.size(); i++) {
            int position = checkedItems.keyAt(i);
            if (checkedItems.valueAt(i)) {
                Cursor cursor = (Cursor) adapter.getItem(position);
                String classifyName = cursor.getString(
                        cursor.getColumnIndex(NotePad.Classify.COLUMN_NAME_NAME));

                try {
                    // 首先删除该分类下的所有笔记
                    getContentResolver().delete(
                            NotePad.Notes.CONTENT_URI,
                            NotePad.Notes.COLUMN_NAME_CLASSIFY_NAME + "=?",
                            new String[]{classifyName}
                    );

                    // 然后删除分类
                    getContentResolver().delete(
                            NotePad.Classify.CONTENT_URI,
                            NotePad.Classify.COLUMN_NAME_NAME + "=?",
                            new String[]{classifyName}
                    );
                } catch (Exception e) {
                    Log.e("NotesList", "Error deleting classify: " + e.getMessage());
                }
            }
        }

        // 清空选中状态
        adapter.clearSelection();
        findViewById(R.id.delete_notes).setVisibility(View.GONE);

        // 刷新分类列表
        showClassify();
    }
}
