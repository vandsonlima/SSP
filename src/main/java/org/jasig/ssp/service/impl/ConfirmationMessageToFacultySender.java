package org.jasig.ssp.service.impl;

import org.jasig.ssp.model.*;
import org.jasig.ssp.service.MessageService;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.service.PersonService;
import org.jasig.ssp.service.reference.ConfigService;
import org.jasig.ssp.service.reference.MessageTemplateService;
import org.jasig.ssp.web.api.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.mail.SendFailedException;
import java.util.Map;
import java.util.UUID;

/**
 * @since 29/08/2020
 **/
//11
@Component
public class ConfirmationMessageToFacultySender {

    //1
    private ConfigService configService;
    //1
    private PersonService personService;
    //1
    private MessageTemplateService messageTemplateService;
    //1
    private MessageService messageService;

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ConfirmationMessageToFacultySender.class);

    public ConfirmationMessageToFacultySender(ConfigService configService, PersonService personService, MessageTemplateService messageTemplateService, MessageService messageService) {
        this.configService = configService;
        this.personService = personService;
        this.messageTemplateService = messageTemplateService;
        this.messageService = messageService;
    }

    /**
     * Send confirmation e-mail ({@link Message}) to the faculty who created
     * this alert.
     *
     * @param earlyAlert
     *            Early Alert
     * @throws ObjectNotFoundException
     * @throws SendFailedException
     * @throws ValidationException
     */
    //7
    public void send(final EarlyAlert earlyAlert)
            throws ObjectNotFoundException, SendFailedException, ValidationException {
        //1
        //1
        if (earlyAlert == null) {
            throw new IllegalArgumentException("EarlyAlert was missing.");
        }

        //1
        if (earlyAlert.getPerson() == null) {
            throw new IllegalArgumentException("EarlyAlert.Person is missing.");
        }

        //1
        if (configService.getByNameOrDefaultValue("send_faculty_mail") != true) {
            LOGGER.debug("Skipping Faculty Early Alert Confirmation Email: Config Turned Off");
            return; //skip if faculty early alert email turned off
        }

        final UUID personId = earlyAlert.getCreatedBy().getId();
        //1
        Person person = personService.get(personId);
        //1
        if ( person == null ) {
            LOGGER.warn("EarlyAlert {} has no creator. Unable to send"
                    + " confirmation message to faculty.", earlyAlert);
        }
        //1
        else {
            final SubjectAndBody subjAndBody = messageTemplateService
                    .createEarlyAlertFacultyConfirmationMessage(fillTemplateParameters(earlyAlert));

            // Create and queue the message
            final Message message = messageService.createMessage(person, null,
                    subjAndBody);

            LOGGER.info("Message {} created for EarlyAlert {}", message, earlyAlert);
        }
    }

    private Map<String, Object> fillTemplateParameters(EarlyAlert earlyAlert) {
        //TODO:abstrair o fillTemplateParameters para um service
        return null;
    }
}
