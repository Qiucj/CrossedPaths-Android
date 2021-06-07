package com.ensure.privatetrac;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialogFragment;

public class CancelDialog extends AppCompatDialogFragment {
    private CancelDialogListener listener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Do you really want to clear all history?")
        .setMessage("History cleared is gone forever")
                .setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setNegativeButton("Go ahead", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        listener.deleteClicked();
                    }
                });
        return builder.create();
    }

    @Override
    public void onStart(){
        super.onStart();
        ((AlertDialog)getDialog()).getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.RedAlert));
    }

    public interface CancelDialogListener{
        void deleteClicked();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try{listener = (CancelDialogListener) context;}
        catch (ClassCastException e){
            throw new ClassCastException(context.toString() + "must immplement CancelDialogListner");
        }
    }
}
