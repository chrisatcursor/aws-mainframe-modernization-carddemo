package com.carddemo.integration.jms;

import com.carddemo.account.Account;
import com.carddemo.account.AccountRepository;
import com.carddemo.authorization.PendingAuthPurgeSupport;
import com.carddemo.authorization.PendingAuthorizationService;
import jakarta.jms.BytesMessage;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * CODATE01 / COACCT01 / COPAUA0C — JMS adapters (IBM MQ → Spring JMS + embedded Artemis).
 */
@Component
@ConditionalOnProperty(name = "carddemo.mq.enabled", havingValue = "true")
public class CardDemoJmsListeners {

    private final AccountRepository accountRepository;
    private final PendingAuthorizationService pendingAuthorizationService;
    private final JmsTemplate jmsTemplate;

    @Value("${carddemo.mq.date-reply-queue}")
    private String dateReplyQueue;

    @Value("${carddemo.mq.acct-reply-queue}")
    private String acctReplyQueue;

    @Value("${carddemo.mq.auth-reply-queue}")
    private String authReplyQueue;

    @Value("${carddemo.mq.error-queue}")
    private String errorQueue;

    public CardDemoJmsListeners(
            AccountRepository accountRepository,
            PendingAuthorizationService pendingAuthorizationService,
            JmsTemplate jmsTemplate) {
        this.accountRepository = accountRepository;
        this.pendingAuthorizationService = pendingAuthorizationService;
        this.jmsTemplate = jmsTemplate;
    }

    @JmsListener(destination = "${carddemo.mq.date-request-queue:carddemo.request.date}")
    public void onDateRequest(TextMessage message, Session session) throws JMSException {
        try {
            String body = "DATE_OK " + Instant.now().toString();
            sendReply(session, message, dateReplyQueue, body);
        } catch (Exception e) {
            sendError(session, message, e.getMessage());
        }
    }

    @JmsListener(destination = "${carddemo.mq.acct-request-queue:carddemo.request.acct}")
    public void onAcctRequest(TextMessage message, Session session) throws JMSException {
        try {
            String text = message.getText() != null ? message.getText().trim() : "";
            long acctId = parseAcctId(text);
            String body;
            if (acctId <= 0) {
                body = "ERROR|INVALID_ACCOUNT_KEY";
            } else {
                body = accountRepository.findById(acctId)
                        .map(this::formatAccount)
                        .orElse("ERROR|NOT_FOUND");
            }
            sendReply(session, message, acctReplyQueue, body);
        } catch (Exception e) {
            sendError(session, message, e.getMessage());
        }
    }

    @JmsListener(destination = "${carddemo.mq.auth-request-queue}")
    public void onAuthRequest(Message message, Session session) throws JMSException {
        try {
            String text = extractText(message);
            String[] p = text.split("\\|");
            if (p.length < 3 || !"AUTH".equalsIgnoreCase(p[0].trim())) {
                sendReply(session, message, authReplyQueue, "ERROR|EXPECTED_AUTH_PIPE_FORMAT");
                return;
            }
            String card = p[1].trim();
            BigDecimal amt = PendingAuthPurgeSupport.parseAuthRequestAmount(p[2]);
            String mcc = p.length > 3 ? p[3].trim() : "0000";
            String mid = p.length > 4 ? p[4].trim() : "MQ";
            String result = pendingAuthorizationService.processAuthorizationRequest(card, amt, mcc, mid);
            sendReply(session, message, authReplyQueue, result);
        } catch (Exception e) {
            sendError(session, message, e.getMessage());
        }
    }

    private String formatAccount(Account a) {
        return String.format(
                "ACCT_ID:%d|STATUS:%s|BALANCE:%s|CREDIT_LIMIT:%s|CASH_LIMIT:%s",
                a.getAcctId(),
                a.getActiveStatus(),
                a.getCurrentBalance() != null ? a.getCurrentBalance().toPlainString() : "0",
                a.getCreditLimit() != null ? a.getCreditLimit().toPlainString() : "0",
                a.getCashCreditLimit() != null ? a.getCashCreditLimit().toPlainString() : "0");
    }

    private static long parseAcctId(String text) {
        if (text == null || text.isBlank()) {
            return -1;
        }
        String digits = text.replaceAll("\\D", "");
        if (digits.length() >= 11) {
            digits = digits.substring(digits.length() - 11);
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String extractText(Message message) throws JMSException {
        if (message instanceof TextMessage tm) {
            return tm.getText() != null ? tm.getText() : "";
        }
        if (message instanceof BytesMessage bm) {
            long len = bm.getBodyLength();
            if (len <= 0 || len > 10_000) {
                return "";
            }
            byte[] buf = new byte[(int) len];
            bm.readBytes(buf);
            return new String(buf).trim();
        }
        return "";
    }

    private void sendReply(Session session, Message request, String queueName, String body) throws JMSException {
        Destination replyTo = request.getJMSReplyTo();
        String correlationId = request.getJMSCorrelationID();
        if (replyTo != null) {
            TextMessage reply = session.createTextMessage(body);
            if (correlationId != null) {
                reply.setJMSCorrelationID(correlationId);
            }
            try (var producer = session.createProducer(replyTo)) {
                producer.send(reply);
            }
        } else {
            jmsTemplate.send(queueName, s -> {
                TextMessage m = s.createTextMessage(body);
                if (correlationId != null) {
                    m.setJMSCorrelationID(correlationId);
                }
                return m;
            });
        }
    }

    private void sendError(Session session, Message request, String err) throws JMSException {
        String correlationId = request.getJMSCorrelationID();
        jmsTemplate.send(errorQueue, s -> {
            TextMessage em = s.createTextMessage("ERROR|" + err);
            if (correlationId != null) {
                em.setJMSCorrelationID(correlationId);
            }
            return em;
        });
    }
}
