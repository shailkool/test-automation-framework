package com.automation.tests.environment;

import com.automation.core.environment.DatabaseSettings;
import com.automation.core.environment.EnvironmentConfig;
import com.automation.core.environment.EnvironmentContext;
import com.automation.core.environment.MessageQueueSettings;
import com.automation.core.environment.UserCredential;
import com.automation.core.environment.WebsiteSettings;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Verifies the simplified environment config plumbing against the dev/qa/uat2
 * JSON files shipped under {@code src/test/resources/environments}.
 */
public class EnvironmentContextTest {

    @BeforeMethod
    public void resetContext() {
        EnvironmentContext.reset();
    }

    @AfterMethod
    public void clearSystemProperty() {
        System.clearProperty("env");
        EnvironmentContext.reset();
    }

    @Test
    public void defaultsToDevWhenNoSystemPropertyIsSet() {
        System.clearProperty("env");
        EnvironmentConfig config = EnvironmentContext.get();
        assertEquals(config.getName(), "dev");
        assertNotNull(config);
    }

    @Test
    public void loadsQaFromSystemProperty() {
        System.setProperty("env", "qa");
        EnvironmentConfig config = EnvironmentContext.get();
        assertEquals(config.getName(), "qa");
        assertTrue(config.getDatabases().containsKey("archiveDb"),
            "qa.json should expose the Oracle archive DB");
        DatabaseSettings archive = config.getDatabases().get("archiveDb");
        assertEquals(archive.getType(), "oracle");
        assertTrue(archive.getUrl().contains("qa-oracle.internal"));
    }

    @Test
    public void qaExposesMultipleMessageQueuesOfDifferentProviders() {
        System.setProperty("env", "qa");
        EnvironmentConfig config = EnvironmentContext.get();
        MessageQueueSettings kafka = config.getMessageQueues().get("pageViewEvents");
        MessageQueueSettings mq = config.getMessageQueues().get("paymentQueue");
        assertEquals(kafka.getProvider(), "kafka");
        assertEquals(mq.getProvider(), "ibm_mq");
        assertEquals(mq.getProperties().get("queueManager"), "QA.QM1");
    }

    @Test
    public void bbcSiteCarriesItsOwnUserBase() {
        System.setProperty("env", "qa");
        EnvironmentConfig config = EnvironmentContext.get();
        WebsiteSettings bbc = config.getWebsites().get("BBC");
        WebsiteSettings yahoo = config.getWebsites().get("Yahoo");

        assertTrue(bbc.getUsers().containsKey("admin"),
            "BBC in qa should define an admin user");
        assertTrue(!yahoo.getUsers().containsKey("admin"),
            "Yahoo in qa should NOT inherit BBC's admin user");

        UserCredential bbcReader = bbc.getUsers().get("reader");
        UserCredential yahooGuest = yahoo.getUsers().get("guest");
        assertEquals(bbcReader.getRole(), "READER");
        assertEquals(yahooGuest.getRole(), "GUEST");
        assertTrue(bbcReader.getUsername().startsWith("qa."));
    }

    @Test
    public void uat2ExposesItsOwnDistinctDatabaseConnections() {
        System.setProperty("env", "uat2");
        EnvironmentConfig config = EnvironmentContext.get();
        assertEquals(config.getDatabases().get("contentDb").getType(), "mssql");
        assertEquals(config.getDatabases().get("archiveDb").getType(), "oracle");
        assertTrue(config.getDatabases().get("contentDb").getUrl().contains("uat2-db.internal"));
    }

    @Test
    public void singletonCachesOnFirstAccess() {
        System.setProperty("env", "dev");
        EnvironmentConfig first = EnvironmentContext.get();
        EnvironmentConfig second = EnvironmentContext.get();
        assertTrue(first == second, "EnvironmentContext must cache the config singleton per JVM");
    }
}
