package com.example.demo.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public void sendVerificationCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (fromEmail != null && !fromEmail.isBlank()) {
            message.setFrom(fromEmail);
        }
        message.setTo(toEmail);
        message.setSubject("小集日帳號驗證碼");
        message.setText("""
                您好，

                您的小集日帳號驗證碼為：%s

                驗證碼將於 10 分鐘後失效。若您沒有註冊小集日帳號，請忽略此信。
                """.formatted(code));

        mailSender.send(message);
    }

    public void sendPasswordResetCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (fromEmail != null && !fromEmail.isBlank()) {
            message.setFrom(fromEmail);
        }
        message.setTo(toEmail);
        message.setSubject("小集日重設密碼驗證碼");
        message.setText("""
                您好，

                您的小集日重設密碼驗證碼為：%s

                驗證碼將於 10 分鐘後失效。若您沒有申請重設小集日帳號密碼，請忽略此信。
                """.formatted(code));

        mailSender.send(message);
    }
}
