package com.ds.deliveryapp.clients.req;

public class ConfirmParcelRequest {
    private String note;
    private String confirmationSource;

    public ConfirmParcelRequest() {
    }

    public ConfirmParcelRequest(String note, String confirmationSource) {
        this.note = note;
        this.confirmationSource = confirmationSource;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getConfirmationSource() {
        return confirmationSource;
    }

    public void setConfirmationSource(String confirmationSource) {
        this.confirmationSource = confirmationSource;
    }
}
