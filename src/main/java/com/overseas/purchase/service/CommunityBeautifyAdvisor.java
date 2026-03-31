package com.overseas.purchase.service;

import com.overseas.purchase.dto.CommunityBeautifyRequestDTO;
import com.overseas.purchase.dto.CommunityBeautifyResponseDTO;

public interface CommunityBeautifyAdvisor {

    CommunityBeautifyResponseDTO beautify(CommunityBeautifyRequestDTO request);
}
