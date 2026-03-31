package com.overseas.purchase.service;

import com.overseas.purchase.dto.CommunityBeautifyRequestDTO;
import com.overseas.purchase.dto.CommunityBeautifyResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommunityBeautifyService {

    private final CommunityBeautifyAdvisor beautifyAdvisor;

    public CommunityBeautifyResponseDTO beautify(CommunityBeautifyRequestDTO request) {
        return beautifyAdvisor.beautify(request);
    }
}
