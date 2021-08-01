package com.musab.notes.activitys;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.musab.notes.R;
import com.musab.notes.adapter.NoteAdapter;
import com.musab.notes.database.NoteDatabase;
import com.musab.notes.entites.Note;
import com.musab.notes.listenesr.NoteListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NoteListener{

    public static final int REQUEST_CODE_ADD_NOTE = 1;
    public static final int REQUEST_CODE_UPDATE_NOTE = 2;
    public static final int REQUEST_CODE_SHOW_NOTE = 3;
    public static final int REQUEST_CODE_SELECT_IMAGE = 4;
    private static final int REQUEST_CODE_STORAEG_PREMISSSION = 5;

    private RecyclerView noteRecyclerView;
    private List<Note> noteList ;
    private NoteAdapter noteAdapter;

    private int note_clicked_position = -1;
    private AlertDialog add_url_dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Animation animation = AnimationUtils.loadAnimation(this,R.anim.animation_get_start);

        findViewById(R.id.text_note).setAnimation(animation);
        findViewById(R.id.search_layout).setAnimation(animation);
        findViewById(R.id.layout_quick_action).setAnimation(animation);

        ImageView crate_note = (ImageView) findViewById(R.id.add_note_main);
        crate_note.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(
                        new Intent(getApplicationContext(), CreateNoteActivity.class),
                        REQUEST_CODE_ADD_NOTE
                );
            }
        });
        crate_note.setAnimation(animation);

        noteRecyclerView = findViewById(R.id.recycler_view_notes);
        noteRecyclerView.setLayoutManager(
                new StaggeredGridLayoutManager(2,StaggeredGridLayoutManager.VERTICAL)
        );
         noteList = new ArrayList<>();
        noteAdapter = new NoteAdapter(getApplicationContext(),noteList,this);
        noteRecyclerView.setAdapter(noteAdapter);
        noteRecyclerView.setAnimation(animation);

       getNotes(REQUEST_CODE_SHOW_NOTE, false);

        EditText search_edit = findViewById(R.id.search_edit);
        search_edit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                noteAdapter.cancelTimer();
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (noteList.size() != 0){
                    noteAdapter.searchNote(editable.toString());
                }
            }
        });
        search_edit.setAnimation(animation);


        findViewById(R.id.add_notes).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(
                        new Intent(getApplicationContext(), CreateNoteActivity.class),
                        REQUEST_CODE_ADD_NOTE
                );
            }
        });

        findViewById(R.id.add_images).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(
                        getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(
                            MainActivity.this,
                            new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_CODE_STORAEG_PREMISSSION
                    );
                }else {
                    selectImage();
                }
            }
        });

        findViewById(R.id.add_web_link).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddUrlDialog();
            }
        });
    }

    private void selectImage(){
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (intent.resolveActivity(getPackageManager()) != null){
            startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAEG_PREMISSSION && grantResults.length > 0){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                selectImage();
            }else {
                Toast.makeText(this, "Permission Denied ", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private String getPathFormUri(Uri content){
        String file_path;
        Cursor cursor = getContentResolver()
                .query(content, null,null,null,null);
        if (cursor == null){
            file_path = content.getPath();
        }else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            file_path = cursor.getString(index);
            cursor.close();
        }
        return file_path;
    }

    private void getNotes(final int request_code, final boolean isNoteDelete){

        @SuppressLint("StaticFieldLeak")
        class GetNotesTask extends AsyncTask< Void,Void, List<Note>>{

            @Override
            protected List doInBackground(Void... voids) {

                return NoteDatabase.getNoteDatabase(getApplicationContext()).noteDao().getAllNotes();
            }

            @Override
            protected void onPostExecute(List<Note> notes) {
                super.onPostExecute(notes);
                if (request_code == REQUEST_CODE_SHOW_NOTE){
                    noteList.addAll(notes);
                    noteAdapter.notifyDataSetChanged();
                }else if (request_code == REQUEST_CODE_ADD_NOTE){
                    noteList.add(0, notes.get(0));
                    noteAdapter.notifyItemInserted(0);
                    noteRecyclerView.smoothScrollToPosition(0);
                }else if (request_code == REQUEST_CODE_UPDATE_NOTE){
                    noteList.remove(note_clicked_position);

                    if (isNoteDelete){
                        noteAdapter.notifyItemRemoved(note_clicked_position);
                    }else {
                        noteList.add(note_clicked_position , notes.get(note_clicked_position));
                        noteAdapter.notifyItemChanged(note_clicked_position);
                    }
                }
            }
        }
        new GetNotesTask().execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK) {
            getNotes(REQUEST_CODE_ADD_NOTE, false);
        }else if (requestCode == REQUEST_CODE_UPDATE_NOTE && resultCode == RESULT_OK){
            if (data != null){
                getNotes(REQUEST_CODE_UPDATE_NOTE , data.getBooleanExtra("isNoteDelete",false));
            }
        }else if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK){
            if (data != null){
                Uri selected_image_uri = data.getData();
                if (selected_image_uri != null){
                    try {
                        String selected_image_path = getPathFormUri(selected_image_uri);
                        Intent intent = new Intent(getApplicationContext() ,CreateNoteActivity.class);
                        intent.putExtra("isFormQuickAction",true);
                        intent.putExtra("quickActionType", "image");
                        intent.putExtra("imagePath", selected_image_path);
                        startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
                    }catch (Exception e){
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    @Override
    public void onNoteClicked(Note note, int position) {
        note_clicked_position = position;
        Intent intent = new Intent(getApplicationContext(),CreateNoteActivity.class);
        intent.putExtra("isViewOrUpdate", true);
        intent.putExtra("note", note);
        startActivityForResult(intent, REQUEST_CODE_UPDATE_NOTE);
    }

    private void showAddUrlDialog(){
        if (add_url_dialog == null){
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            View view = LayoutInflater.from(this).inflate(
                    R.layout.layout_add_url,
                    (ViewGroup)findViewById(R.id.layout_add_url)
            );
            builder.setView(view);
            add_url_dialog = builder.create();

            if (add_url_dialog.getWindow() != null){
                add_url_dialog .getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            final EditText url_edit = view.findViewById(R.id.url_edit);
            url_edit.requestFocus();

            view.findViewById(R.id.add_url).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (url_edit.getText().toString().trim().isEmpty()){
                        Toast.makeText(MainActivity.this, "Enter URL", Toast.LENGTH_SHORT).show();
                    }else if (!Patterns.WEB_URL.matcher(url_edit.getText().toString()).matches()){
                        Toast.makeText(MainActivity.this, "URL Not Valid", Toast.LENGTH_SHORT).show();
                    }else {
                        add_url_dialog.dismiss();
                        Intent intent = new Intent(getApplicationContext() ,CreateNoteActivity.class);
                        intent.putExtra("isFormQuickAction",true);
                        intent.putExtra("quickActionType", "URL");
                        intent.putExtra("URL", url_edit.getText().toString());
                        startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
                    }
                }
            });

            view.findViewById(R.id.console).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    add_url_dialog.dismiss();
                }
            });
        }

        add_url_dialog.show();
    }
}
