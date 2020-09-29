package org.jasig.ssp.service.impl;

import org.jasig.ssp.model.EarlyAlert;
import org.jasig.ssp.model.Message;
import org.jasig.ssp.model.Person;
import org.jasig.ssp.model.SubjectAndBody;
import org.jasig.ssp.service.MessageService;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.service.reference.MessageTemplateService;
import org.jasig.ssp.web.api.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @since 29/08/2020
 **/
//11
@Component
public class MessageToStudentSender {

    //1
    private MessageTemplateService messageTemplateService;
    //1
    private MessageService messageService;
    //1
    private FilltemplateParametersProcessor fillTemplateParametersProcessor;

    private static final Logger LOGGER = LoggerFactory
            .getLogger(MessageToStudentSender.class);

    public MessageToStudentSender(MessageTemplateService messageTemplateService, MessageService messageService, FilltemplateParametersProcessor fillTemplateParametersProcessor) {
        this.messageTemplateService = messageTemplateService;
        this.messageService = messageService;
        this.fillTemplateParametersProcessor = fillTemplateParametersProcessor;
    }

    //8
    public void send(@NotNull EarlyAlert earlyAlert) throws ValidationException, ObjectNotFoundException {
        //1
        if (earlyAlert == null) {
            throw new IllegalArgumentException("EarlyAlert was missing.");
        }

        //1
        if (earlyAlert.getPerson() == null) {
            throw new IllegalArgumentException("EarlyAlert.Person is missing.");
        }

        final Person person = earlyAlert.getPerson();
        final SubjectAndBody subjAndBody = messageTemplateService
                .createEarlyAlertToStudentMessage(fillTemplateParametersProcessor.process(earlyAlert));

        Set<String> watcheremails = new HashSet<String>(person.getWatcherEmailAddresses());
        // Create and queue the message
        final Message message = messageService.createMessage(person, org.springframework.util.StringUtils.arrayToCommaDelimitedString(watcheremails
                        .toArray(new String[watcheremails.size()])),
                subjAndBody);

        LOGGER.info("Message {} created for EarlyAlert {}", message, earlyAlert);
    }

}
