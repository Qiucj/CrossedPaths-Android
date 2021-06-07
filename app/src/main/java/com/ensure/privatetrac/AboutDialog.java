package com.ensure.privatetrac;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialogFragment;

public class AboutDialog  extends AppCompatDialogFragment {
    private AboutDialogListener listener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Info")
                .setItems(R.array.infos_array, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        listener.displayInfo(which);
                    }
                });
        return builder.create();
    }

    public interface AboutDialogListener{
        void displayInfo(int userChoice);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try{listener = (AboutDialog.AboutDialogListener) context;}
        catch (ClassCastException e){
            throw new ClassCastException(context.toString() + "must immplement AboutDialogListner");
        }
    }
}
