package com.pranav.synctask.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.R;
import com.pranav.synctask.models.User;
import com.pranav.synctask.utils.FirebaseHelper;

public class PairingActivity extends AppCompatActivity {

    private TextView tvPartnerCode;
    private EditText etPartnerCode;
    private Button btnPair;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing);

        tvPartnerCode = findViewById(R.id.tv_partner_code);
        etPartnerCode = findViewById(R.id.et_partner_code);
        btnPair = findViewById(R.id.btn_pair);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            loadUserPartnerCode();
        }

        btnPair.setOnClickListener(v -> {
            String partnerCode = etPartnerCode.getText().toString().trim().toUpperCase();
            if (partnerCode.length() == 6) {
                pairWithPartner(partnerCode);
            } else {
                Toast.makeText(this, "Please enter a valid 6-character code.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserPartnerCode() {
        FirebaseHelper.getUser(currentUser.getUid(), new FirebaseHelper.UserCallback() {
            @Override
            public void onSuccess(User user) {
                tvPartnerCode.setText(user.getPartnerCode());
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(PairingActivity.this, "Could not load your code.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pairWithPartner(String partnerCode) {
        FirebaseHelper.pairUsers(currentUser.getUid(), partnerCode, new FirebaseHelper.PairingCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(PairingActivity.this, "Successfully paired!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(PairingActivity.this, MainActivity.class));
                finish();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(PairingActivity.this, "Pairing failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
