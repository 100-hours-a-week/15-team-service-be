package com.sipomeokjo.commitme.domain.credit.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.credit.config.AiCreditProperties;
import com.sipomeokjo.commitme.domain.credit.entity.AiCreditWallet;
import com.sipomeokjo.commitme.domain.credit.repository.AiCreditWalletRepository;
import com.sipomeokjo.commitme.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiCreditService {

    private final AiCreditWalletRepository aiCreditWalletRepository;
    private final AiCreditProperties aiCreditProperties;

    @Transactional
    public void initialize(User user) {
        aiCreditWalletRepository.save(
                AiCreditWallet.create(user, aiCreditProperties.getInitialCredit()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deduct(Long userId, long amount) {
        validatePositiveAmount(amount);
        if (aiCreditWalletRepository.deductIfSufficient(userId, amount) == 0) {
            throw new BusinessException(ErrorCode.AI_CREDIT_INSUFFICIENT);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refund(Long userId, long amount) {
        validatePositiveAmount(amount);
        if (aiCreditWalletRepository.refund(userId, amount) == 0) {
            throw new BusinessException(ErrorCode.AI_CREDIT_WALLET_NOT_FOUND);
        }
    }

    private void validatePositiveAmount(long amount) {
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
    }
}
