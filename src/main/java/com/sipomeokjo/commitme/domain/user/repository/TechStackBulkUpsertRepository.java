package com.sipomeokjo.commitme.domain.user.repository;

import java.util.Collection;

public interface TechStackBulkUpsertRepository {

    void upsertAll(Collection<TechStackUpsertRow> rows);
}
