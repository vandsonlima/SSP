package org.jasig.ssp.service.impl;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.jasig.ssp.model.*;
import org.jasig.ssp.service.EarlyAlertRoutingService;
import org.jasig.ssp.service.MessageService;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.service.reference.MessageTemplateService;
import org.jasig.ssp.util.sort.PagingWrapper;
import org.jasig.ssp.util.sort.SortingAndPaging;
import org.jasig.ssp.web.api.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.mail.SendFailedException;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @since 29/08/2020
 **/
//17
@Component
public class MessageToAdvisorSender {

    //1
    private MessageTemplateService messageTemplateService;
    //1
    private MessageService messageService;
    //1
    private EarlyAlertRoutingService earlyAlertRoutingService;
    //1
    private FilltemplateParametersProcessor fillTemplateParametersProcessor;

    private static final Logger LOGGER = LoggerFactory
            .getLogger(MessageToAdvisorSender.class);

    public MessageToAdvisorSender(MessageTemplateService messageTemplateService, MessageService messageService, EarlyAlertRoutingService earlyAlertRoutingService, FilltemplateParametersProcessor fillTemplateParametersProcessor) {
        this.messageTemplateService = messageTemplateService;
        this.messageService = messageService;
        this.earlyAlertRoutingService = earlyAlertRoutingService;
        this.fillTemplateParametersProcessor = fillTemplateParametersProcessor;
    }

    /**
     * Send e-mail ({@link Message}) to the assigned advisor for the student.
     *
     * @param earlyAlert
     *            Early Alert
     * @throws ObjectNotFoundException
     * @throws ValidationException
     */
    //13
    public void send(@NotNull final EarlyAlert earlyAlert) throws ObjectNotFoundException,
            ValidationException {
        //1
        if (earlyAlert == null) {
            throw new IllegalArgumentException("Early alert was missing.");
        }
        //1
        if (earlyAlert.getPerson() == null) {
            throw new IllegalArgumentException("EarlyAlert Person is missing.");
        }

        final String emailCC = earlyAlert.getEmailCC();
        final Person person = earlyAlert.getPerson().getCoach();
        final SubjectAndBody subjAndBody = messageTemplateService
                .createEarlyAlertAdvisorConfirmationMessage(fillTemplateParametersProcessor.process(earlyAlert));

        Set<String> watcherEmailAddresses = new HashSet<String>(earlyAlert.getPerson().getWatcherEmailAddresses());
        //1
        if(emailCC != null && !emailCC.isEmpty())
        {
            watcherEmailAddresses.add(emailCC);
        }
        //1
        if ( person == null ) {
            LOGGER.warn("Student {} had no coach when EarlyAlert {} was"
                            + " created. Unable to send message to coach.",
                    earlyAlert.getPerson(), earlyAlert);
        }
        //1
        else {
            // Create and queue the message
            final Message message = messageService.createMessage(person, org.springframework.util.StringUtils.arrayToCommaDelimitedString(watcherEmailAddresses
                            .toArray(new String[watcherEmailAddresses.size()])),
                    subjAndBody);
            LOGGER.info("Message {} created for EarlyAlert {}", message, earlyAlert);
        }

        // Send same message to all applicable Campus Early Alert routing
        // entries
        final PagingWrapper<EarlyAlertRouting> routes = earlyAlertRoutingService
                .getAllForCampus(earlyAlert.getCampus(), new SortingAndPaging(
                        ObjectStatus.ACTIVE));

        //1
        if (routes.getResults() > 0) {
            final ArrayList<String> alreadySent = Lists.newArrayList();

            //1
            for (final EarlyAlertRouting route : routes.getRows()) {
                // Check that route applies
                //1
                if ( route.getEarlyAlertReason() == null ) {
                    throw new ObjectNotFoundException(
                            "EarlyAlertRouting missing EarlyAlertReason.", "EarlyAlertReason");
                }

                // Only routes that are for any of the Reasons in this EarlyAlert should be applied.
                //1
                if ( (earlyAlert.getEarlyAlertReasons() == null)
                        || !earlyAlert.getEarlyAlertReasons().contains(route.getEarlyAlertReason()) ) {
                    continue;
                }

                // Send e-mail to specific person
                final Person to = route.getPerson();
                //1
                if ( to != null && StringUtils.isNotBlank(to.getPrimaryEmailAddress()) ) {
                    //check if this alert has already been sent to this recipient, if so skip
                    //1
                    if ( alreadySent.contains(route.getPerson().getPrimaryEmailAddress()) ) {
                        continue;
                    }
                    //1
                    else {
                        alreadySent.add(route.getPerson().getPrimaryEmailAddress());
                    }

                    final Message message = messageService.createMessage(to, null, subjAndBody);
                    LOGGER.info("Message {} for EarlyAlert {} also routed to {}",
                            new Object[]{message, earlyAlert, to}); // NOPMD
                }

                // Send e-mail to a group
                //1
                if ( !StringUtils.isEmpty(route.getGroupName()) && !StringUtils.isEmpty(route.getGroupEmail()) ) {
                    final Message message = messageService.createMessage(route.getGroupEmail(), null,subjAndBody);
                    LOGGER.info("Message {} for EarlyAlert {} also routed to {}", new Object[] { message, earlyAlert, // NOPMD
                            route.getGroupEmail() });
                }
            }
        }
    }

}
