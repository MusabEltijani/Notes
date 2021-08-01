package com.musab.notes.adapter;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.makeramen.roundedimageview.RoundedImageView;
import com.musab.notes.R;
import com.musab.notes.entites.Note;
import com.musab.notes.listenesr.NoteListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteHolder> {

    private Context context;
    private List<Note> noteList;
    private NoteListener noteListener;
    private Timer timer;
    private List<Note> noteSource;

    public NoteAdapter(Context context, List<Note> noteList, NoteListener noteListener) {
        this.context = context;
        this.noteList = noteList;
        this.noteListener = noteListener;
        noteSource = noteList;
    }


    @NonNull
    @Override
    public NoteHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NoteHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.note_items,
                        parent, false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull NoteHolder holder, final int position) {
        holder.setNote(noteList.get(position));
        holder.note_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                noteListener.onNoteClicked(noteList.get(position), position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return noteList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public class NoteHolder extends RecyclerView.ViewHolder {

        TextView note_title , note_sub_title, note_data_time;
        LinearLayout note_layout;
        RoundedImageView note_image;

        public NoteHolder(@NonNull View itemView) {
            super(itemView);

            note_title = itemView.findViewById(R.id.note_title);
            note_sub_title = itemView.findViewById(R.id.note_sub_title);
            note_data_time = itemView.findViewById(R.id.note_time);
            note_layout = itemView.findViewById(R.id.note_layout);
            note_image = itemView.findViewById(R.id.note_image);

        }

        void setNote(Note note){
            note_title.setText(note.getTitle());
            if (note.getSub_title().trim().isEmpty()){
               note_sub_title.setVisibility(View.GONE);
            }else {
                note_sub_title.setText(note.getSub_title());
            }
            note_data_time.setText(note.getData_time());

            GradientDrawable gradientDrawable = (GradientDrawable)note_layout.getBackground();
            if (note.getColor() != null){
                gradientDrawable.setColor(Color.parseColor(note.getColor()));
            }else {
                gradientDrawable.setColor(Color.parseColor("#333333"));
            }

            if (note.getImage_path() != null){
                note_image.setImageBitmap(BitmapFactory.decodeFile(note.getImage_path()));
                note_image.setVisibility(View.VISIBLE);
            }else {
                note_image.setVisibility(View.GONE);
            }
        }
    }

    public void searchNote(final String search_key_word){
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (search_key_word.trim().isEmpty()){
                    noteList = noteSource;
                }else {
                    ArrayList<Note> temp =new ArrayList<>();
                    for (Note note : noteSource) {
                        if (note.getTitle().toLowerCase().contains(search_key_word.toLowerCase())
                        || note.getSub_title().toLowerCase().contains(search_key_word.toLowerCase())
                        || note.getNote_text().toLowerCase().contains(search_key_word.toLowerCase())){
                            temp.add(note);
                        }
                    }
                    noteList = temp;
                }
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });
            }
        }, 500);
    }

    public void cancelTimer(){
        if (timer != null) {
            timer.cancel();
        }
    }
}
