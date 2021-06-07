package com.ensure.privatetrac;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialogFragment;

public class SendDialog extends AppCompatDialogFragment {
    private SendDialog.SendDialogListener listener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Send digital tokens of your location history")
                .setMessage("in past 7 days to this group's administrator.\nPlease read linked group's privacy policy before send.\nSending implies you agree with the policy.")
                .setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {}
                })
                .setNegativeButton("Send", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        listener.sendConfirmedClicked();
                    }
                })
                .setNeutralButton("🔗", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        listener.showLink();
                    }
                });
        return builder.create();
    }

    public interface SendDialogListener{
        void sendConfirmedClicked();
        void showLink();
    }

    @Override
    public void onStart(){
        super.onStart();
        ((AlertDialog)getDialog()).getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.RedAlert));
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try{listener = (SendDialog.SendDialogListener) context;}
        catch (ClassCastException e){
            throw new ClassCastException(context.toString() + "must implement SendDialogListener");
        }
    }
}
