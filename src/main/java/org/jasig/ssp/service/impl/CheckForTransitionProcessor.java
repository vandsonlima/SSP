package org.jasig.ssp.service.impl;

import org.jasig.ssp.model.JournalEntry;
import org.jasig.ssp.model.JournalEntryDetail;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.service.PersonProgramStatusService;
import org.jasig.ssp.web.api.validation.ValidationException;
import org.springframework.stereotype.Component;

/**
 * @since 29/08/2020
 **/
//4
@Component
public class CheckForTransitionProcessor {

    //1
    private PersonProgramStatusService personProgramStatusService;

    public CheckForTransitionProcessor(PersonProgramStatusService personProgramStatusService) {
        this.personProgramStatusService = personProgramStatusService;
    }

    public void process(final JournalEntry journalEntry)
            throws ObjectNotFoundException, ValidationException {
        // search for a JournalStep that indicates a transition
        //1:JournalEntryDetail
        //1:for
        for (final JournalEntryDetail detail : journalEntry
                .getJournalEntryDetails()) {
            //1
            if (detail.getJournalStepJournalStepDetail().getJournalStep()
                    .isUsedForTransition()) {
                // is used for transition, so attempt to set program status
                personProgramStatusService.setTransitionForStudent(journalEntry
                        .getPerson());

                // exit early because no need to loop through others
                return;
            }
        }
    }
}
