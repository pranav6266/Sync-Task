package com.pranav.synctask.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.R;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.models.User;
import com.pranav.synctask.ui.viewmodels.PairingViewModel;

public class PairingActivity extends AppCompatActivity {

    private TextView tvPartnerCode;
    private EditText etPartnerCode;
    private Button btnPair;
    private FirebaseUser currentUser;
    private ProgressBar progressBar;
    private View pairingLayout;
    private PairingViewModel viewModel;
    private final MutableLiveData<Result<User>> userPairingStatus = new MutableLiveData<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing);

        viewModel = new ViewModelProvider(this).get(PairingViewModel.class);

        [cite_start]tvPartnerCode = findViewById(R.id.tv_partner_code); 
        etPartnerCode = findViewById(R.id.et_partner_code);
        btnPair = findViewById(R.id.btn_pair);
        progressBar = findViewById(R.id.pairing_progress_bar);
        pairingLayout = findViewById(R.id.pairing_layout);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        [cite_start]if (currentUser != null) { 
            loadUserPartnerCode();
        }

        tvPartnerCode.setOnClickListener(v -> {
            [cite_start]ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE); 
            [cite_start]ClipData clip = ClipData.newPlainText("Partner Code", tvPartnerCode.getText().toString()); 
            [cite_start]clipboard.setPrimaryClip(clip); 
            [cite_start]Toast.makeText(this, "Code copied to clipboard!", Toast.LENGTH_SHORT).show(); 
        });

        [cite_start]btnPair.setOnClickListener(v -> { 
            String partnerCode = etPartnerCode.getText().toString().trim();
            if (partnerCode.length() == 6) {
                pairWithPartner(partnerCode);
            } else {
                [cite_start]Toast.makeText(this, "Please enter a valid 6-character code.", Toast.LENGTH_SHORT).show(); 
            }
        });

        observePairingStatus();
    }

    @Override
    protected void onStart() {
        super.onStart();
        [cite_start]if (currentUser != null) { 
            viewModel.attachUserListener(currentUser.getUid(), userPairingStatus);
        }
    }

    private void observePairingStatus() {
        userPairingStatus.observe(this, result -> {
            if (result instanceof Result.Success) {
                User user = ((Result.Success<User>) result).data;
                [cite_start]if (user.getPairedWithUID() != null && !user.getPairedWithUID().isEmpty()) { 
                    [cite_start]if (!isFinishing() && !isDestroyed()) { 
                        [cite_start]Toast.makeText(PairingActivity.this, "Partner connected!", Toast.LENGTH_SHORT).show(); 
                        startActivity(new Intent(PairingActivity.this, MainActivity.class));
                        finish();
                    }
                }
            } else if (result instanceof Result.Error) {
                [cite_start]Log.e("PairingActivity", "Error listening to user updates", ((Result.Error<User>) result).exception); 
            }
        });
    }


    private void loadUserPartnerCode() {
        showLoading(true);
        viewModel.getUser(currentUser.getUid()).observe(this, result -> {
            if (result instanceof Result.Success) {
                User user = ((Result.Success<User>) result).data;
                tvPartnerCode.setText(user.getPartnerCode());
                showLoading(false);
            } else if (result instanceof Result.Error) {
                [cite_start]Toast.makeText(PairingActivity.this, "Could not load your code.", Toast.LENGTH_SHORT).show(); 
                showLoading(false);
            }
        });
    }

    private void pairWithPartner(String partnerCode) {
        showLoading(true);
        viewModel.pairWithPartner(currentUser.getUid(), partnerCode).observe(this, result -> {
            if (result instanceof Result.Success) {
                showLoading(false);
                [cite_start]Toast.makeText(PairingActivity.this, "Pairing successful! Connecting...", Toast.LENGTH_SHORT).show(); 
            } else if (result instanceof Result.Error) {
                Exception e = ((Result.Error<Void>) result).exception;
                [cite_start]Snackbar.make(pairingLayout, "Pairing failed: " + e.getMessage(), Snackbar.LENGTH_LONG).show(); 
                showLoading(false);
            } else if (result instanceof Result.Loading) {
                showLoading(true);
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            [cite_start]btnPair.setEnabled(false); 
            [cite_start]etPartnerCode.setEnabled(false); 
        } else {
            progressBar.setVisibility(View.GONE);
            btnPair.setEnabled(true);
            etPartnerCode.setEnabled(true);
        }
    }
}