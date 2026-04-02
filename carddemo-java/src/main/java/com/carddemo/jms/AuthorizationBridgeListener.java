package com.carddemo.jms;

import java.math.BigDecimal;

import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;

import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import com.carddemo.account.Account;
import com.carddemo.account.AccountRepository;

/**
 * COPAUA0C.cbl — simplified authorization bridge: request {@code AUTH,<acctId>,<amount>} replies {@code STATUS=A|D;REASON=...}.
 */
@Component
@Profile("jms")
public class AuthorizationBridgeListener {

    private final JmsTemplate jmsTemplate;
    private final AccountRepository accountRepository;

    public AuthorizationBridgeListener(JmsTemplate jmsTemplate, AccountRepository accountRepository) {
        this.jmsTemplate = jmsTemplate;
        this.accountRepository = accountRepository;
    }

    @JmsListener(
            destination = "${carddemo.jms.auth-bridge-request-queue}",
            containerFactory = "jmsListenerContainerFactory")
    public void receive(TextMessage message) throws JMSException {
        Destination replyTo = message.getJMSReplyTo();
        if (replyTo == null) {
            return;
        }
        String text = message.getText();
        if (text == null || !text.startsWith("AUTH,")) {
            reply(message, replyTo, "STATUS=D;REASON=INVALID_FORMAT");
            return;
        }
        String[] parts = text.split(",", 3);
        if (parts.length < 3) {
            reply(message, replyTo, "STATUS=D;REASON=INVALID_FORMAT");
            return;
        }
        long acctId;
        BigDecimal amt;
        try {
            acctId = Long.parseLong(parts[1].trim());
            amt = new BigDecimal(parts[2].trim());
        } catch (Exception e) {
            reply(message, replyTo, "STATUS=D;REASON=INVALID_FORMAT");
            return;
        }
        Account acc = accountRepository.findById(acctId).orElse(null);
        if (acc == null) {
            reply(message, replyTo, "STATUS=D;REASON=ACCOUNT_NOT_FOUND");
            return;
        }
        if (!"A".equalsIgnoreCase(acc.getActiveStatus())) {
            reply(message, replyTo, "STATUS=D;REASON=ACCOUNT_CLOSED");
            return;
        }
        BigDecimal avail = acc.getCreditLimit().subtract(acc.getCurrentBalance());
        if (avail.compareTo(amt) >= 0) {
            reply(message, replyTo, "STATUS=A;REASON=OK");
        } else {
            reply(message, replyTo, "STATUS=D;REASON=INSUFFICIENT_FUNDS");
        }
    }

    private void reply(TextMessage request, Destination replyTo, String body) {
        jmsTemplate.send(replyTo, s -> {
            TextMessage m = s.createTextMessage(body);
            try {
                m.setJMSCorrelationID(request.getJMSCorrelationID());
            } catch (JMSException ignored) {
                // ignore
            }
            return m;
        });
    }
}
