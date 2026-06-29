package com.wallet.notification_service.service;

import com.wallet.notification_service.event.WalletEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${notification.email.from}")
    private String fromAddress;

    @Value("${notification.email.from-name}")
    private String fromName;

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    // ==================== PUBLIC SEND METHODS ====================

    @Async
    public void sendMoneyAddedEmail(String toEmail, WalletEvent event) {
        String subject = "✅ Money Added to Your VaultX Wallet";
        String body = buildMoneyAddedHtml(event);
        sendHtmlEmail(toEmail, subject, body);
    }

    @Async
    public void sendMoneyWithdrawnEmail(String toEmail, WalletEvent event) {
        String subject = "💸 Withdrawal Successful – VaultX Wallet";
        String body = buildMoneyWithdrawnHtml(event);
        sendHtmlEmail(toEmail, subject, body);
    }

    @Async
    public void sendTransferDebitEmail(String toEmail, WalletEvent event) {
        String subject = "🔄 Money Transferred – VaultX Wallet";
        String body = buildTransferDebitHtml(event);
        sendHtmlEmail(toEmail, subject, body);
    }

    @Async
    public void sendTransferCreditEmail(String toEmail, WalletEvent event) {
        String subject = "🎉 Money Received – VaultX Wallet";
        String body = buildTransferCreditHtml(event);
        sendHtmlEmail(toEmail, subject, body);
    }

    @Async
    public void sendWalletFrozenEmail(String toEmail, WalletEvent event) {
        String subject = "🔒 Your VaultX Wallet Has Been Frozen";
        String body = buildWalletFrozenHtml(event);
        sendHtmlEmail(toEmail, subject, body);
    }

    @Async
    public void sendWalletSuspendedEmail(String toEmail, WalletEvent event) {
        String subject = "⚠️ Your VaultX Wallet Has Been Suspended";
        String body = buildWalletSuspendedHtml(event);
        sendHtmlEmail(toEmail, subject, body);
    }

    @Async
    public void sendDailyLimitAlertEmail(String toEmail, WalletEvent event) {
        String subject = "🚨 Daily Spend Limit Alert – VaultX Wallet";
        String body = buildDailyLimitAlertHtml(event);
        sendHtmlEmail(toEmail, subject, body);
    }

    @Async
    public void sendLowBalanceAlertEmail(String toEmail, WalletEvent event) {
        String subject = "⚡ Low Balance Alert – VaultX Wallet";
        String body = buildLowBalanceAlertHtml(event);
        sendHtmlEmail(toEmail, subject, body);
    }

    @Async
    public void sendTransactionFailedEmail(String toEmail, WalletEvent event) {
        String subject = "❌ Transaction Failed – VaultX Wallet";
        String body = buildTransactionFailedHtml(event);
        sendHtmlEmail(toEmail, subject, body);
    }

    // ==================== CORE SEND LOGIC ====================

    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        if (!emailEnabled) {
            log.info("Email disabled in config — skipping email to {}", to);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent successfully to {} | subject: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {} | subject: {} | error: {}", to, subject, e.getMessage());
            throw new RuntimeException("Email sending failed: " + e.getMessage(), e);
        }
    }

    // ==================== HTML TEMPLATE BUILDERS ====================

    private String buildMoneyAddedHtml(WalletEvent event) {
        return baseTemplate(
                "Money Added Successfully",
                "Your VaultX wallet has been credited.",
                tableRow("Amount", formatAmount(event.getAmount(), event.getCurrency())) +
                tableRow("Balance After", formatAmount(event.getBalanceAfter(), event.getCurrency())) +
                tableRow("Transaction ID", event.getTransactionId()) +
                tableRow("Date & Time", formatDateTime(event.getOccurredAt())),
                "green"
        );
    }

    private String buildMoneyWithdrawnHtml(WalletEvent event) {
        return baseTemplate(
                "Withdrawal Successful",
                "Money has been debited from your VaultX wallet.",
                tableRow("Amount Withdrawn", formatAmount(event.getAmount(), event.getCurrency())) +
                tableRow("Balance After", formatAmount(event.getBalanceAfter(), event.getCurrency())) +
                tableRow("Transaction ID", event.getTransactionId()) +
                tableRow("Date & Time", formatDateTime(event.getOccurredAt())),
                "orange"
        );
    }

    private String buildTransferDebitHtml(WalletEvent event) {
        return baseTemplate(
                "Money Transfer Successful",
                "You have successfully transferred money via VaultX.",
                tableRow("Amount Sent", formatAmount(event.getAmount(), event.getCurrency())) +
                        tableRow("Sent To", event.getRecipientName() != null ? event.getRecipientName() : event.getRecipientUserId()) +
                        tableRow("Balance After", formatAmount(event.getBalanceAfter(), event.getCurrency())) +
                        tableRow("Transfer ID", event.getTransferId()) +
                        tableRow("Date & Time", formatDateTime(event.getOccurredAt())),
                "blue"
        );
    }

    private String buildTransferCreditHtml(WalletEvent event) {
        return baseTemplate(
                "Money Received",
                "You have received money in your VaultX wallet.",
                tableRow("Amount Received", formatAmount(event.getAmount(), event.getCurrency())) +
                        tableRow("Received From", event.getSenderName() != null ? event.getSenderName() : event.getSenderUserId()) +
                        tableRow("Balance After", formatAmount(event.getBalanceAfter(), event.getCurrency())) +
                        tableRow("Transfer ID", event.getTransferId()) +
                        tableRow("Date & Time", formatDateTime(event.getOccurredAt())),
                "green"
        );
    }

    private String buildWalletFrozenHtml(WalletEvent event) {
        return baseTemplate(
                "Wallet Frozen",
                "Your VaultX wallet has been frozen. Transactions are temporarily suspended.",
                tableRow("Wallet ID", event.getWalletId()) +
                tableRow("Reason", event.getReason() != null ? event.getReason() : "N/A") +
                tableRow("Date & Time", formatDateTime(event.getOccurredAt())) +
                "<p style='margin-top:16px;color:#555;'>Please contact support at support@vaultx.com if you believe this is a mistake.</p>",
                "red"
        );
    }

    private String buildWalletSuspendedHtml(WalletEvent event) {
        return baseTemplate(
                "Wallet Suspended",
                "Your VaultX wallet has been suspended.",
                tableRow("Wallet ID", event.getWalletId()) +
                tableRow("Reason", event.getReason() != null ? event.getReason() : "N/A") +
                tableRow("Date & Time", formatDateTime(event.getOccurredAt())) +
                "<p style='margin-top:16px;color:#555;'>Contact support at support@vaultx.com for assistance.</p>",
                "red"
        );
    }

    private String buildDailyLimitAlertHtml(WalletEvent event) {
        BigDecimal remaining = event.getDailyLimit() != null && event.getDailySpent() != null
                ? event.getDailyLimit().subtract(event.getDailySpent())
                : BigDecimal.ZERO;
        return baseTemplate(
                "Daily Spend Limit Alert",
                "You have used a significant portion of your daily spend limit.",
                tableRow("Daily Limit", formatAmount(event.getDailyLimit(), event.getCurrency())) +
                tableRow("Amount Spent Today", formatAmount(event.getDailySpent(), event.getCurrency())) +
                tableRow("Remaining", formatAmount(remaining, event.getCurrency())) +
                tableRow("Date & Time", formatDateTime(event.getOccurredAt())),
                "orange"
        );
    }

    private String buildLowBalanceAlertHtml(WalletEvent event) {
        return baseTemplate(
                "Low Balance Alert",
                "Your VaultX wallet balance is running low.",
                tableRow("Current Balance", formatAmount(event.getBalanceAfter(), event.getCurrency())) +
                tableRow("Wallet ID", event.getWalletId()) +
                tableRow("Date & Time", formatDateTime(event.getOccurredAt())),
                "orange"
        );
    }

    private String buildTransactionFailedHtml(WalletEvent event) {
        return baseTemplate(
                "Transaction Failed",
                "A transaction on your VaultX wallet could not be completed.",
                tableRow("Amount", formatAmount(event.getAmount(), event.getCurrency())) +
                tableRow("Transaction ID", event.getTransactionId()) +
                tableRow("Reason", event.getReason() != null ? event.getReason() : "Unknown error") +
                tableRow("Date & Time", formatDateTime(event.getOccurredAt())),
                "red"
        );
    }

    // ==================== TEMPLATE HELPERS ====================

    private String baseTemplate(String title, String subtitle, String content, String accentColor) {
        String colorMap = switch (accentColor) {
            case "green"  -> "#22c55e";
            case "red"    -> "#ef4444";
            case "orange" -> "#f97316";
            case "blue"   -> "#3b82f6";
            default       -> "#6366f1";
        };
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"/></head>
                <body style="margin:0;padding:0;background:#f4f4f5;font-family:Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f4f5;padding:32px 0;">
                    <tr><td align="center">
                      <table width="600" cellpadding="0" cellspacing="0"
                             style="background:#ffffff;border-radius:12px;overflow:hidden;
                                    box-shadow:0 2px 8px rgba(0,0,0,0.08);">
                        <!-- header -->
                        <tr>
                          <td style="background:%s;padding:28px 32px;">
                            <h1 style="margin:0;color:#ffffff;font-size:22px;">VaultX Wallet</h1>
                            <p  style="margin:6px 0 0;color:rgba(255,255,255,0.85);font-size:14px;">%s</p>
                          </td>
                        </tr>
                        <!-- body -->
                        <tr>
                          <td style="padding:28px 32px;">
                            <p style="margin:0 0 20px;color:#374151;font-size:15px;">%s</p>
                            <table width="100%%" cellpadding="8" cellspacing="0"
                                   style="border-collapse:collapse;background:#f9fafb;
                                          border-radius:8px;overflow:hidden;">
                              %s
                            </table>
                          </td>
                        </tr>
                        <!-- footer -->
                        <tr>
                          <td style="background:#f9fafb;padding:20px 32px;border-top:1px solid #e5e7eb;">
                            <p style="margin:0;color:#9ca3af;font-size:12px;">
                              This is an automated message from VaultX. Do not reply.<br/>
                              © 2024 VaultX Wallet. All rights reserved.
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(colorMap, title, subtitle, content);
    }

    private String tableRow(String label, String value) {
        return """
                <tr style="border-bottom:1px solid #e5e7eb;">
                  <td style="color:#6b7280;font-size:13px;padding:10px 12px;width:40%%;">%s</td>
                  <td style="color:#111827;font-size:13px;font-weight:600;padding:10px 12px;">%s</td>
                </tr>
                """.formatted(label, value != null ? value : "—");
    }

    private String formatAmount(BigDecimal amount, String currency) {
        if (amount == null) return "—";
        String cur = (currency != null) ? currency : "INR";
        return cur + " " + String.format("%,.2f", amount);
    }

    private String formatDateTime(java.time.LocalDateTime dt) {
        if (dt == null) return "—";
        return dt.toString().replace("T", " ").substring(0, 19);
    }
}
