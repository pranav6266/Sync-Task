package com.pranav.synctask.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.R;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.models.Space;
import com.pranav.synctask.ui.PairingViewModel;

public class PairingActivity extends AppCompatActivity {

    private EditText etPartnerCode;
    private Button btnPair;
    private FirebaseUser currentUser;
    private ProgressBar progressBar;
    private View pairingLayout;
    private PairingViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing);

        viewModel = new ViewModelProvider(this).get(PairingViewModel.class);

        etPartnerCode = findViewById(R.id.et_partner_code);
        btnPair = findViewById(R.id.btn_pair);
        progressBar = findViewById(R.id.pairing_progress_bar);
        pairingLayout = findViewById(R.id.pairing_layout);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        btnPair.setOnClickListener(v -> {
            String inviteCode = etPartnerCode.getText().toString().trim();
            if (inviteCode.length() == 6) {
                joinSpace(inviteCode);
            } else {
                Toast.makeText(this, "Please enter a valid 6-character code.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void joinSpace(String inviteCode) {
        if (currentUser == null) {
            Toast.makeText(this, "Error: Not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }
        showLoading(true);
        viewModel.joinSpace(inviteCode, currentUser.getUid()).observe(this, result -> {
            if (result instanceof Result.Success) {
                showLoading(false);
                Toast.makeText(PairingActivity.this, "Successfully joined space!", Toast.LENGTH_SHORT).show();
                finish(); // Close this activity and return to Dashboard
            } else if (result instanceof Result.Error) {
                Exception e = ((Result.Error<Space>) result).exception;
                Snackbar.make(pairingLayout, "Joining failed: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                showLoading(false);
            } else if (result instanceof Result.Loading) {
                showLoading(true);
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnPair.setEnabled(false);
            etPartnerCode.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnPair.setEnabled(true);
            etPartnerCode.setEnabled(true);
        }
    }
}