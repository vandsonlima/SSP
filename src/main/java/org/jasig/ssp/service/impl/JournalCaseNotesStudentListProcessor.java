package org.jasig.ssp.service.impl;

import org.apache.commons.lang.StringUtils;
import org.jasig.ssp.dao.JournalEntryDao;
import org.jasig.ssp.dao.PersonDao;
import org.jasig.ssp.model.ObjectStatus;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.transferobject.reports.BaseStudentReportTO;
import org.jasig.ssp.transferobject.reports.JournalCaseNotesStudentReportTO;
import org.jasig.ssp.transferobject.reports.JournalStepSearchFormTO;
import org.jasig.ssp.transferobject.reports.JournalStepStudentReportTO;
import org.jasig.ssp.util.sort.PagingWrapper;
import org.jasig.ssp.util.sort.SortingAndPaging;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.validation.constraints.NotNull;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 29/08/2020
 **/
//8
@Component
public class JournalCaseNotesStudentListProcessor {

    //1
    private PersonDao personDao;
    //1
    private JournalEntryDao journalEntryDao;

    public JournalCaseNotesStudentListProcessor(PersonDao personDao, JournalEntryDao journalEntryDao) {
        this.personDao = personDao;
        this.journalEntryDao = journalEntryDao;
    }

    public List<JournalCaseNotesStudentReportTO> process(@NotNull JournalStepSearchFormTO personSearchForm, @NotNull SortingAndPaging sAndP) throws ObjectNotFoundException {
        //1:JournalCaseNotesStudentReportTO
        final List<JournalCaseNotesStudentReportTO> personsWithJournalEntries = journalEntryDao.getJournalCaseNoteStudentReportTOsFromCriteria(personSearchForm, sAndP);
        final Map<String, JournalCaseNotesStudentReportTO> map = new HashMap<String, JournalCaseNotesStudentReportTO>();
        //1
        for(JournalCaseNotesStudentReportTO entry:personsWithJournalEntries){
            map.put(entry.getSchoolId(), entry);
        }
        final SortingAndPaging personSAndP = SortingAndPaging.createForSingleSortAll(ObjectStatus.ACTIVE, "lastName", "DESC") ;
        //1:BaseStudentReportTO
        final PagingWrapper<BaseStudentReportTO> persons = personDao.getStudentReportTOs(personSearchForm, personSAndP);

        //1
        if (persons == null) {
            return personsWithJournalEntries;
        }

        //1
        for (BaseStudentReportTO person:persons) {
            //1
            if (!map.containsKey(person.getSchoolId()) && StringUtils.isNotBlank(person.getCoachSchoolId()) &&
                journalEntryDao.getJournalCountForPersonForJournalSourceIds(person.getId(), personSearchForm.getJournalSourceIds()) != 0) {
                    final JournalCaseNotesStudentReportTO entry = new JournalCaseNotesStudentReportTO(person);
                    personsWithJournalEntries.add(entry);
                    map.put(entry.getSchoolId(), entry);
            }
        }
        JournalCaseNotesStudentReportTO.sortByStudentName(personsWithJournalEntries);

        return personsWithJournalEntries;
    }


}
