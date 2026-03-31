package com.overseas.purchase.service;

import com.overseas.purchase.dto.RegisterRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LegalService {

    public static final String TERMS_VERSION = "v1.0";
    public static final String PRIVACY_VERSION = "v1.0";

    public Map<String, Object> getCurrentLegal() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("termsVersion", TERMS_VERSION);
        result.put("privacyVersion", PRIVACY_VERSION);

        Map<String, Object> clauses = new LinkedHashMap<>();
        clauses.put("terms", buildTermsClause());
        clauses.put("privacy", buildPrivacyClause());
        result.put("clauses", clauses);

        result.put("officialReferences", buildOfficialReferences());
        return result;
    }

    public void validateRegisterConsent(RegisterRequest request) {
        if (!Boolean.TRUE.equals(request.getAgreeTerms()) || !Boolean.TRUE.equals(request.getAgreePrivacy())) {
            throw new RuntimeException("Please read and agree to both Terms and Privacy Policy");
        }
        if (!TERMS_VERSION.equals(request.getTermsVersion()) || !PRIVACY_VERSION.equals(request.getPrivacyVersion())) {
            throw new RuntimeException("Legal version mismatch, please refresh and confirm again");
        }
    }

    private Map<String, Object> buildTermsClause() {
        Map<String, Object> terms = new LinkedHashMap<>();
        terms.put("title", "用户协议与跨境责任边界");
        terms.put("sections", Arrays.asList(
                "本平台为跨境代购信息与交易撮合服务，跨境税费、清关、物流时效与国内电商存在差异。",
                "用户下单前应充分阅读商品页面的税费说明、清关说明、售后限制与时效提示。",
                "商家对商品来源真实性、合规性、质量与售后承诺负责；平台依法履行审核、处理投诉与纠纷协助义务。",
                "涉及禁限售、海关政策限制或跨境清关失败导致的履约异常，按页面告知规则与法律规定处理。",
                "平台不会通过格式条款免除依法应承担的责任，亦不会排除消费者依法享有的核心权利。"
        ));
        return terms;
    }

    private Map<String, Object> buildPrivacyClause() {
        Map<String, Object> privacy = new LinkedHashMap<>();
        privacy.put("title", "隐私政策摘要");
        privacy.put("sections", Arrays.asList(
                "为完成注册、下单、支付、物流与售后服务，平台将处理账号信息、联系方式、收货信息与交易记录。",
                "平台仅在必要范围内处理个人信息，并采取访问控制、日志审计等措施保护信息安全。",
                "在跨境履约场景中，依法可能向物流、清关、支付等合作方提供必要信息。",
                "用户可依法行使查询、更正、删除与注销等权利；如对隐私处理有疑问，可通过平台客服渠道联系。",
                "本政策更新时将通过版本号与页面公示，重大变更会提示用户重新确认。"
        ));
        return privacy;
    }

    private List<Map<String, Object>> buildOfficialReferences() {
        return Arrays.asList(
                ref("国家法律法规数据库", "https://flk.npc.gov.cn/", "全国人大", "官方法律数据库入口"),
                ref("《中华人民共和国电子商务法》", "https://www.npc.gov.cn/zgrdw/npc/xinwen/2018-08/31/content_2060172.htm", "全国人大", "2018-08-31"),
                ref("《中华人民共和国个人信息保护法》", "https://www.cac.gov.cn/2021-08/20/c_1631050028355286.htm", "中央网信办", "2021-08-20"),
                ref("《中华人民共和国消费者权益保护法》", "https://www.gov.cn/zhengce/2013-10/25/content_2602290.htm", "中国政府网", "2013-10-25"),
                ref("《关于完善跨境电子商务零售进口监管有关工作的通知》", "https://www.gov.cn/zhengce/zhengceku/2018-12/31/content_5437823.htm", "中国政府网", "2018-11-28")
        );
    }

    private Map<String, Object> ref(String title, String url, String agency, String publishDate) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", title);
        map.put("url", url);
        map.put("agency", agency);
        map.put("publishDate", publishDate);
        map.put("domain", extractDomain(url));
        return map;
    }

    private String extractDomain(String url) {
        if (!StringUtils.hasText(url)) {
            return "";
        }
        String normalized = url.replace("https://", "").replace("http://", "");
        int slashIndex = normalized.indexOf('/');
        return slashIndex > -1 ? normalized.substring(0, slashIndex) : normalized;
    }
}
