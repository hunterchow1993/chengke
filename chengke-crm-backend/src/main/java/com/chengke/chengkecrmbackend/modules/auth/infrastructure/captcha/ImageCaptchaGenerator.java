package com.chengke.chengkecrmbackend.modules.auth.infrastructure.captcha;

import com.chengke.chengkecrmbackend.modules.auth.application.port.CaptchaGenerator;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 生成 4 位不区分大小写的 PNG 验证码。
 */
@Component
public class ImageCaptchaGenerator implements CaptchaGenerator {
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    @Override
    public GeneratedCaptcha generate() {
        StringBuilder code = new StringBuilder(4);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 4; i++) {
            code.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return new GeneratedCaptcha(UUID.randomUUID().toString(), code.toString(), render(code.toString()));
    }

    private String render(String code) {
        BufferedImage image = new BufferedImage(120, 40, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(248, 250, 252));
        graphics.fillRect(0, 0, 120, 40);
        graphics.setColor(new Color(15, 108, 189));
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        graphics.drawString(code, 18, 28);
        graphics.dispose();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("无法生成验证码图片", exception);
        }
    }
}
