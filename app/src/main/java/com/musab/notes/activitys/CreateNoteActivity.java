package com.musab.notes.activitys;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ActionBarContextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;
import com.musab.notes.R;
import com.musab.notes.database.NoteDatabase;
import com.musab.notes.entites.Note;

import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

public class CreateNoteActivity extends AppCompatActivity {

    private EditText title, sub_title, note_text;
    private TextView data_time;
    private ImageView done;
    private View view_subtitle_indicator;
    private ImageView note_image;
    private LinearLayout web_link_layout;
    private TextView web_text;

    private String select_note_color;
    private String select_image_path;
    private static final int REQUEST_CODE_STORAEG_PREMISSSION = 1;
    private static final int REQUEST_CODE_SELECT_IMAGE = 2;

    private AlertDialog dialog_add_url;
    private AlertDialog dialog_delete_note;

    private Note already_available_note;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_note);

        ImageView image_back = (ImageView) findViewById(R.id.back_image);
        image_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        title = findViewById(R.id.note_title_edit);
        sub_title = findViewById(R.id.subtitle_note_edit);
        note_text = findViewById(R.id.note_edit);
        data_time = findViewById(R.id.data_time_text);
        view_subtitle_indicator = findViewById(R.id.subtitle_indicator_view);
        note_image = findViewById(R.id.note_image);
        web_link_layout = findViewById(R.id.web_link_layout);
        web_text = findViewById(R.id.web_text);

        data_time.setText(
                new SimpleDateFormat("EEEE ,dd MMMM yyyy HH:mm a", Locale.getDefault())
                .format(new Date())
        );

        done = findViewById(R.id.done_image);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveNote();
            }
        });

        select_note_color = "#333333";
        select_image_path = "";

        if (getIntent().getBooleanExtra("isViewOrUpdate",false)){
           already_available_note = (Note)getIntent().getSerializableExtra("note");
           setViewOrUpdateNote();
        }

        findViewById(R.id.image_remove_note).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    web_text.setText(null);
                    web_link_layout.setVisibility(View.GONE);
            }
        });

        findViewById(R.id.image_remove_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               note_image.setImageBitmap(null);
               note_image.setVisibility(View.GONE);
               findViewById(R.id.image_remove_image).setVisibility(View.GONE);
               select_image_path = "";
            }
        });

        if (getIntent().getBooleanExtra("isFormQuickAction",false)){
            String type = getIntent().getStringExtra("quickActionType");
            if (type != null){
                if (type.equals("image")){
                    select_image_path = getIntent().getStringExtra("imagePath");
                    note_image.setImageBitmap(BitmapFactory.decodeFile(select_image_path));
                    note_image.setVisibility(View.VISIBLE);
                    findViewById(R.id.image_remove_image).setVisibility(View.VISIBLE);
                }else if (type.equals("URL")){
                    web_text.setText(getIntent().getStringExtra("URL"));
                    web_link_layout.setVisibility(View.VISIBLE);
                }
            }
        }
        iniMiscellaneous();
        setSubTitleIndicator();

    }
    public void setViewOrUpdateNote(){

        title.setText(already_available_note.getTitle());
        sub_title.setText(already_available_note.getSub_title());
        note_text.setText(already_available_note.getNote_text());
        data_time.setText(already_available_note.getData_time());

        if (already_available_note.getImage_path() != null && !already_available_note.getImage_path().trim().isEmpty()){
            note_image.setImageBitmap(BitmapFactory.decodeFile(already_available_note.getImage_path()));
            note_image.setVisibility(View.VISIBLE);
            findViewById(R.id.image_remove_image).setVisibility(View.VISIBLE);
            select_image_path = already_available_note.getImage_path();
        }

        if (already_available_note.getWeb_link() != null && !already_available_note.getWeb_link().trim().isEmpty()){
            web_text.setText(already_available_note.getWeb_link());
            web_link_layout.setVisibility(View.VISIBLE);
        }
    }

    public void saveNote(){
        if (title.getText().toString().trim().isEmpty()){
            Toast.makeText(this, "Title note can`t be empty!", Toast.LENGTH_SHORT).show();
            return;

        }else if (sub_title.getText().toString().trim().isEmpty()
                && note_text.getText().toString().trim().isEmpty()){
            Toast.makeText(this, "Note can`t be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        final Note note = new Note();
        note.setTitle(title.getText().toString());
        note.setSub_title(sub_title.getText().toString());
        note.setNote_text(note_text.getText().toString());
        note.setData_time(data_time.getText().toString());
        note.setColor(select_note_color);
        note.setImage_path(select_image_path);

        if (web_link_layout.getVisibility() == View.VISIBLE){
            note.setWeb_link(web_text.getText().toString());
        }

        if (already_available_note!= null){
            note.setId(already_available_note.getId());
        }

        @SuppressLint("StaticFieldLeak")
        class saveNoteTask extends AsyncTask< Void, Void, Void>{

            @Override
            protected Void doInBackground(Void... voids) {
                NoteDatabase.getNoteDatabase(getApplicationContext()).noteDao().insertNote(note);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                Intent intent = new Intent();
                setResult(RESULT_OK,intent);
                finish();
            }
        }

        new saveNoteTask().execute();
    }

    private void iniMiscellaneous(){
        final LinearLayout linearLayout = findViewById(R.id.layout_miscellaneous);
        final BottomSheetBehavior<LinearLayout> bottomSheetBehavior = BottomSheetBehavior.from(linearLayout);
        linearLayout.findViewById(R.id.miscellaneous_text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bottomSheetBehavior.getState() != bottomSheetBehavior.STATE_EXPANDED){
                       bottomSheetBehavior.setState(bottomSheetBehavior.STATE_EXPANDED);
                }else {
                     bottomSheetBehavior.setState(bottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        });

        final ImageView image_color1 = linearLayout.findViewById(R.id.image_color1);
        final ImageView image_color2 = linearLayout.findViewById(R.id.image_color2);
        final ImageView image_color3 = linearLayout.findViewById(R.id.image_color3);
        final ImageView image_color4 = linearLayout.findViewById(R.id.image_color4);
        final ImageView image_color5 = linearLayout.findViewById(R.id.image_color5);

        linearLayout.findViewById(R.id.view_color1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                select_note_color = "#333333";
                image_color1.setImageResource(R.drawable.ic_done);
                image_color2.setImageResource(0);
                image_color3.setImageResource(0);
                image_color4.setImageResource(0);
                image_color5.setImageResource(0);
                setSubTitleIndicator();
            }
        });

        linearLayout.findViewById(R.id.view_color2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                select_note_color = "#FF4842";
                image_color1.setImageResource(0);
                image_color2.setImageResource(R.drawable.ic_done);
                image_color3.setImageResource(0);
                image_color4.setImageResource(0);
                image_color5.setImageResource(0);
                setSubTitleIndicator();
            }
        });

        linearLayout.findViewById(R.id.view_color3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                select_note_color = "#3A52Fc";
                image_color1.setImageResource(0);
                image_color2.setImageResource(0);
                image_color3.setImageResource(R.drawable.ic_done);
                image_color4.setImageResource(0);
                image_color5.setImageResource(0);
                setSubTitleIndicator();
            }
        });

        linearLayout.findViewById(R.id.view_color4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                select_note_color = "#000000";
                image_color1.setImageResource(0);
                image_color2.setImageResource(0);
                image_color3.setImageResource(0);
                image_color4.setImageResource(R.drawable.ic_done);
                image_color5.setImageResource(0);
                setSubTitleIndicator();
            }
        });

        linearLayout.findViewById(R.id.view_color5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                select_note_color = "#FDBE3B";
                image_color1.setImageResource(0);
                image_color2.setImageResource(0);
                image_color3.setImageResource(0);
                image_color4.setImageResource(0);
                image_color5.setImageResource(R.drawable.ic_done);
                setSubTitleIndicator();
            }
        });

        if (already_available_note != null && already_available_note.getColor() != null && !already_available_note.getColor().trim().isEmpty()){
            switch (already_available_note.getColor()){
                case "#FF4842":
                    linearLayout.findViewById(R.id.view_color2).performClick();
                    break;
                case "#3A52Fc":
                    linearLayout.findViewById(R.id.view_color3).performClick();
                    break;
                case "#000000":
                    linearLayout.findViewById(R.id.view_color4).performClick();
                    break;
                case "#FDBE3B":
                    linearLayout.findViewById(R.id.view_color5).performClick();
                    break;
            }
        }

        linearLayout.findViewById(R.id.add_image_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                if (ContextCompat.checkSelfPermission(
                        getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(
                            CreateNoteActivity.this,
                            new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_CODE_STORAEG_PREMISSSION
                    );
                }else {
                    selectImage();
                }
            }
        });

        linearLayout.findViewById(R.id.add_url_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                showAddUrlDialog();
            }
        });

        if (already_available_note != null){
            linearLayout.findViewById(R.id.delete_note_layout).setVisibility(View.VISIBLE);
            linearLayout.findViewById(R.id.delete_note_layout).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    showDialogDeleteNote();
                }
            });
        }
    }

    private void showDialogDeleteNote(){

        if (dialog_delete_note == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
            View view = LayoutInflater.from(this).inflate(
                    R.layout.layout_delete_note,
                    (ViewGroup) findViewById(R.id.layout_delete_note)
            );
            builder.setView(view);
            dialog_delete_note = builder.create();

            if (dialog_delete_note.getWindow() != null) {
                dialog_delete_note.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            view.findViewById(R.id.delete_text).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    class DeleteNoteTask extends AsyncTask<Void, Void, Void> {

                        @Override
                        protected Void doInBackground(Void... voids) {
                            NoteDatabase.getNoteDatabase(getApplicationContext()).noteDao()
                                    .delete(already_available_note);
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            super.onPostExecute(aVoid);
                            Intent intent = new Intent();
                            intent.putExtra("isNoteDelete", true);
                            setResult(RESULT_OK, intent);
                            finish();
                        }
                    }
                    new DeleteNoteTask().execute();
                }
            });
            view.findViewById(R.id.cancel_text).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog_delete_note.dismiss();
                }
            });
        }
       dialog_delete_note.show();
    }

    private void setSubTitleIndicator(){
        GradientDrawable gradientDrawable = (GradientDrawable)view_subtitle_indicator.getBackground();
        gradientDrawable.setColor(Color.parseColor(select_note_color));
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK){
            if (data != null){
                Uri selected_image_url = data.getData();
                if (selected_image_url != null){
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(selected_image_url);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        note_image.setImageBitmap(bitmap);
                        note_image.setVisibility(View.VISIBLE);
                         findViewById(R.id.image_remove_image).setVisibility(View.VISIBLE);
                        select_image_path = getPathFormUri(selected_image_url);
                    }catch (Exception e){
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }else {

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

    private void showAddUrlDialog(){
        if (dialog_add_url == null){
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
            View view = LayoutInflater.from(this).inflate(
                    R.layout.layout_add_url,
                    (ViewGroup)findViewById(R.id.layout_add_url)
            );
            builder.setView(view);
            dialog_add_url = builder.create();

            if (dialog_add_url.getWindow() != null){
                dialog_add_url .getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            final EditText url_edit = view.findViewById(R.id.url_edit);
            url_edit.requestFocus();

            view.findViewById(R.id.add_url).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (url_edit.getText().toString().trim().isEmpty()){
                        Toast.makeText(CreateNoteActivity.this, "Enter URL", Toast.LENGTH_SHORT).show();
                    }else if (!Patterns.WEB_URL.matcher(url_edit.getText().toString()).matches()){
                        Toast.makeText(CreateNoteActivity.this, "URL Not Valid", Toast.LENGTH_SHORT).show();
                    }else {
                        web_text.setText(url_edit.getText().toString());
                        web_link_layout.setVisibility(View.VISIBLE);
                        dialog_add_url.dismiss();
                    }
                }
            });

            view.findViewById(R.id.console).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog_add_url.dismiss();
                }
            });
        }

        dialog_add_url.show();
    }
}
