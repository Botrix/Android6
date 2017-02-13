package com.example.doodlz;


import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;

/**
 * // class for the Select Line Width dialog
 */
public class LineWidthDialogFragment extends DialogFragment {
    private ImageView widthImageView;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View lineWidthDialogView = getActivity().getLayoutInflater().inflate(R.layout.fragment_line_width, null);
        builder.setView(lineWidthDialogView);
        builder.setTitle(R.string.title_line_width_dialog);

        widthImageView = (ImageView) lineWidthDialogView.findViewById(R.id.widthImageView);

        // configure widthSeekBar
        final DoodleView doodleView = getDoodleFragment().getDoodleView();
        final SeekBar widthSeekBar = (SeekBar) lineWidthDialogView.findViewById(R.id.widthSeekBar);
        widthSeekBar.setOnSeekBarChangeListener(lineWidthChanged);
        widthSeekBar.setProgress(doodleView.getLineWidth());

        // add Set Line Width Button
        builder.setPositiveButton(R.string.button_set_line_width, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                doodleView.setLineWidth(widthSeekBar.getProgress());
            }
        });

        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        MainActivityFragment fragment = getDoodleFragment();
        if (fragment != null) {
            fragment.setDialogOnScreen(true);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        MainActivityFragment fragment = getDoodleFragment();
        if (fragment != null) {
            fragment.setDialogOnScreen(false);
        }
    }

    // return a reference to the MainActivityFragment
    private MainActivityFragment getDoodleFragment() {
        return (MainActivityFragment) getFragmentManager().findFragmentById(R.id.doodleFragment);
    }

    // OnSeekBarChangeListener for the SeekBar in the width dialog
    private final SeekBar.OnSeekBarChangeListener lineWidthChanged = new SeekBar.OnSeekBarChangeListener() {
        final Bitmap bitmap = Bitmap.createBitmap(400, 100, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap); // draws into bitmap

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            // configure a Paint object for the current SeekBar value
            Paint p = new Paint();
            p.setColor(getDoodleFragment().getDoodleView().getDrawingColor());
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeWidth(progress);

            // erase the bitmap and redraw the line
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                bitmap.eraseColor(getResources().getColor(android.R.color.transparent, getContext().getTheme()));
            } else {
                bitmap.eraseColor(Color.TRANSPARENT);
            }
            canvas.drawLine(30, 50, 370, 50, p);
            widthImageView.setImageBitmap(bitmap);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };
}
