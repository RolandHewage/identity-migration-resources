package org.wso2.carbon.is.migration.service.v5100.migrator;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.core.migrate.MigrationClientException;
import org.wso2.carbon.is.migration.internal.ISMigrationServiceDataHolder;
import org.wso2.carbon.is.migration.service.Migrator;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.common.User;
import org.wso2.carbon.user.core.ldap.ReadWriteLDAPUserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.List;
import java.util.UUID;

public class UserIDMigrator extends Migrator {

    private static final Logger log = LoggerFactory.getLogger(UserIDMigrator.class);

    private static final String INCREMENT_PARAMETER_NAME = "increment";
    private static final String STARTING_POINT_PARAMETER_NAME = "starting_point";

    private static int INCREMENT = 1000;
    private static int STARTING_POINT = 0;

    private static final int SUPER_TENANT_ID = -1234;
    private static final String USER_ID_CLAIM = "http://wso2.org/claim/userid";
    private static final String USERNAME_CLAIM = "http://wso2.org/claim/username";

    private static final String DEFAULT_PROFILE = "default";

    private RealmService realmService = ISMigrationServiceDataHolder.getRealmService();

    @Override
    public void migrate() throws MigrationClientException {

        String startingPointValue = getMigratorConfig().getParameterValue(STARTING_POINT_PARAMETER_NAME);
        String incrementValue = getMigratorConfig().getParameterValue(INCREMENT_PARAMETER_NAME);

        if (StringUtils.isNotEmpty(startingPointValue)) {
            STARTING_POINT = Integer.parseInt(startingPointValue);
        }

        if (StringUtils.isNotEmpty(incrementValue)) {
            INCREMENT = Integer.parseInt(incrementValue);
        }

        log.info("User ID migrator started with offset {} and increment {} .", STARTING_POINT, INCREMENT);

        int i = STARTING_POINT;
        try {
            UserRealm userRealm = realmService.getTenantUserRealm(SUPER_TENANT_ID);
            UserStoreManager userStoreManager = userRealm.getUserStoreManager();

            while (true) {
                List<User> userList = ((AbstractUserStoreManager) userStoreManager).listUsersWithID("*", INCREMENT, i);
                for (User user : userList) {
                    if (userStoreManager instanceof ReadWriteLDAPUserStoreManager) {
                        updateUserIDClaim(user, (AbstractUserStoreManager) userStoreManager);
                    }
                    updateUserNameClaim(user, (AbstractUserStoreManager) userStoreManager);
                    i++;
                }
                if (userList.size() < INCREMENT) {
                    break;
                }
            }
        } catch (UserStoreException e) {
            String message = String.format("Error occurred while updating user id for the user. user id updating " +
                    "process stopped at the offset %d", i);
            log.error(message, e);
        }
    }

    private void updateUserIDClaim(User user, AbstractUserStoreManager abstractUserStoreManager)
            throws org.wso2.carbon.user.core.UserStoreException {

        String uuid = UUID.randomUUID().toString();
        abstractUserStoreManager.setUserClaimValue(user.getUsername(), USER_ID_CLAIM, uuid, DEFAULT_PROFILE);
    }

    private void updateUserNameClaim(User user, AbstractUserStoreManager abstractUserStoreManager)
            throws org.wso2.carbon.user.core.UserStoreException {

        abstractUserStoreManager.setUserClaimValue(user.getUsername(), USERNAME_CLAIM, user.getUsername(),
                DEFAULT_PROFILE);
    }
}
