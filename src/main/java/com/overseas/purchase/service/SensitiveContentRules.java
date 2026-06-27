package com.overseas.purchase.service;

import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Built-in bilingual baseline rules. Environment configuration can add terms,
 * but cannot accidentally remove these minimum safety categories.
 */
final class SensitiveContentRules {

    private static final Set<String> BLOCK_WORDS = new LinkedHashSet<>(Arrays.asList(
            "毒品", "海洛因", "冰毒", "甲基苯丙胺", "可卡因", "大麻", "芬太尼", "氯胺酮", "k粉",
            "drugs", "heroin", "meth", "cocaine", "marijuana", "fentanyl", "ketamine",
            "枪支", "手枪", "步枪", "弹药", "炸药", "雷管", "手雷",
            "weapon", "weapons", "gun", "pistol", "rifle", "ammunition", "explosive", "bomb",
            "色情", "淫秽", "裸聊", "成人视频", "儿童色情",
            "porn", "pornography", "explicit sex", "child sexual",
            "赌博", "博彩", "赌场", "赌资", "网络赌博",
            "gambling", "gamble", "casino", "sportsbook"
    ));

    private static final Set<String> REVIEW_WORDS = new LinkedHashSet<>(Arrays.asList(
            "加微信", "加v", "加vx", "微信", "私聊", "私信", "私下交易", "线下交易", "直接转账",
            "绕过平台", "引流", "导流", "拉群", "站外交易",
            "wechat", "telegram", "whatsapp", "private transfer", "off-platform", "direct transfer"
    ));

    private SensitiveContentRules() {
    }

    static Set<String> blockWords(String configured) {
        Set<String> result = parse(configured);
        result.addAll(BLOCK_WORDS);
        return result;
    }

    static Set<String> reviewWords(String configured) {
        Set<String> result = parse(configured);
        result.addAll(REVIEW_WORDS);
        result.removeAll(BLOCK_WORDS);
        return result;
    }

    private static Set<String> parse(String configured) {
        Set<String> result = new LinkedHashSet<>();
        if (!StringUtils.hasText(configured)) {
            return result;
        }
        String[] parts = configured.split("[,;|\\n\\r]");
        for (String part : parts) {
            if (StringUtils.hasText(part)) {
                result.add(part.trim());
            }
        }
        return result;
    }
}
