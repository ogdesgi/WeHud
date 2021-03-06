package com.wehud.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;

import com.wehud.R;

import java.util.Calendar;

/**
 * A generic class that can be used to display a dialog window
 * containing Date and Time pickers to let the user select a
 * precise date and time.
 *
 * @author Olivier Gonçalves, 2017
 */
public final class DateTimePickerDialogFragment extends DialogFragment {

    private static final String KEY_ID = "key_id";
    private static final String KEY_TITLE = "key_title";

    private static OnDateTimePickListener mListener;

    private static DateTimePickerDialogFragment newInstance() {
        return new DateTimePickerDialogFragment();
    }

    public void setOnDatePickListener(OnDateTimePickListener listener) {
        mListener = listener;
    }

    /**
     * Creates the dialog window using the provided parameters.
     *
     * @param manager   a {@link FragmentManager} object
     * @param listener  a {@link OnDateTimePickListener} interface instance
     * @param title     a unique {@link String} serving as header for the dialog
     * @param id        a unique identifier for an instance of this class
     */
    public static void generate(FragmentManager manager, OnDateTimePickListener listener,
                                String title, Object id
    ) {
        Bundle args = new Bundle();
        if (id instanceof Long)
            args.putLong(KEY_ID, (long) id);
        if (id instanceof Integer)
            args.putInt(KEY_ID, (int) id);
        if (id instanceof Short)
            args.putShort(KEY_ID, (short) id);
        if (id instanceof Double)
            args.putDouble(KEY_ID, (double) id);
        if (id instanceof Float)
            args.putFloat(KEY_ID, (float) id);
        if (id instanceof Byte)
            args.putByte(KEY_ID, (byte) id);
        if (id instanceof String)
            args.putString(KEY_ID, id.toString());
        if (id instanceof Character)
            args.putChar(KEY_ID, (char) id);
        if (id instanceof Boolean)
            args.putBoolean(KEY_ID, (boolean) id);

        args.putString(KEY_TITLE, title);

        DateTimePickerDialogFragment datePickerDialog = DateTimePickerDialogFragment.newInstance();
        datePickerDialog.setArguments(args);
        datePickerDialog.setOnDatePickListener(listener);
        datePickerDialog.show(manager, title);
    }

    @SuppressLint("InflateParams")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Calendar cal = Calendar.getInstance();
        final int year = cal.get(Calendar.YEAR);
        final int month = cal.get(Calendar.MONTH);
        final int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        final int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
        final int minute = cal.get(Calendar.MINUTE);

        final Context context = getActivity();
        final Bundle args = getArguments();
        final Object viewId = args.get(KEY_ID);
        final String title = args.getString(KEY_TITLE);

        final View headerView = LayoutInflater.from(context).inflate(R.layout.dialog_header, null);
        final View bodyView = LayoutInflater.from(context).inflate(R.layout.dialog_date_picker, null);

        final TextView titleView = (TextView) headerView.findViewById(R.id.dialog_title);
        if (!TextUtils.isEmpty(title)) titleView.setText(title);

        final DatePicker datePicker = (DatePicker) bodyView.findViewById(R.id.date_picker);
        datePicker.init(year, month, dayOfMonth, null);

        final TimePicker timePicker = (TimePicker) bodyView.findViewById(R.id.time_picker);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            timePicker.setCurrentHour(hourOfDay);
            timePicker.setCurrentMinute(minute);
        } else {
            timePicker.setHour(hourOfDay);
            timePicker.setMinute(minute);
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCustomTitle(headerView);
        builder.setView(bodyView);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                if (mListener != null) {
                    final int year = datePicker.getYear();
                    final int month = datePicker.getMonth();
                    final int dayOfMonth = datePicker.getDayOfMonth();
                    final int hourOfDay = (
                            Build.VERSION.SDK_INT < Build.VERSION_CODES.M ?
                                    timePicker.getCurrentHour() :
                                    timePicker.getHour()
                    );
                    final int minute = (
                            Build.VERSION.SDK_INT < Build.VERSION_CODES.M ?
                                    timePicker.getCurrentMinute() :
                                    timePicker.getMinute()
                    );

                    mListener.onDateTimePick(viewId, year, month, dayOfMonth, hourOfDay, minute);
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();
            }
        });

        return builder.create();
    }

    /**
     * This interface is used to get data from this class to another that implements it.
     */
    public interface OnDateTimePickListener {
        void onDateTimePick(final Object o, int i, int i1, int i2, int i3, int i4);
    }
}
