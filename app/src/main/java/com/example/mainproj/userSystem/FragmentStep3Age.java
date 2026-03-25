package com.example.mainproj.userSystem;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mainproj.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class FragmentStep3Age extends Fragment {

    private TextView tvBirthdate, tvAgeResult;
    private MaterialCardView cardBirthdate;
    private MaterialButton btnNext;

    private Calendar selectedDate = null;
    private int calculatedAge = 0;

    private OnAgeCompleteListener listener;

    public interface OnAgeCompleteListener {
        void onAgeNext(int age);
    }

    public void setOnAgeCompleteListener(OnAgeCompleteListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_step3_age, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvBirthdate = view.findViewById(R.id.tvBirthdate);
        tvAgeResult = view.findViewById(R.id.tvAgeResult);
        cardBirthdate = view.findViewById(R.id.cardBirthdate);
        btnNext = view.findViewById(R.id.btnNext);

        cardBirthdate.setOnClickListener(v -> showDatePicker());

        btnNext.setOnClickListener(v -> {
            if (selectedDate == null) {
                Toast.makeText(requireContext(), "Please select your birthdate", Toast.LENGTH_SHORT).show();
                return;
            }

            if (calculatedAge < 13) {
                Toast.makeText(requireContext(), "You must be at least 13 years old to sign up", Toast.LENGTH_SHORT).show();
                return;
            }

            if (listener != null) {
                listener.onAgeNext(calculatedAge);
            }
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -13);

        DatePickerDialog datePicker = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);

                    SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
                    tvBirthdate.setText(sdf.format(selectedDate.getTime()));

                    calculatedAge = calculateAge(selectedDate);
                    tvAgeResult.setText("Age: " + calculatedAge);
                    tvAgeResult.setVisibility(View.VISIBLE);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePicker.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePicker.show();
    }

    private int calculateAge(Calendar birthDate) {
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR);

        if (today.get(Calendar.DAY_OF_YEAR) < birthDate.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }

        return age;
    }
}
