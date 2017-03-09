package com.asymmetrik.nifi.mongo.processors;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Sets;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;

import org.apache.commons.io.IOUtils;
import org.apache.nifi.annotation.behavior.SupportsBatching;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.PropertyValue;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.bson.Document;

import static com.asymmetrik.nifi.mongo.processors.MongoProps.COLLECTION;
import static com.asymmetrik.nifi.mongo.processors.MongoProps.DATABASE;
import static com.asymmetrik.nifi.mongo.processors.MongoProps.LIMIT;
import static com.asymmetrik.nifi.mongo.processors.MongoProps.MONGO_SERVICE;
import static com.asymmetrik.nifi.mongo.processors.MongoProps.PROJECTION;
import static com.asymmetrik.nifi.mongo.processors.MongoProps.QUERY;
import static com.asymmetrik.nifi.mongo.processors.MongoProps.SORT;
import static com.asymmetrik.nifi.mongo.processors.MongoProps.WRITE_CONCERN;

@SupportsBatching
@Tags({"asymmetrik", "mongo", "query"})
@CapabilityDescription("Performs mongo queries.")
public class QueryMongo extends AbstractMongoProcessor {

    Integer limit;
    private List<PropertyDescriptor> props = Arrays.asList(MONGO_SERVICE, DATABASE, COLLECTION, QUERY, PROJECTION, SORT, LIMIT, WRITE_CONCERN);
    private PropertyValue queryProperty;
    private PropertyValue projectionProperty;
    private PropertyValue sortProperty;

    @Override
    protected void init(ProcessorInitializationContext context) {
        properties = Collections.unmodifiableList(props);
        relationships = Collections.unmodifiableSet(Sets.newHashSet(REL_SUCCESS, REL_FAILURE));
        clientId = getIdentifier();
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {
        queryProperty = context.getProperty(QUERY);
        projectionProperty = context.getProperty(PROJECTION);
        sortProperty = context.getProperty(SORT);

        limit = context.getProperty(LIMIT).isSet() ? context.getProperty(LIMIT).asInteger() : null;

        createMongoConnection(context);
        ensureIndexes(context, collection);
    }

    @Override
    public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
        FlowFile flowFile = session.get();
        if (flowFile == null) {
            return;
        }

        ComponentLog logger = this.getLogger();

        // Evaluate expression language and create BSON Documents
        Document query = (queryProperty.isSet()) ? Document.parse(queryProperty.evaluateAttributeExpressions(flowFile).getValue()) : null;
        Document projection = (projectionProperty.isSet()) ? Document.parse(projectionProperty.evaluateAttributeExpressions(flowFile).getValue()) : null;
        Document sort = (sortProperty.isSet()) ? Document.parse(sortProperty.evaluateAttributeExpressions(flowFile).getValue()) : null;

        try {
            FindIterable<Document> it = (query != null) ? collection.find(query) : collection.find();

            // Apply projection if needed
            if (projection != null) {
                it.projection(projection);
            }

            // Apply sort if needed
            if (sort != null) {
                it.sort(sort);
            }

            // Apply limit if set
            if (limit != null) {
                it.limit(limit.intValue());
            }

            // Iterate and create flowfile for each result
            final MongoCursor<Document> cursor = it.iterator();
            try {
                while (cursor.hasNext()) {
                    // Create new flowfile with all parent attributes
                    FlowFile ff = session.clone(flowFile);
                    ff = session.write(ff, new OutputStreamCallback() {
                        @Override
                        public void process(OutputStream outputStream) throws IOException {
                            IOUtils.write(cursor.next().toJson(), outputStream);
                        }
                    });

                    session.transfer(ff, REL_SUCCESS);
                }
            } finally {
                cursor.close();
                session.remove(flowFile);
            }

        } catch (Exception e) {
            logger.error("Failed to execute query {} due to {}.", new Object[]{query, e}, e);
            flowFile = session.putAttribute(flowFile, "mongo.exception", e.getMessage());
            session.transfer(flowFile, REL_FAILURE);
        }
    }
}
