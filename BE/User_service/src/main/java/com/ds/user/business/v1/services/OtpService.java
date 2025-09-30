package com.ds.user.business.v1.services;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {
    private static final String FIXED_OTP = "000000";
    private final Map<String, Instant> phoneToExpiry = new ConcurrentHashMap<>();
    private static final long TTL_SECONDS = 300; // 5 minutes

    public String sendOtp(String phone) {
        phoneToExpiry.put(phone, Instant.now().plusSeconds(TTL_SECONDS));
        return FIXED_OTP;
    }

    public boolean verifyOtp(String phone, String otp) {
        Instant expiry = phoneToExpiry.get(phone);
        if (expiry == null) return false;
        if (Instant.now().isAfter(expiry)) {
            phoneToExpiry.remove(phone);
            return false;
        }
        boolean ok = FIXED_OTP.equals(otp);
        if (ok) phoneToExpiry.remove(phone);
        return ok;
    }
}
