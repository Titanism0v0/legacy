package com.overseas.purchase.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.overseas.purchase.dto.CommunityPostCreateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class CommunityModerationServiceTest {

    private CommunityModerationService service;

    @BeforeEach
    void setUp() {
        service = new CommunityModerationService(new ObjectMapper());
        ReflectionTestUtils.setField(service, "enabled", false);
        ReflectionTestUtils.setField(service, "aiRequired", false);
        ReflectionTestUtils.setField(service, "provider", "openai-compatible");
        ReflectionTestUtils.setField(service, "model", "demo-model");
        ReflectionTestUtils.setField(service, "sensitiveBlockWords", "drugs,gun,explosive,terror,porn,child-sexual");
        ReflectionTestUtils.setField(service, "sensitiveReviewWords", "wechat,telegram,whatsapp,casino,private-transfer");
        ReflectionTestUtils.setField(service, "imageBlockKeywords", "weapon,gun,drugs,porn,explosive");
        ReflectionTestUtils.setField(service, "imageReviewKeywords", "wechat,whatsapp,telegram,casino,gamble");
        ReflectionTestUtils.setField(service, "imageAiEnabled", false);
        ReflectionTestUtils.setField(service, "thresholdAllow", new java.math.BigDecimal("0.30"));
        ReflectionTestUtils.setField(service, "thresholdBlock", new java.math.BigDecimal("0.75"));
    }

    @Test
    void allowsNormalMarketplaceContentWhenAiIsOptional() {
        CommunityModerationService.ModerationDecision result = service.moderatePost(post("分享一款咖啡杯", "正规渠道购入，支持平台担保交易"));

        assertThat(result.getDecision()).isEqualTo("ALLOW");
        assertThat(result.getRiskLevel()).isEqualTo("LOW");
    }

    @Test
    void sendsOffPlatformTradingLanguageToManualReview() {
        CommunityModerationService.ModerationDecision result = service.moderatePost(post("低价转让", "加微信私聊后直接转账"));

        assertThat(result.getDecision()).isEqualTo("REVIEW");
        assertThat(result.getRiskLevel()).isEqualTo("MEDIUM");
    }

    @Test
    void blocksChineseDrugAndWeaponContentEvenWithEnglishOnlyConfiguration() {
        CommunityModerationService.ModerationDecision result = service.moderatePost(post("出售违禁品", "可提供海洛因和手枪"));

        assertThat(result.getDecision()).isEqualTo("BLOCK");
        assertThat(result.getRiskLevel()).isEqualTo("HIGH");
    }

    private CommunityPostCreateDTO post(String title, String content) {
        CommunityPostCreateDTO dto = new CommunityPostCreateDTO();
        dto.setTitle(title);
        dto.setContent(content);
        return dto;
    }
}
