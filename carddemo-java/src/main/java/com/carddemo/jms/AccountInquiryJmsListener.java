package com.carddemo.jms;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Optional;

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
 * COACCT01.cbl — request: 4-char function (INQA) + 11-digit account id; reply is textual account summary.
 */
@Component
@Profile("jms")
public class AccountInquiryJmsListener {

    private static final DecimalFormat MONEY = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.US));

    private final JmsTemplate jmsTemplate;
    private final AccountRepository accountRepository;

    public AccountInquiryJmsListener(JmsTemplate jmsTemplate, AccountRepository accountRepository) {
        this.jmsTemplate = jmsTemplate;
        this.accountRepository = accountRepository;
    }

    @JmsListener(
            destination = "${carddemo.jms.account-request-queue}",
            containerFactory = "jmsListenerContainerFactory")
    public void receive(TextMessage message) throws JMSException {
        Destination replyTo = message.getJMSReplyTo();
        if (replyTo == null) {
            return;
        }
        String body = message.getText();
        if (body == null || body.length() < 15) {
            jmsTemplate.send(replyTo, s -> s.createTextMessage("INVALID REQUEST PARAMETERS"));
            return;
        }
        String func = body.substring(0, 4).trim();
        String acctPart = body.substring(4, 15).trim();
        if (!"INQA".equalsIgnoreCase(func)) {
            jmsTemplate.send(replyTo, s -> s.createTextMessage("INVALID REQUEST PARAMETERS"));
            return;
        }
        long acctId;
        try {
            acctId = Long.parseLong(acctPart.replaceFirst("^0+(?!$)", ""));
        } catch (NumberFormatException e) {
            jmsTemplate.send(replyTo, s -> s.createTextMessage("INVALID REQUEST PARAMETERS ACCT ID : " + acctPart));
            return;
        }
        Optional<Account> acc = accountRepository.findById(acctId);
        if (acc.isEmpty()) {
            jmsTemplate.send(replyTo, s -> s.createTextMessage("ERROR WHILE READING ACCTFILE"));
            return;
        }
        Account a = acc.get();
        String reply = buildResponse(a);
        jmsTemplate.send(replyTo, s -> {
            TextMessage m = s.createTextMessage(reply);
            m.setJMSCorrelationID(message.getJMSCorrelationID());
            return m;
        });
    }

    private static String buildResponse(Account a) {
        StringBuilder sb = new StringBuilder(300);
        sb.append("ACCOUNT ID : ").append(String.format("%011d", a.getAccountId()));
        sb.append("ACCOUNT STATUS : ").append(nvl(a.getActiveStatus()));
        sb.append("BALANCE : ").append(fmt(a.getCurrentBalance()));
        sb.append("CREDIT LIMIT : ").append(fmt(a.getCreditLimit()));
        sb.append("CASH LIMIT : ").append(fmt(a.getCashCreditLimit()));
        sb.append("OPEN DATE : ").append(nvl(a.getOpenDate()));
        sb.append("EXPR DATE : ").append(nvl(a.getExpirationDate()));
        sb.append("REIS DATE : ").append(nvl(a.getReissueDate()));
        sb.append("CREDIT BAL : ").append(fmt(a.getCurrentCycleCredit()));
        sb.append("DEBIT BAL : ").append(fmt(a.getCurrentCycleDebit()));
        sb.append("GROUP ID : ").append(nvl(a.getGroupId()));
        return sb.toString();
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static String fmt(BigDecimal v) {
        if (v == null) {
            return "0.00";
        }
        return MONEY.format(v);
    }
}
