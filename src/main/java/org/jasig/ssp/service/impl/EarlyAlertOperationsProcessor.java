package org.jasig.ssp.service.impl;

import org.jasig.ssp.dao.EarlyAlertDao;
import org.jasig.ssp.model.EarlyAlert;
import org.jasig.ssp.security.SspUser;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.service.SecurityService;
import org.jasig.ssp.web.api.validation.ValidationException;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

/**
 * @since 03/09/2020
 **/
//8
@Component
public class EarlyAlertOperationsProcessor {

    private static final String OPEN = "Opened";
    private static final String CLOSE = "Closed";

    //1
    private final SecurityService securityService;

    public EarlyAlertOperationsProcessor(SecurityService securityService) {
        this.securityService = securityService;
    }

    //5
    public void close(UUID earlyAlertId, EarlyAlertDao earlyAlertDao) throws ObjectNotFoundException, ValidationException {
        //1
        //1
        //1
        //1
        final EarlyAlert earlyAlert = earlyAlertDao.get(earlyAlertId);
        //1
        final SspUser sspUser = securityService.currentUser();

        validate(earlyAlert, earlyAlertId, CLOSE, sspUser);
        earlyAlert.setClosedDate(new Date());
        earlyAlert.setClosedBy(sspUser.getPerson());

        // This save will result in a Hib session flush, which works fine with
        // our current usage. Future use cases might prefer to delay the
        // flush and we can address that when the time comes. Might not even
        // need to change anything here if it turns out nothing actually
        // *depends* on the flush.
        earlyAlertDao.save(earlyAlert);
    }

    //0
    public void open(UUID earlyAlertId, EarlyAlertDao earlyAlertDao) throws ObjectNotFoundException, ValidationException {
        final EarlyAlert earlyAlert = earlyAlertDao.get(earlyAlertId);

        // DAOs don't implement ObjectNotFoundException consistently and we'd
        // rather they not implement it at all, so a small attempt at 'future
        // proofing' here
        final SspUser sspUser = securityService.currentUser();
        validate(earlyAlert, earlyAlertId, OPEN, sspUser);
        earlyAlert.setClosedDate(null);
        earlyAlert.setClosedBy(null);

        // This save will result in a Hib session flush, which works fine with
        // our current usage. Future use cases might prefer to delay the
        // flush and we can address that when the time comes. Might not even
        // need to change anything here if it turns out nothing actually
        // *depends* on the flush.
        earlyAlertDao.save(earlyAlert);
    }

    //3
    private void validate(EarlyAlert earlyAlert, UUID earlyAlertId, String operation, SspUser sspUser) throws ObjectNotFoundException, ValidationException {
        //1
        if ( earlyAlert == null ) {
            throw new ObjectNotFoundException(earlyAlertId, EarlyAlert.class.getName());
        }
        //1
        if ( earlyAlert.getClosedDate() == null ) {
            return;
        }

        //1
        if ( sspUser == null ) {
            throw new ValidationException(String.format("Early Alert cannot be %s by a null User.", operation));
        }
    }

}
