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
package org.jasig.ssp.transferobject.reports;

import java.io.Serializable;


public class EarlyAlertCourseCountsTO implements Serializable {

	private static final long serialVersionUID = 1L;

    private Long totalStudentsReported;
	private Long totalAlerts;
    private String courseName;
    private String courseTitle;
    private String termCode;
    private String campusName;


	public EarlyAlertCourseCountsTO () {

    }

	public String getTermCode() {
		return termCode;
	}

    public void setTermCode(String termCode) {
		this.termCode = termCode;
	}

    public String getCourseName () {
        return courseName;
    }

    public void setCourseName (final String courseName) {
        this.courseName = courseName;
    }

    public String getCampusName () { return campusName; }

    public void setCampusName (final String campusName) { this.campusName = campusName; }

    public String getCourseTitle () {
        return courseTitle;
    }

    public void setCourseTitle (final String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public Long getTotalAlerts () {
        return totalAlerts;
    }

    public void setTotalAlerts (final Long totalAlerts) {
        this.totalAlerts = totalAlerts;
    }

    public Long getTotalStudentsReported () {
        return totalStudentsReported;
    }

    public void setTotalStudentsReported (final Long totalStudentsReported) {
        this.totalStudentsReported = totalStudentsReported;
    }
}
