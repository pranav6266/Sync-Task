package com.pranav.synctask.activities;

import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.R;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.models.Space;
import com.pranav.synctask.ui.PairingViewModel;

public class PairingActivity extends AppCompatActivity {

    private TextInputEditText etInputCode;
    private Button btnPair, btnCopyUid;
    private TextView tvMyUid;
    private FirebaseUser currentUser;
    private ProgressBar progressBar;
    private View pairingLayout;
    private PairingViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing);

        viewModel = new ViewModelProvider(this).get(PairingViewModel.class);

        etInputCode = findViewById(R.id.et_partner_code);
        btnPair = findViewById(R.id.btn_pair);

        // New Views
        tvMyUid = findViewById(R.id.tv_my_uid_display);
        btnCopyUid = findViewById(R.id.btn_copy_my_uid);

        progressBar = findViewById(R.id.pairing_progress_bar);
        pairingLayout = findViewById(R.id.pairing_layout);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            tvMyUid.setText(currentUser.getUid());

            btnCopyUid.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("User ID", currentUser.getUid());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "ID Copied", Toast.LENGTH_SHORT).show();
            });
        }

        btnPair.setOnClickListener(v -> handleConnect());
        observeViewModel();
    }

    private void handleConnect() {
        String input = etInputCode.getText().toString().trim();
        if (TextUtils.isEmpty(input)) {
            etInputCode.setError("Please enter a code or ID");
            return;
        }

        if (input.length() == 6) {
            joinSpace(input);
        } else if (input.length() > 20) {
            linkPartner(input);
        } else {
            Toast.makeText(this, "Invalid format. Space codes are 6 chars, User IDs are longer.", Toast.LENGTH_LONG).show();
        }
    }

    private void joinSpace(String inviteCode) {
        if (currentUser == null) return;
        showLoading(true);
        viewModel.joinSpace(inviteCode, currentUser.getUid()).observe(this, result -> {
            if (result instanceof Result.Success) {
                showLoading(false);
                Toast.makeText(this, "Successfully joined space!", Toast.LENGTH_SHORT).show();
                finish();
            } else if (result instanceof Result.Error) {
                handleError(((Result.Error<Space>) result).exception);
            } else if (result instanceof Result.Loading) {
                showLoading(true);
            }
        });
    }

    private void linkPartner(String partnerUid) {
        if (currentUser == null) return;
        viewModel.linkPartner(partnerUid, currentUser.getUid());
    }

    private void observeViewModel() {
        viewModel.getLinkPartnerResult().observe(this, result -> {
            if (result instanceof Result.Success) {
                showLoading(false);
                Toast.makeText(this, "Personal link created successfully!", Toast.LENGTH_SHORT).show();
                finish();
            } else if (result instanceof Result.Error) {
                handleError(((Result.Error<Space>) result).exception);
            } else if (result instanceof Result.Loading) {
                showLoading(true);
            }
        });
    }

    private void handleError(Exception e) {
        showLoading(false);
        String msg = e != null ? e.getMessage() : "Unknown error";
        Snackbar.make(pairingLayout, "Connection failed: " + msg, Snackbar.LENGTH_LONG).show();
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnPair.setEnabled(false);
            etInputCode.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnPair.setEnabled(true);
            etInputCode.setEnabled(true);
        }
    }
}