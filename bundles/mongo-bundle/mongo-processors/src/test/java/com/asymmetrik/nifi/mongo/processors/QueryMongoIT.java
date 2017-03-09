package com.asymmetrik.nifi.mongo.processors;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.UUID;

import com.asymmetrik.nifi.mongo.services.MongoClientService;
import com.asymmetrik.nifi.mongo.services.impl.StandardMongoClientService;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.util.JSON;

import org.apache.commons.io.FileUtils;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Requires a Mongo instance running at localhost:28017
 */
public class QueryMongoIT {
    private static final String MONGO_CONTROLLER_SERVICE = "mongo_controller_service";
    private static final String MONGO_DATABASE_NAME = "testQueryMongo" + UUID.randomUUID();

    MongoClientService mongo;

    @Before
    public void setUp() throws Exception {
        mongo = new StandardMongoClientService();
        insertRefinedPayload();
    }

    @After
    public void tearDown() throws Exception {
        if (null != mongo) {
            MongoClient client = mongo.getMongoClient();
            MongoDatabase db = client.getDatabase(MONGO_DATABASE_NAME);
            if (db != null) {
                db.drop();
            }
            client.close();
        }
    }

    protected void addMongoService(TestRunner runner) throws InitializationException {
        HashMap<String, String> props = new HashMap<>();

        // Add mongo controller service
        runner.addControllerService(MONGO_CONTROLLER_SERVICE, mongo, props);
        runner.setProperty(MongoProps.MONGO_SERVICE, MONGO_CONTROLLER_SERVICE);
        runner.enableControllerService(mongo);
    }

    protected void insertRefinedPayload() throws Exception {
        final TestRunner runner = TestRunners.newTestRunner(new StoreInMongo());

        addMongoService(runner);
        runner.setProperty(MongoProps.DATABASE, MONGO_DATABASE_NAME);
        runner.setProperty(MongoProps.COLLECTION, "insert_test");

        String contents = FileUtils.readFileToString(Paths.get("src/test/resources/query_payload.json").toFile());

        runner.enqueue(contents.getBytes());
        runner.run();
    }

    @Test
    public void testQuery() throws Exception {
        final TestRunner runner = TestRunners.newTestRunner(new QueryMongo());
        addMongoService(runner);
        runner.setProperty(MongoProps.DATABASE, MONGO_DATABASE_NAME);
        runner.setProperty(MongoProps.COLLECTION, "insert_test");
        runner.setProperty(MongoProps.QUERY, "{\"criteria\": \"${test_attribute}\"}");

        ProcessSession session = runner.getProcessSessionFactory().createSession();
        FlowFile ff = session.create();
        ff = session.putAttribute(ff, "test_attribute", "12345");

        runner.enqueue(ff);
        runner.run();

        runner.assertTransferCount(AbstractMongoProcessor.REL_FAILURE, 0);
        runner.assertTransferCount(AbstractMongoProcessor.REL_SUCCESS, 1);

        MockFlowFile out = runner.getFlowFilesForRelationship(AbstractMongoProcessor.REL_SUCCESS).get(0);
        BasicDBObject actual = (BasicDBObject) JSON.parse(new String(out.toByteArray(), StandardCharsets.UTF_8));
        assertEquals("[ \"12345\" , \"23456\" , \"34567\"]", actual.getString("criteria"));
    }

    @Test
    public void testNoResults() throws Exception {
        final TestRunner runner = TestRunners.newTestRunner(new QueryMongo());
        addMongoService(runner);
        runner.setProperty(MongoProps.DATABASE, MONGO_DATABASE_NAME);
        runner.setProperty(MongoProps.COLLECTION, "insert_test");
        runner.setProperty(MongoProps.QUERY, "{\"criteria\": \"${test_attribute}\"}");

        ProcessSession session = runner.getProcessSessionFactory().createSession();
        FlowFile ff = session.create();
        ff = session.putAttribute(ff, "test_attribute", "98765");

        runner.enqueue(ff);
        runner.run();

        runner.assertTransferCount(AbstractMongoProcessor.REL_FAILURE, 0);
        runner.assertTransferCount(AbstractMongoProcessor.REL_SUCCESS, 0);
    }
}