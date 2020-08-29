package org.jasig.ssp.service.impl;

import org.jasig.ssp.dao.EarlyAlertDao;
import org.jasig.ssp.model.*;
import org.jasig.ssp.model.reference.ProgramStatus;
import org.jasig.ssp.model.reference.StudentType;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.service.PersonProgramStatusService;
import org.jasig.ssp.service.PersonService;
import org.jasig.ssp.service.reference.ProgramStatusService;
import org.jasig.ssp.service.reference.StudentTypeService;
import org.jasig.ssp.web.api.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * @since 29/08/2020
 **/
//21
@Component
public class EarlyAlertCreator {

    //1
    private PersonService personService;
    //1
    private transient EarlyAlertDao earlyAlertDao;
    //1
    private ProgramStatusService programStatusService;
    //1
    private PersonProgramStatusService personProgramStatusService;
    //1
    private StudentTypeService studentTypeService;

    private static final Logger LOGGER = LoggerFactory
            .getLogger(EarlyAlertCreator.class);

    public EarlyAlertCreator(PersonService personService, EarlyAlertDao earlyAlertDao, ProgramStatusService programStatusService, PersonProgramStatusService personProgramStatusService, StudentTypeService studentTypeService) {
        this.personService = personService;
        this.earlyAlertDao = earlyAlertDao;
        this.programStatusService = programStatusService;
        this.personProgramStatusService = personProgramStatusService;
        this.studentTypeService = studentTypeService;
    }

    //6
    public EarlyAlert process(EarlyAlert earlyAlert) throws ValidationException, ObjectNotFoundException {
        //1
        //1:person
        final Person student = earlyAlert.getPerson();

        // Figure student advisor or early alert coordinator
        final UUID assignedAdvisor = getEarlyAlertAdvisor(earlyAlert);
        //1
        if (assignedAdvisor == null) {
            throw new ValidationException(
                    "Could not determine the Early Alert Advisor for student ID "
                            + student.getId());
        }

        //1
        if (student.getCoach() == null
                || assignedAdvisor.equals(student.getCoach().getId())) {
            student.setCoach(personService.get(assignedAdvisor));
        }

        ensureValidAlertedOnPersonStateNoFail(student);

        // Create alert
        final EarlyAlert saved = earlyAlertDao.save(earlyAlert);
        return saved;
    }

    /**
     * Business logic to determine the advisor that is assigned to the student
     * for this Early Alert.
     *
     * @param earlyAlert
     *            EarlyAlert instance
     * @throws ValidationException
     *             If Early Alert, Student, and/or system information could not
     *             determine the advisor for this student.
     * @return The assigned advisor
     */
    //3
    private UUID getEarlyAlertAdvisor(final EarlyAlert earlyAlert)
            throws ValidationException {
        // Check for student already assigned to an advisor (a.k.a. coach)
        //1
        if ((earlyAlert.getPerson().getCoach() != null) &&
                (earlyAlert.getPerson().getCoach().getId() != null)) {
            return earlyAlert.getPerson().getCoach().getId();
        }

        // Get campus Early Alert coordinator
        //1
        if (earlyAlert.getCampus() == null) {
            throw new IllegalArgumentException("Campus ID can not be null.");
        }

        //1
        if (earlyAlert.getCampus().getEarlyAlertCoordinatorId() != null) {
            // Return Early Alert coordinator UUID
            return earlyAlert.getCampus().getEarlyAlertCoordinatorId();
        }

        // TODO If no campus EA Coordinator, assign to default EA Coordinator
        // (which is not yet implemented)

        // getEarlyAlertAdvisor should never return null
        throw new ValidationException(
                "Could not determined the Early Alert Coordinator for this student. Ensure that a default coordinator is set globally and for all campuses.");
    }

    //2
    private void ensureValidAlertedOnPersonStateNoFail(Person person) {
        //1
        try {
            ensureValidAlertedOnPersonStateOrFail(person);
        }
        //1
        catch ( Exception e ) {
            LOGGER.error("Unable to set a program status or student type on "
                    + "person '{}'. This is likely to prevent that person "
                    + "record from appearing in caseloads, student searches, "
                    + "and some reports.", person.getId(), e);
        }
    }

    //5
    private void ensureValidAlertedOnPersonStateOrFail(Person person)
            throws ObjectNotFoundException, ValidationException {

        //1
        if ( person.getObjectStatus() != ObjectStatus.ACTIVE ) {
            person.setObjectStatus(ObjectStatus.ACTIVE);
        }

        final ProgramStatus programStatus =  programStatusService.getActiveStatus();
        //1
        if ( programStatus == null ) {
            throw new ObjectNotFoundException(
                    "Unable to find a ProgramStatus representing \"activeness\".",
                    "ProgramStatus");
        }

        Set<PersonProgramStatus> programStatuses =
                person.getProgramStatuses();
        //1
        if ( programStatuses == null || programStatuses.isEmpty() ) {
            PersonProgramStatus personProgramStatus = new PersonProgramStatus();
            personProgramStatus.setEffectiveDate(new Date());
            personProgramStatus.setProgramStatus(programStatus);
            personProgramStatus.setPerson(person);
            programStatuses.add(personProgramStatus);
            person.setProgramStatuses(programStatuses);
            // save should cascade, but make sure custom create logic fires
            personProgramStatusService.save(personProgramStatus);
        }

        //1
        if ( person.getStudentType() == null ) {
            StudentType studentType = studentTypeService.get(StudentType.EAL_ID);
            //1
            if ( studentType == null ) {
                throw new ObjectNotFoundException(
                        "Unable to find a StudentType representing an early "
                                + "alert-assigned type.", "StudentType");
            }
            person.setStudentType(studentType);
        }
    }
}
