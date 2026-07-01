package com.example.demo.dto.request;

public class OrganizerApplicationReviewRequest {

    private String reviewNote;
    private String reviewNoteDetail;

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public String getReviewNoteDetail() {
        return reviewNoteDetail;
    }

    public void setReviewNoteDetail(String reviewNoteDetail) {
        this.reviewNoteDetail = reviewNoteDetail;
    }
}
