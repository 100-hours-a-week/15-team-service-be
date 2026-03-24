package com.sipomeokjo.commitme.global.mongo;

import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MongoSequenceService {

    private final MongoTemplate mongoTemplate;

    public Long nextResumeId() {
        Query query = Query.query(Criteria.where("_id").is("resume_id"));
        Update update = new Update().inc("seq", 1);
        Document result =
                mongoTemplate.findAndModify(
                        query,
                        update,
                        FindAndModifyOptions.options().returnNew(true).upsert(true),
                        Document.class,
                        "counters");
        return ((Number) result.get("seq")).longValue();
    }
}
