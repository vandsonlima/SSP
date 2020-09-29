package org.jasig.ssp.service.impl;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.jasig.ssp.config.EarlyAlertResponseReminderRecipientsConfig;
import org.jasig.ssp.dao.EarlyAlertDao;
import org.jasig.ssp.model.EarlyAlert;
import org.jasig.ssp.model.Person;
import org.jasig.ssp.model.SubjectAndBody;
import org.jasig.ssp.model.WatchStudent;
import org.jasig.ssp.model.reference.Campus;
import org.jasig.ssp.service.MessageService;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.service.PersonService;
import org.jasig.ssp.service.reference.ConfigService;
import org.jasig.ssp.service.reference.MessageTemplateService;
import org.jasig.ssp.transferobject.EarlyAlertTO;
import org.jasig.ssp.transferobject.messagetemplate.EarlyAlertMessageTemplateTO;
import org.jasig.ssp.util.DateTimeUtils;
import org.jasig.ssp.util.collections.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @since 29/08/2020
 **/
//39
@Component
public class EarlyAlertReminderNotifier {

    //1
    private EarlyAlertDao earlyAlertDao;
    //1
    private EarlyAlertResponseReminderRecipientsConfig earReminderRecipientConfig;
    //1
    private PersonService personService;
    //1
    private ConfigService configService;
    //1
    private MessageTemplateService messageTemplateService;
    //1
    private MessageService messageService;

    private static final Logger LOGGER = LoggerFactory
            .getLogger(EarlyAlertReminderNotifier.class);

    public EarlyAlertReminderNotifier(EarlyAlertDao earlyAlertDao, EarlyAlertResponseReminderRecipientsConfig earReminderRecipientConfig, PersonService personService, ConfigService configService, MessageTemplateService messageTemplateService, MessageService messageService) {
        this.earlyAlertDao = earlyAlertDao;
        this.earReminderRecipientConfig = earReminderRecipientConfig;
        this.personService = personService;
        this.configService = configService;
        this.messageTemplateService = messageTemplateService;
        this.messageService = messageService;
    }

    public void send(){
        Date lastResponseDate = getMinimumResponseComplianceDate();
        // if no responseDate is given no emails are sent
        //1
        if (lastResponseDate == null) {
            return;
        }
        List<EarlyAlert> eaOutOfCompliance = earlyAlertDao.getResponseDueEarlyAlerts(lastResponseDate);
        //1
        Map<UUID, List<EarlyAlertMessageTemplateTO>> easByCoach = new HashMap<UUID, List<EarlyAlertMessageTemplateTO>>();
        //1
        Map<UUID, Person> coaches = new HashMap<UUID, Person>();
        final boolean includeCoachAsRecipient = this.earReminderRecipientConfig.includeCoachAsRecipient();
        final boolean includeEarlyAlertCoordinatorAsRecipient = this.earReminderRecipientConfig.includeEarlyAlertCoordinatorAsRecipient();
        final boolean includeEarlyAlertCoordinatorAsRecipientOnlyIfStudentHasNoCoach = this.earReminderRecipientConfig.includeEarlyAlertCoordinatorAsRecipientOnlyIfStudentHasNoCoach();
        LOGGER.info("Config: includeCoachAsRecipient(): {}", includeCoachAsRecipient);
        LOGGER.info("Config: includeEarlyAlertCoordinatorAsRecipient(): {}", includeEarlyAlertCoordinatorAsRecipient);
        LOGGER.info("Config: includeEarlyAlertCoordinatorAsRecipientOnlyIfStudentHasNoCoach(): {}", includeEarlyAlertCoordinatorAsRecipientOnlyIfStudentHasNoCoach);
        //1
        for (EarlyAlert earlyAlert: eaOutOfCompliance){
            final Set<Person> recipients = new HashSet<Person>();
            Person coach = earlyAlert.getPerson().getCoach();
            //1
            if (includeCoachAsRecipient) {
                //1
                if (coach == null) {
                    LOGGER.warn("Early Alert with id: {} is associated with a person without a coach, so skipping email to coach.", earlyAlert.getId());
                }
                //1
                else {
                    recipients.add(coach);
                }
            }
            //1
            if (includeEarlyAlertCoordinatorAsRecipient || (coach == null && includeEarlyAlertCoordinatorAsRecipientOnlyIfStudentHasNoCoach)) {
                final Campus campus = earlyAlert.getCampus();
                //1
                if (campus == null) {
                    LOGGER.error("Early Alert with id: {} does not have valid a campus, so skipping email to EAC.", earlyAlert.getId());
                }
                //1
                else {
                    final UUID earlyAlertCoordinatorId = campus.getEarlyAlertCoordinatorId();
                    //1
                    if ( earlyAlertCoordinatorId == null ) {
                        LOGGER.error("Early Alert with id: {} has campus with no early alert coordinator, so skipping email to EAC.", earlyAlert.getId());
                    }
                    //1
                    else {
                        //1
                        try {
                            final Person earlyAlertCoordinator = personService.get(earlyAlertCoordinatorId);
                            //1
                            if (earlyAlertCoordinator == null) { // guard against change in behavior where ObjectNotFoundException is not thrown (which we've seen)
                                LOGGER.error("Early Alert with id: {} has campus with an early alert coordinator with a bad ID ({}), so skipping email to EAC.", earlyAlert.getId(), earlyAlertCoordinatorId);
                            }
                            //1
                            else {
                                recipients.add(earlyAlertCoordinator);
                            }
                        }
                        //1
                        catch(ObjectNotFoundException exp){
                            LOGGER.error("Early Alert with id: {} has campus with an early alert coordinator with a bad ID ({}), so skipping email to coach because no coach can be resolved.", new Object[] { earlyAlert.getId(), earlyAlertCoordinatorId, exp });
                        }
                    }
                }
            }
            LOGGER.debug("Early Alert: {}; Recipients: {}", earlyAlert.getId(), recipients);
            //1
            if (recipients.isEmpty()) {
                continue;
            }
            //1
            else {
                //1
                for (Person person : recipients) {
                    // We've definitely got a coach by this point
                    //1
                    if (easByCoach.containsKey(person.getId())){
                        final List<EarlyAlertMessageTemplateTO> coachEarlyAlerts = easByCoach.get(person.getId());
                        coachEarlyAlerts.add(createEarlyAlertTemplateTO(earlyAlert));
                    }
                    //1
                    else {
                        coaches.put(person.getId(), person);
                        final ArrayList<EarlyAlertMessageTemplateTO> eam = Lists.newArrayList();
                        eam.add(createEarlyAlertTemplateTO(earlyAlert)); // add separately from newArrayList() call else list will be sized to 1
                        easByCoach.put(person.getId(), eam);
                    }
                }
            }
            List<WatchStudent> watchers = earlyAlert.getPerson().getWatchers();
            //1
            for (WatchStudent watcher : watchers) {
                //1
                if(easByCoach.containsKey(watcher.getPerson().getId())){
                    final List<EarlyAlertMessageTemplateTO> coachEarlyAlerts = easByCoach.get(watcher.getPerson().getId());
                    coachEarlyAlerts.add(createEarlyAlertTemplateTO( earlyAlert));
                }
                //1
                else{
                    coaches.put(watcher.getPerson().getId(), watcher.getPerson());
                    final ArrayList<EarlyAlertMessageTemplateTO> eam = Lists.newArrayList();
                    eam.add(createEarlyAlertTemplateTO( earlyAlert)); // add separately from newArrayList() call else list will be sized to 1
                    easByCoach.put(watcher.getPerson().getId(), eam);
                }
            }
        }
        //1
        for(UUID coachId: easByCoach.keySet()){
            Map<String,Object> messageParams = new HashMap<String,Object>();

            Collections.sort(easByCoach.get(coachId), new Comparator<EarlyAlertTO>() {
                @Override
                public int compare(EarlyAlertTO p1, EarlyAlertTO p2) {
                    Date p1Date = p1.getLastResponseDate();
                    //1
                    if (p1Date == null)
                        p1Date = p1.getCreatedDate();
                    Date p2Date = p2.getLastResponseDate();
                    //1
                    if (p2Date == null)
                        p2Date = p2.getCreatedDate();
                    return p1Date.compareTo(p2Date);
                }

            });

            Integer daysSince1900ResponseExpected =  DateTimeUtils.daysSince1900(lastResponseDate);
            //1
            List<Pair<EarlyAlertMessageTemplateTO,Integer>> earlyAlertTOPairs = new ArrayList<Pair<EarlyAlertMessageTemplateTO,Integer>>();
            //1
            for(EarlyAlertMessageTemplateTO ea:easByCoach.get(coachId)){
                Integer daysOutOfCompliance;
                //1
                if(ea.getLastResponseDate() != null){
                    daysOutOfCompliance = daysSince1900ResponseExpected - DateTimeUtils.daysSince1900(ea.getLastResponseDate());
                }
                //1
                else{
                    daysOutOfCompliance = daysSince1900ResponseExpected - DateTimeUtils.daysSince1900(ea.getCreatedDate());
                }

                // Just in case attempt to only send emails for EA full day out of compliance
                //1
                if(daysOutOfCompliance >= 0)
                    earlyAlertTOPairs.add(new Pair<EarlyAlertMessageTemplateTO,Integer>(ea, daysOutOfCompliance));
            }
            messageParams.put("earlyAlertTOPairs", earlyAlertTOPairs);
            messageParams.put("coach", coaches.get(coachId));
            messageParams.put("DateTimeUtils", DateTimeUtils.class);
            messageParams.put("termToRepresentEarlyAlert",
                    configService.getByNameEmpty("term_to_represent_early_alert"));


            //1
            SubjectAndBody subjAndBody = messageTemplateService.createEarlyAlertResponseRequiredToCoachMessage(messageParams);
            //1
            try{
                messageService.createMessage(coaches.get(coachId), null, subjAndBody);
            }
            //1
            catch(Exception exp){
                LOGGER.error("Unable to send reminder emails to coach: " + coaches.get(coachId).getFullName() + "\n", exp);
            }
        }
    }


    //3
    private EarlyAlertMessageTemplateTO createEarlyAlertTemplateTO(EarlyAlert earlyAlert){
        //1
        Person creator = null;
        //1
        try{
            creator = personService.get(earlyAlert.getCreatedBy().getId());
        }
        //1
        catch(ObjectNotFoundException exp){
            LOGGER.error("Early Alert with id: " + earlyAlert.getId() + " does not have valid creator: " + earlyAlert.getCreatedBy(), exp);
        }
        return new EarlyAlertMessageTemplateTO(earlyAlert, creator,earlyAlert.getPerson().getWatcherEmailAddresses());
    }

    //1
    private Date getMinimumResponseComplianceDate(){
        final String numVal = configService
                .getByNameNull("maximum_days_before_early_alert_response");
        if(StringUtils.isBlank(numVal))
            return null;
        Integer allowedDaysPastResponse = Integer.parseInt(numVal);

        return DateTimeUtils.getDateOffsetInDays(new Date(), -allowedDaysPastResponse);

    }

}
