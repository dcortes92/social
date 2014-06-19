package org.craftercms.social.repositories.ugc.impl;

import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.util.JSONParseException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.craftercms.commons.collections.IterableUtils;
import org.craftercms.commons.mongo.MongoDataException;
import org.craftercms.social.domain.UGC;
import org.craftercms.social.repositories.SocialJongoRepository;
import org.craftercms.social.repositories.TreeUGC;
import org.craftercms.social.repositories.ugc.UGCRepository;
import org.craftercms.social.repositories.ugc.support.BaseTreeUgc;
import org.jongo.Aggregate;
import org.jongo.Find;
import org.jongo.ResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class UGCRepositoryImpl<T extends UGC> extends SocialJongoRepository implements UGCRepository {


    private Logger log = LoggerFactory.getLogger(UGCRepositoryImpl.class);

    /**
     * Creates a instance of a Jongo Repository.
     */
    public UGCRepositoryImpl() throws Exception {
    }

    //TODO cortiz Fix this To many queries, Jongo Aggregation limitation for now wait for release 1.1 or make a patch
    @Override
    public List<T> findChildrenOf(final String ugcId, final int childrenCount,
                                  final String tenantId) throws MongoDataException {
        try {
            String pt1 = getQueryFor("social.ugc.getTree1");
            String pt2 = getQueryFor("social.ugc.getTree2");
            String pt3 = getQueryFor("social.ugc.getTree3");
            String pt4 = getQueryFor("social.ugc.getTree4");
            String pt5 = getQueryFor("social.ugc.getTree5");
            String pt6 = getQueryFor("social.ugc.getTree6");
            String pt7 = getQueryFor("social.ugc.getTree7");
            String pt8 = getQueryFor("social.ugc.getTree8");
            String pt9 = getQueryFor("social.ugc.getTree9");
            String pt10 = getQueryFor("social.ugc.getTree10");

            if (!ObjectId.isValid(ugcId)) {
                throw new IllegalArgumentException("Given UGC id is not valid");
            }
            ObjectId id = new ObjectId(ugcId);
            Aggregate aggregation = getCollection().aggregate(pt1);
            aggregation.and(pt2, tenantId, Arrays.asList(id), id);
            aggregation.and(pt3).and(pt4).and(pt5).and(pt6).and(pt7).and(pt8);
            aggregation.and(pt9, childrenCount);
            aggregation.and(pt10);
            return toUgcList(aggregation.as(super.ugcFactory.getTreeClass()));
        } catch (Exception ex) {
            log.error("Unable to ", ex);
            throw new MongoDataException("Unable to find children of given UGC", ex);
        }
    }

    @Override
    public Iterable<T> findByTargetId(final String targetId, final String tenantId) throws MongoDataException {
        String query = getQueryFor("social.ugc.byTargetId");
        return (Iterable<T>)find(query, tenantId, targetId);
    }

    @Override
    public void deleteAttribute(final String ugcId, final String tenantId, final String[] attributesName) throws
        MongoDataException {
        String query = getQueryFor("social.ugc.byTenantAndId");
        String delete = getQueryFor("social.ugc.deleteAttribute");
        try {
            if (!ObjectId.isValid(ugcId)) {
                throw new IllegalArgumentException("Given UGC Id is not valid");
            }
            Map<String, Integer> map = new HashMap<>();
            for (String attributeName : attributesName) {
                map.put("attributes." + attributeName, -1);
            }
            checkCommandResult(getCollection().update(query, new ObjectId(ugcId), tenantId).with(delete, map));
        } catch (MongoException ex) {
            log.error("Unable to delete attribute " + attributesName + " for UGC " + ugcId + "of tenantId " +
                tenantId, ex);
            throw new MongoDataException("Unable to delete attribute of a ugc", ex);
        }

    }

    @Override
    public T findUGC(final String tenantId, final String ugcId) throws MongoDataException {
        String query = getQueryFor("social.ugc.byTenantAndId");
        if (ObjectId.isValid(ugcId)) {
            return (T)findOne(query, new ObjectId(ugcId), tenantId);
        } else {
            throw new IllegalArgumentException("Given UGC " + ugcId + " is invalid");
        }
    }

    @Override
    public void setAttributes(final String ugcId, final String tenantId,
                              final Map attributes) throws MongoDataException {
        String query = getQueryFor("social.ugc.byTenantAndId");
        String update = getQueryFor("social.ugc.addAttributes");
        try {
            if (!ObjectId.isValid(ugcId)) {
                throw new IllegalArgumentException("Given UGC Id is not valid");
            }
            checkCommandResult(getCollection().update(query, new ObjectId(ugcId), tenantId).with(update, attributes));
        } catch (MongoException ex) {
            log.error("Unable to delete attribute " + attributes + " for UGC " + ugcId + "of tenantId " + tenantId, ex);
            throw new MongoDataException("Unable to delete attribute of a ugc", ex);
        }
    }


    @Override
    public void deleteUgc(final String ugcId, final String tenantId) throws MongoDataException {
        String delete = getQueryFor("social.ugc.byIds");
        if (!ObjectId.isValid(ugcId)) {
            throw new IllegalArgumentException("Given UGC Id is not valid");
        }
        //Good Candidate for java8
        Collection<ObjectId> toDelete = CollectionUtils.collect(findChildrenOf(ugcId, Integer.MAX_VALUE, tenantId),
            new Transformer<UGC, ObjectId>() {
                @Override
                public ObjectId transform(final UGC input) {
                    return input.getId();
                }
            });
        log.debug("Deleting UGC's {}", toDelete);
        remove(delete, toDelete);
    }

    @Override
    public Iterable<T> findByUserQuery(final String tenant, final String query, final String sort, final int start,
                                       final int limit) throws MongoDataException {
        try {
            StringBuffer realQuery = new StringBuffer(query);
            realQuery.insert(1, "tenantId:\"" + tenant + "\", ");
            Find find = getCollection().find(realQuery.toString());
            if (!StringUtils.isBlank(sort)) {
                find.sort(sort);
            }
            return (Iterable<T>)find.skip(start).limit(limit).as(clazz);
        } catch (MongoException | JSONParseException ex) {
            log.error("Unable to Find UGC with given User query " + query + "sorted by " + sort, ex);
            throw new MongoDataException("Unable to find ugcs by user query", ex);
        }
    }


    @Override
    public Iterable<T> findChildren(final String ugcId, final String targetId, final String tenantId,
                                    final int start, final int limit, final List sortOrder,
                                    final int upToLevel) throws MongoDataException {
        try {
            if (!ObjectId.isValid(ugcId)) {
                throw new IllegalArgumentException("Given UGC id is not valid");
            }
            T parent = findUGC(tenantId,ugcId);
            ArrayDeque<ObjectId> ancestors = parent.getAncestors().clone();
            ancestors.addLast(parent.getId());
            parent = null;
            String query = getQueryFor("social.ugc.byTenantTargetAncestorsExact");
            Find find = getCollection().find(query, tenantId, targetId, ancestors);
            return getUgcsToFind(find, targetId, tenantId, start, limit, sortOrder, (ancestors.size()+upToLevel));
        } catch (MongoException ex) {
            log.error("Unable to find children of " + ugcId, ex);
            throw new MongoDataException("Unable to find children of given UGC", ex);
        }
    }

    @Override
    public Iterable<T> findByTargetId(final String targetId, final String tenantId, final int start, final int limit,
                                      final List sortOrder, final int upToLevel) throws MongoDataException {
        try {
            String query = getQueryFor("social.ugc.byTargetIdRootLvl");
            Find find = getCollection().find(query, tenantId, targetId);
            return getUgcsToFind(find, targetId, tenantId, start, limit, sortOrder, upToLevel);
        } catch (MongoException ex) {
            log.error("Unable to Find UGC's " + targetId + "children", ex);
            throw new MongoDataException("Unable to find ugcs by user query", ex);
        }
    }


    private Iterable<T> getUgcsToFind(final Find initialFind, final String targetId, final String tenantId,
                                      final int start, final int limit, final List sortOrder,
                                      final int upToLevel) throws MongoDataException {
        if (CollectionUtils.isEmpty(sortOrder)) {
            initialFind.sort(getQueryFor("social.ugc.defaultSort"));
        } else {
            initialFind.sort(createSortQuery(sortOrder));
        }
        initialFind.projection("{_id:1}");
        List<ObjectId> listOfIds = IterableUtils.toList(initialFind.skip(start).limit(limit).map(new ResultHandler<ObjectId>() {
            @Override
            public ObjectId map(final DBObject result) {
                return (ObjectId)result.get("_id");
            }
        }));
        String finalQuery = getQueryFor("social.ugc.byTargetIdRootNLvl");
        finalQuery = finalQuery.replaceAll("%@", String.valueOf(upToLevel));
        return find(finalQuery, targetId, tenantId, listOfIds, listOfIds);
    }

    @Override
    public long countByTargetId(final String tenant, final String threadId, final int levels) throws MongoDataException {
        try{
          return count(getQueryFor("social.ugc.byTargetIdWithFixLvl"),tenant,threadId,levels);
        }catch (MongoException ex){
            log.error("Unable to count ugc for tenant "+tenant+ "and target "+threadId,ex);
            throw new MongoDataException("Unable to count ugc for tenant and target",ex);
        }
    }

    @Override
    public long countChildrenOf(final String tenant, final String ugcId) throws MongoDataException {
        try{
            if (!ObjectId.isValid(ugcId)) {
                throw new IllegalArgumentException("Given UGC id is not valid");
            }
            T parent = findUGC(tenant,ugcId);
            ArrayDeque<ObjectId> ancestors = parent.getAncestors().clone();
            ancestors.addLast(parent.getId());
            parent = null;
            return count(getQueryFor("social.ugc.byTenantAncestorsExact"),tenant,ancestors,ancestors.size());
        }catch (MongoException ex){
            log.error("Unable to count ugc for tenant "+tenant+ "and id "+ugcId,ex);
            throw new MongoDataException("Unable to count ugc for tenant and target",ex);
        }
    }


    // Always find UGC by id AND tenantId
    @Override
    public UGC findById(final String id) throws MongoDataException {
        throw new UnsupportedOperationException("Disable for security reasons , use findUgc(String,String)");
    }

    @Override
    public Iterable<UGC> findAll() throws MongoDataException {
        throw new UnsupportedOperationException("Disable for security reasons , use findAllUgc(String,String)");
    }

    private List<T> toUgcList(final List<BaseTreeUgc> as) {
        ArrayList<T> ugcList = new ArrayList<>(as.size());
        for (TreeUGC a : as) {
            ugcList.add((T)a.getUGC());
        }
        return ugcList;
    }


}
