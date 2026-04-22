package com.automation.tests.environment;

import com.automation.core.environment.DatabaseSettings;
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
import static org.testng.Assert.expectThrows;

/**
 * Verifies the agnostic environment config plumbing against the dev/qa/uat2
 * JSON files shipped under {@code src/test/resources/environments}.
 */
public class EnvironmentContextTest {

    @BeforeMethod
    public void resetContext() {
        EnvironmentContext.reset();
    }

    @AfterMethod
    public void clearSystemProperty() {
        System.clearProperty(EnvironmentContext.ENV_SYSTEM_PROPERTY);
        EnvironmentContext.reset();
    }

    @Test
    public void defaultsToDevWhenNoSystemPropertyIsSet() {
        System.clearProperty(EnvironmentContext.ENV_SYSTEM_PROPERTY);
        EnvironmentContext ctx = EnvironmentContext.getInstance();
        assertEquals(ctx.getEnvironmentName(), "dev");
        assertNotNull(ctx.getConfig());
    }

    @Test
    public void loadsQaFromSystemProperty() {
        System.setProperty(EnvironmentContext.ENV_SYSTEM_PROPERTY, "qa");
        EnvironmentContext ctx = EnvironmentContext.getInstance();
        assertEquals(ctx.getEnvironmentName(), "qa");
        assertTrue(ctx.getDatabases().containsKey("archiveDb"),
            "qa.json should expose the Oracle archive DB");
        DatabaseSettings archive = ctx.getDatabase("archiveDb");
        assertEquals(archive.getType(), "oracle");
        assertTrue(archive.getUrl().contains("qa-oracle.internal"));
    }

    @Test
    public void qaExposesMultipleMessageQueuesOfDifferentProviders() {
        EnvironmentContext ctx = EnvironmentContext.reload("qa");
        MessageQueueSettings kafka = ctx.getMessageQueue("pageViewEvents");
        MessageQueueSettings mq = ctx.getMessageQueue("paymentQueue");
        assertEquals(kafka.getProvider(), "kafka");
        assertEquals(mq.getProvider(), "ibm_mq");
        assertEquals(mq.getProperties().get("queueManager"), "QA.QM1");
    }

    @Test
    public void bbcSiteCarriesItsOwnUserBase() {
        EnvironmentContext ctx = EnvironmentContext.reload("qa");
        WebsiteSettings bbc = ctx.getWebsite("BBC");
        WebsiteSettings yahoo = ctx.getWebsite("Yahoo");

        assertTrue(bbc.getUsers().containsKey("admin"),
            "BBC in qa should define an admin user");
        assertTrue(!yahoo.getUsers().containsKey("admin"),
            "Yahoo in qa should NOT inherit BBC's admin user");

        UserCredential bbcReader = ctx.getUser("BBC", "reader");
        UserCredential yahooGuest = ctx.getUser("Yahoo", "guest");
        assertEquals(bbcReader.getRole(), "READER");
        assertEquals(yahooGuest.getRole(), "GUEST");
        assertTrue(bbcReader.getUsername().startsWith("qa."));
    }

    @Test
    public void uat2ExposesItsOwnDistinctDatabaseConnections() {
        EnvironmentContext ctx = EnvironmentContext.reload("uat2");
        assertEquals(ctx.getDatabase("contentDb").getType(), "mssql");
        assertEquals(ctx.getDatabase("archiveDb").getType(), "oracle");
        assertTrue(ctx.getDatabase("contentDb").getUrl().contains("uat2-db.internal"));
    }

    @Test
    public void singletonCachesOnFirstAccess() {
        System.setProperty(EnvironmentContext.ENV_SYSTEM_PROPERTY, "dev");
        EnvironmentContext first = EnvironmentContext.getInstance();
        EnvironmentContext second = EnvironmentContext.getInstance();
        assertTrue(first == second, "EnvironmentContext must be a singleton per JVM");
    }

    @Test
    public void unknownDatabaseRaisesDescriptiveError() {
        EnvironmentContext ctx = EnvironmentContext.reload("dev");
        IllegalArgumentException thrown = expectThrows(IllegalArgumentException.class,
            () -> ctx.getDatabase("nope"));
        assertTrue(thrown.getMessage().contains("dev"));
        assertTrue(thrown.getMessage().contains("nope"));
    }
}
