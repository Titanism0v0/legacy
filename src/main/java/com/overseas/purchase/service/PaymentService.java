package com.overseas.purchase.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.overseas.purchase.entity.Order;
import com.overseas.purchase.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付服务类（个人收款码方案）
 * 
 * @author System
 */
@Service
@RequiredArgsConstructor
public class PaymentService {
    
    private final OrderMapper orderMapper;
    
    @Value("${payment.qrcode.receiver-name:收款人}")
    private String receiverName;
    
    @Value("${payment.qrcode.receiver-wechat:}")
    private String receiverWechat;
    
    @Value("${payment.qrcode.payment-domain:http://localhost:8081}")
    private String paymentDomain;
    
    @Value("${payment.qrcode.qrcode-image-path:}")
    private String qrcodeImagePath;
    
    /**
     * 生成支付二维码
     * 生成包含订单信息的收款码
     */
    public Map<String, Object> generatePaymentQRCode(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new RuntimeException("订单不存在");
        }
        
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不正确");
        }
        
        // 生成收款信息文本（用于显示）
        String paymentInfo = String.format(
            "订单号：%s\n金额：¥%.2f\n请转账后点击'我已支付'按钮",
            order.getOrderNo(),
            order.getTotalPrice().doubleValue()
        );
        
        // 优先加载个人收款码图片
        String receiverQRCodeImage = null;
        String qrCodeImage = null;
        String paymentUrl = null;
        
        if (qrcodeImagePath != null && !qrcodeImagePath.isEmpty()) {
            try {
                String resourcePath = qrcodeImagePath.replace("classpath:", "");
                System.out.println("尝试加载收款码图片，路径: " + resourcePath);
                
                // 尝试从classpath加载收款码图片
                java.io.InputStream is = this.getClass().getClassLoader().getResourceAsStream(resourcePath);
                if (is != null) {
                    BufferedImage receiverImage = ImageIO.read(is);
                    if (receiverImage != null) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        // 尝试PNG格式，如果失败则尝试JPG
                        String format = "PNG";
                        if (resourcePath.toLowerCase().endsWith(".jpg") || resourcePath.toLowerCase().endsWith(".jpeg")) {
                            format = "JPG";
                        }
                        ImageIO.write(receiverImage, format, baos);
                        byte[] imageBytes = baos.toByteArray();
                        String mimeType = format.equals("JPG") ? "image/jpeg" : "image/png";
                        receiverQRCodeImage = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(imageBytes);
                        is.close();
                        System.out.println("成功加载个人收款码图片，大小: " + imageBytes.length + " 字节");
                    } else {
                        System.out.println("收款码图片读取失败：图片为null");
                    }
                } else {
                    System.out.println("收款码图片文件不存在，路径: " + resourcePath);
                    System.out.println("请确保图片文件位于: src/main/resources/" + resourcePath);
                }
            } catch (Exception e) {
                System.out.println("加载收款码图片失败: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("未配置收款码图片路径");
        }
        
        // 如果没有配置收款码图片，生成支付链接二维码（仅用于开发测试）
        if (receiverQRCodeImage == null) {
            paymentUrl = String.format("%s/payment?orderId=%d&orderNo=%s&amount=%.2f",
                paymentDomain,
                orderId,
                order.getOrderNo(),
                order.getTotalPrice().doubleValue()
            );
            qrCodeImage = generateQRCodeImage(paymentUrl);
            System.out.println("未配置收款码图片，使用支付链接二维码");
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("amount", order.getTotalPrice());
        result.put("qrCodeImage", qrCodeImage); // 支付链接二维码（如果没有收款码图片）
        result.put("receiverQRCodeImage", receiverQRCodeImage); // 个人收款码图片（优先使用）
        result.put("paymentInfo", paymentInfo);
        result.put("receiverName", receiverName);
        result.put("receiverWechat", receiverWechat);
        result.put("paymentUrl", paymentUrl); // 支付页面链接（如果没有收款码图片）
        
        return result;
    }
    
    /**
     * 生成二维码图片（Base64编码）
     */
    private String generateQRCodeImage(String content) {
        try {
            int width = 300;
            int height = 300;
            
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 1);
            
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
            
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.setColor(Color.BLACK);
            
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    if (bitMatrix.get(x, y)) {
                        graphics.fillRect(x, y, 1, 1);
                    }
                }
            }
            
            graphics.dispose();
            
            // 转换为Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
            
        } catch (WriterException | IOException e) {
            throw new RuntimeException("生成二维码失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 确认支付（用户点击"我已支付"后调用）
     * @param orderId 订单ID
     * @param paymentProof 支付凭证（转账截图URL，可选）
     */
    public void confirmPayment(Long orderId, String paymentProof) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new RuntimeException("订单不存在");
        }
        
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不正确");
        }
        
        // 更新订单状态为待发货
        order.setStatus("PENDING_SHIPMENT");
        try {
            // 尝试设置支付凭证和支付时间（如果字段存在）
            if (paymentProof != null && !paymentProof.isEmpty()) {
                order.setPaymentProof(paymentProof);
            }
            order.setPaymentTime(java.time.LocalDateTime.now());
        } catch (Exception e) {
            // 如果字段不存在，忽略（兼容旧数据库）
            System.out.println("支付凭证字段不存在，跳过设置: " + e.getMessage());
        }
        orderMapper.updateById(order);
    }
}
