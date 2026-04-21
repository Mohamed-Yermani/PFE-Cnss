package com.projet.cnss.exception;



import com.projet.cnss.dto.AiVerificationResult;
import lombok.Getter;

@Getter
public class AiVerificationException extends RuntimeException {

    private final AiVerificationResult verification;

    public AiVerificationException(String message, AiVerificationResult verification) {
        super(message);
        this.verification = verification;
    }
}
