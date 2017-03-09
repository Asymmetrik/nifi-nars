package com.asymmetrik.nifi.standard.processors;

import java.util.List;

import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class Md5HashAttributesTest {

    @Test
    public void singleAttributeTest() {
        TestRunner runner = TestRunners.newTestRunner(Md5HashAttributes.class);
        runner.setProperty("hash", "${attribute1}");

        ProcessSession session = runner.getProcessSessionFactory().createSession();
        FlowFile ff = session.create();
        ff = session.putAttribute(ff, "attribute1", "pbs.twimg.com/media/ClvFsHiUgAE4fT6.jpg");
        runner.enqueue(ff);
        runner.run();


        // All results were processed with out failure
        runner.assertQueueEmpty();

        runner.assertTransferCount(Md5HashAttributes.REL_SUCCESS, 1);

        // If you need to read or do additional tests on results you can access the content
        List<MockFlowFile> results = runner.getFlowFilesForRelationship(Md5HashAttributes.REL_SUCCESS);
        assertTrue("1 match", results.size() == 1);
        MockFlowFile result = results.get(0);

        result.assertAttributeEquals("hash", "e2c26a2479f497562a615a42ecb1ef7e");
    }


    @Test
    public void multipleAttributeTest() {
        TestRunner runner = TestRunners.newTestRunner(Md5HashAttributes.class);
        runner.setProperty("hash", "${attribute1}");
        runner.setProperty("hash2", "${attribute2}");

        ProcessSession session = runner.getProcessSessionFactory().createSession();
        FlowFile ff = session.create();
        ff = session.putAttribute(ff, "attribute1", "pbs.twimg.com/media/ClvFsHiUgAE4fT6.jpg");
        ff = session.putAttribute(ff, "attribute2", "");

        runner.enqueue(ff);
        runner.run();


        // All results were processed with out failure
        runner.assertQueueEmpty();

        runner.assertTransferCount(Md5HashAttributes.REL_SUCCESS, 1);

        // If you need to read or do additional tests on results you can access the content
        List<MockFlowFile> results = runner.getFlowFilesForRelationship(Md5HashAttributes.REL_SUCCESS);
        assertTrue("1 match", results.size() == 1);
        MockFlowFile result = results.get(0);

        result.assertAttributeEquals("hash", "e2c26a2479f497562a615a42ecb1ef7e");
        result.assertAttributeEquals("hash2", "d41d8cd98f00b204e9800998ecf8427e");

    }
}