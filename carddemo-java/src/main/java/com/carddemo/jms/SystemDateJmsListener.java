package com.carddemo.jms;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;

import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * CODATE01.cbl — returns current system date/time on reply queue.
 */
@Component
@Profile("jms")
public class SystemDateJmsListener {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MM-dd-yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final JmsTemplate jmsTemplate;

    public SystemDateJmsListener(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    @JmsListener(
            destination = "${carddemo.jms.date-request-queue}",
            containerFactory = "jmsListenerContainerFactory")
    public void receive(TextMessage message) throws JMSException {
        Destination replyTo = message.getJMSReplyTo();
        if (replyTo == null) {
            return;
        }
        String body = "SYSTEM DATE : " + LocalDate.now().format(DATE)
                + " SYSTEM TIME : " + LocalTime.now().format(TIME);
        jmsTemplate.send(replyTo, s -> {
            TextMessage m = s.createTextMessage(body);
            m.setJMSCorrelationID(message.getJMSCorrelationID());
            return m;
        });
    }
}
