package org.jasig.ssp.service.impl;

import org.jasig.ssp.model.EarlyAlert;
import org.jasig.ssp.model.Message;
import org.jasig.ssp.model.Person;
import org.jasig.ssp.model.SubjectAndBody;
import org.jasig.ssp.service.MessageService;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.service.PersonService;
import org.jasig.ssp.service.reference.ConfigService;
import org.jasig.ssp.service.reference.MessageTemplateService;
import org.jasig.ssp.web.api.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
    //1
    private FilltemplateParametersProcessor fillTemplateParametersProcessor;

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ConfirmationMessageToFacultySender.class);

    public ConfirmationMessageToFacultySender(ConfigService configService, PersonService personService, MessageTemplateService messageTemplateService, MessageService messageService, FilltemplateParametersProcessor fillTemplateParametersProcessor) {
        this.configService = configService;
        this.personService = personService;
        this.messageTemplateService = messageTemplateService;
        this.messageService = messageService;
        this.fillTemplateParametersProcessor = fillTemplateParametersProcessor;
    }

    /**
     * Send confirmation e-mail ({@link Message}) to the faculty who created
     * this alert.
     *
     * @param earlyAlert
     *            Early Alert
     * @throws ObjectNotFoundException
     * @throws ValidationException
     */
    //6
    public void send(final EarlyAlert earlyAlert)
            throws ObjectNotFoundException, ValidationException {
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
                    .createEarlyAlertFacultyConfirmationMessage(fillTemplateParametersProcessor.process(earlyAlert));

            // Create and queue the message
            final Message message = messageService.createMessage(person, null,
                    subjAndBody);

            LOGGER.info("Message {} created for EarlyAlert {}", message, earlyAlert);
        }
    }

}
