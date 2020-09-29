/**
 * Licensed to Apereo under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Apereo licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jasig.ssp.service.impl; // NOPMD by jon.adams

import org.apache.commons.lang.StringUtils;
import org.jasig.ssp.dao.EarlyAlertDao;
import org.jasig.ssp.factory.EarlyAlertSearchResultTOFactory;
import org.jasig.ssp.model.EarlyAlert;
import org.jasig.ssp.model.EarlyAlertSearchResult;
import org.jasig.ssp.model.ObjectStatus;
import org.jasig.ssp.model.Person;
import org.jasig.ssp.model.reference.Campus;
import org.jasig.ssp.service.AbstractPersonAssocAuditableService;
import org.jasig.ssp.service.EarlyAlertService;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.service.reference.ConfigService;
import org.jasig.ssp.transferobject.EarlyAlertSearchResultTO;
import org.jasig.ssp.transferobject.PagedResponse;
import org.jasig.ssp.transferobject.form.EarlyAlertSearchForm;
import org.jasig.ssp.transferobject.reports.*;
import org.jasig.ssp.util.DateTimeUtils;
import org.jasig.ssp.util.collections.Triple;
import org.jasig.ssp.util.sort.PagingWrapper;
import org.jasig.ssp.util.sort.SortingAndPaging;
import org.jasig.ssp.web.api.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.*;


/**
 * EarlyAlert service implementation
 * 
 * @author jon.adams
 * 
 */
//pontuação inicial: 154
//nova pontuação: 24
@Service
@Transactional
public class EarlyAlertServiceImpl extends // NOPMD
		AbstractPersonAssocAuditableService<EarlyAlert>
		implements EarlyAlertService {

	@Autowired
	private transient EarlyAlertDao dao;
	@Autowired
	private transient ConfigService configService;
	@Autowired
	private transient EarlyAlertSearchResultTOFactory searchResultFactory;
	@Autowired
	private EarlyAlertCreator earlyAlertCreator;
	@Autowired
	private MessageToStudentSender messageToStudentSender;
	@Autowired
	private FilltemplateParametersProcessor filltemplateParametersProcessor;
	@Autowired
	private EarlyAlertReminderNotifier earlyAlertReminderNotifier;
	@Autowired
	private EarlyAlertOperationsProcessor earlyAlertOperationsProcessor;
//8
	@Override
	protected EarlyAlertDao getDao() {
		return dao;
	}

	//5
	@Override
	@Transactional(rollbackFor = { ObjectNotFoundException.class, ValidationException.class })
	public EarlyAlert create(@NotNull final EarlyAlert earlyAlert)
			throws ObjectNotFoundException, ValidationException {
		// Validate objects
		if (earlyAlert == null) {
			throw new IllegalArgumentException("EarlyAlert must be provided.");
		}
		if (earlyAlert.getPerson() == null) {
			throw new ValidationException(
					"EarlyAlert Student data must be provided.");
		}
		return earlyAlertCreator.create(earlyAlert);
	}

	//0
	@Override
	public void closeEarlyAlert(UUID earlyAlertId)
			throws ObjectNotFoundException, ValidationException {
			earlyAlertOperationsProcessor.close(earlyAlertId, getDao());
	}

	//0
	@Override
	public void openEarlyAlert(UUID earlyAlertId)
			throws ObjectNotFoundException, ValidationException {
		earlyAlertOperationsProcessor.open(earlyAlertId, getDao());
	}

	//0
	@Override
	public EarlyAlert save(@NotNull final EarlyAlert obj)
			throws ObjectNotFoundException {
		return earlyAlertCreator.save(obj);
	}

	//2
	@Override
	public PagingWrapper<EarlyAlert> getAllForPerson(final Person person,
			final SortingAndPaging sAndP) {
		return getDao().getAllForPersonId(person.getId(), sAndP);
	}

	//0
	@Override
	public void sendMessageToStudent(@NotNull final EarlyAlert earlyAlert)
			throws ObjectNotFoundException,
			ValidationException {
		messageToStudentSender.send(earlyAlert);
	}

	//0
	@Override
	public Map<String, Object> fillTemplateParameters(@NotNull final EarlyAlert earlyAlert) {
		return filltemplateParametersProcessor.process(earlyAlert);
	}

	//0
	@Deprecated
	@Override
	public void applyEarlyAlertCounts(Person person) {
		if ( person == null ) {
			return; // can occur in some legit person lookup call paths
		}
	}

	//0
	@Override
	public Map<UUID, Number> getCountOfActiveAlertsForPeopleIds(
			final Collection<UUID> peopleIds) {
		return dao.getCountOfActiveAlertsForPeopleIds(peopleIds);
	}

	//0
	@Override
	public Map<UUID, Number> getCountOfClosedAlertsForPeopleIds(
			final Collection<UUID> peopleIds) {
		return dao.getCountOfClosedAlertsForPeopleIds(peopleIds);
	}

	//0
	@Override
	public Long getCountOfEarlyAlertsForSchoolIds(
			final Collection<String> schoolIds, Campus campus) {
		return dao.getCountOfAlertsForSchoolIds(schoolIds, campus);
	}

	//0
	@Override
	public Long getEarlyAlertCountForCoach(Person coach, Date createDateFrom, Date createDateTo, List<UUID> studentTypeIds) {
		return dao.getEarlyAlertCountForCoach(coach, createDateFrom, createDateTo, studentTypeIds);
	}

	//0
	@Override
	public Long getStudentEarlyAlertCountForCoach(Person coach, Date createDateFrom, Date createDateTo, List<UUID> studentTypeIds) {
		return dao.getStudentEarlyAlertCountForCoach(coach, createDateFrom, createDateTo, studentTypeIds);
	}

	//0
	@Override
	public Long getEarlyAlertCountForCreatedDateRange(String termCode, Date createDatedFrom, Date createdDateTo, Campus campus, String rosterStatus) {
		return dao.getEarlyAlertCountForCreatedDateRange(termCode, createDatedFrom, createdDateTo, campus, rosterStatus);
	}

	//0
	@Override
	public Long getClosedEarlyAlertCountForClosedDateRange(Date closedDateFrom, Date closedDateTo, Campus campus, String rosterStatus) {
		return dao.getClosedEarlyAlertCountForClosedDateRange(closedDateFrom, closedDateTo, campus, rosterStatus);
	}

	//0
	@Override
	public Long getClosedEarlyAlertsCountForEarlyAlertCreatedDateRange(String termCode, Date createDatedFrom, Date createdDateTo, Campus campus, String rosterStatus) {
		return dao.getClosedEarlyAlertsCountForEarlyAlertCreatedDateRange(termCode, createDatedFrom, createdDateTo, campus, rosterStatus);
	}

	//0
	@Override
	public Long getStudentCountForEarlyAlertCreatedDateRange(String termCode, Date createDatedFrom,
															 Date createdDateTo, Campus campus, String rosterStatus) {
		return dao.getStudentCountForEarlyAlertCreatedDateRange(termCode, createDatedFrom, createdDateTo, campus, rosterStatus);
	}

	//1
	@Override
	public PagingWrapper<EarlyAlertStudentReportTO> getStudentsEarlyAlertCountSetForCriteria(
			EarlyAlertStudentSearchTO earlyAlertStudentSearchTO,
			SortingAndPaging createForSingleSort) {
		return dao.getStudentsEarlyAlertCountSetForCriteria(earlyAlertStudentSearchTO, createForSingleSort);
	}

	//1
    @Override
    public  List<EarlyAlertCourseCountsTO> getStudentEarlyAlertCountSetPerCourses(
            String termCode, Date createdDateFrom, Date createdDateTo, Campus campus, ObjectStatus objectStatus ) {
        return dao.getStudentEarlyAlertCountSetPerCourses(termCode, createdDateFrom, createdDateTo, campus, objectStatus);
    }

    //0
	@Override
	public Long getStudentEarlyAlertCountSetPerCoursesTotalStudents(
			String termCode, Date createdDateFrom, Date createdDateTo, Campus campus, ObjectStatus objectStatus ) {
		return dao.getStudentEarlyAlertCountSetPerCoursesTotalStudents(termCode, createdDateFrom, createdDateTo, campus, objectStatus);
	}

	//0
    @Override
    public  List<Triple<String, Long, Long>> getEarlyAlertReasonTypeCountByCriteria(
            Campus campus, String termCode, Date createdDateFrom, Date createdDateTo, ObjectStatus status) {
        return dao.getEarlyAlertReasonTypeCountByCriteria(campus, termCode, createdDateFrom, createdDateTo, status);
    }

    //1
    @Override
    public List<EarlyAlertReasonCountsTO> getStudentEarlyAlertReasonCountByCriteria(
            String termCode, Date createdDateFrom, Date createdDateTo, Campus campus, ObjectStatus objectStatus) {
        return dao.getStudentEarlyAlertReasonCountByCriteria(termCode, createdDateFrom, createdDateTo, campus, objectStatus);
    }

    //0
	@Override
	public Long getStudentEarlyAlertReasonCountByCriteriaTotalStudents(
			String termCode, Date createdDateFrom, Date createdDateTo, Campus campus, ObjectStatus objectStatus) {
		return dao.getStudentEarlyAlertReasonCountByCriteriaTotalStudents(termCode, createdDateFrom, createdDateTo, campus, objectStatus);
	}

	//1
	@Override
	public PagingWrapper<EntityStudentCountByCoachTO> getStudentEarlyAlertCountByCoaches(EntityCountByCoachSearchForm form) {
		return dao.getStudentEarlyAlertCountByCoaches(form);
	}

	//0
	@Override
	public Long getEarlyAlertCountSetForCriteria(EarlyAlertStudentSearchTO searchForm){
		return dao.getEarlyAlertCountSetForCriteria(searchForm);
	}

	//0
	@Override
	public void sendAllEarlyAlertReminderNotifications() {
		earlyAlertReminderNotifier.send();
	}

	//1
	public Map<UUID,Number> getResponsesDueCountEarlyAlerts(List<UUID> personIds){
		Date lastResponseDate = getMinimumResponseComplianceDate();
		if(lastResponseDate == null)
			return new HashMap<UUID,Number>();
		return dao.getResponsesDueCountEarlyAlerts(personIds, lastResponseDate);
	}

	//1
	private Date getMinimumResponseComplianceDate(){
		final String numVal = configService
				.getByNameNull("maximum_days_before_early_alert_response");
		if(StringUtils.isBlank(numVal))
			return null;
		int allowedDaysPastResponse = Integer.parseInt(numVal);
		
	    return DateTimeUtils.getDateOffsetInDays(new Date(), -allowedDaysPastResponse);
				
	}

	//3
	@Override
	public PagedResponse<EarlyAlertSearchResultTO> searchEarlyAlert(
			EarlyAlertSearchForm form) {
		PagingWrapper<EarlyAlertSearchResult> models = dao.searchEarlyAlert(form);
		return new PagedResponse<>(true,
				models.getResults(), searchResultFactory.asTOList(models.getRows()));	
	}
}