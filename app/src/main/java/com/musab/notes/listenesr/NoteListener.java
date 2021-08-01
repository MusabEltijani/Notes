package com.musab.notes.listenesr;

import com.musab.notes.entites.Note;

public interface NoteListener {
    void onNoteClicked(Note note ,int position);
}
