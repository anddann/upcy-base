package de.upb.prestudy.db;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertOneResult;
import de.upb.prestudy.db.model.check.SigTestCheckDBDoc;
import de.upb.prestudy.db.model.generate.SigTestDBDoc;
import de.upb.prestudy.db.model.sootdiff.BasicAPICheckDoc;
import de.upb.prestudy.db.model.sootdiff.CallGraphCheckDoc;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

/**
 * The original sigtest classes use Arrays, but it supports List, thus we use
 * http://modelmapper.org/ to transform them into our own datatype
 */
public class MongoDBHandler {

  private final MongoDatabase database;

  private static final String DB_NAME = "sigtest";
  private final MongoClient mongoClient;

  private static final HashMap<String, MongoDBHandler> instances = new HashMap<>();

  public MongoCollection<SigTestDBDoc> getSigTestGenerateCollection() {
    return sigTestGenerateCollection;
  }

  private final MongoCollection<SigTestDBDoc> sigTestGenerateCollection;

  private final MongoCollection<SigTestCheckDBDoc> sigTestCheckCollection;
  private final MongoCollection<SigTestCheckDBDoc> sigTestCheckSourceCollection;

  private final MongoCollection<BasicAPICheckDoc> sootDiffAPIBasicCheckCollection;
  private final MongoCollection<CallGraphCheckDoc> sootDiffCGCheckCollection;

  public static String getMongoHostFromEnvironment() {
    String res = System.getenv("MONGO_HOST");
    if (res == null || res.isEmpty()) {
      res = "localhost";
    }
    return res;
  }

  public static String getMongoUSERFromEnvironment() {
    String res = System.getenv("MONGO_USER");
    if (res == null || res.isEmpty()) {
      res = "mongo";
    }
    return res;
  }

  public static String getMongoPWDFromEnvironment() {
    String res = System.getenv("MONGO_PW");
    if (res == null || res.isEmpty()) {
      res = "mongo";
    }
    return res;
  }

  public static MongoDBHandler getInstance() {
    return getInstance(
        getMongoHostFromEnvironment(), getMongoUSERFromEnvironment(), getMongoPWDFromEnvironment());
  }

  public static MongoDBHandler getInstance(String db_host, String db_user, String db_pwd) {
    String key = db_user + "@" + db_host;

    return instances.computeIfAbsent(key, x -> new MongoDBHandler(db_host, db_user, db_pwd));
  }

  private MongoDBHandler(String db_host, String db_user, String db_pwd) {
    CodecRegistry pojoCodecRegistry =
        fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(PojoCodecProvider.builder().automatic(true).build()));
    MongoCredential credential =
        MongoCredential.createCredential(db_user, "admin", db_pwd.toCharArray());

    MongoClientSettings settings =
        MongoClientSettings.builder()
            .applyToClusterSettings(
                builder -> builder.hosts(Arrays.asList(new ServerAddress(db_host, 27017))))
            .applyToSslSettings(builder -> builder.enabled(false))
            .codecRegistry(pojoCodecRegistry)
            .credential(credential)
            .build();

    mongoClient = new MongoClient(settings);

    List<Document> databases = mongoClient.listDatabases().into(new ArrayList<>());
    databases.forEach(db -> System.out.println(db.toJson()));

    database = mongoClient.getDatabase(DB_NAME);
    sigTestGenerateCollection = database.getCollection("generate", SigTestDBDoc.class);
    sigTestCheckCollection = database.getCollection("check", SigTestCheckDBDoc.class);
    sigTestCheckSourceCollection = database.getCollection("checkSource", SigTestCheckDBDoc.class);

    sootDiffAPIBasicCheckCollection =
        database.getCollection("sootdiffBasicAPICheck", BasicAPICheckDoc.class);

    sootDiffCGCheckCollection = database.getCollection("sootdiffCGCheck", CallGraphCheckDoc.class);
  }

  public boolean addToDB(SigTestDBDoc dbDocument) {

    final InsertOneResult insertOneResult = sigTestGenerateCollection.insertOne(dbDocument);
    return insertOneResult.wasAcknowledged();
  }

  public boolean addToDB(SigTestCheckDBDoc dbDocument) {
    final InsertOneResult insertOneResult = sigTestCheckCollection.insertOne(dbDocument);
    return insertOneResult.wasAcknowledged();
  }

  public Iterable<SigTestDBDoc> findBy(String groupId, String artifactId) {
    BasicDBObject whereQuery = new BasicDBObject();
    whereQuery.put("artifactInfo.groupId", groupId);
    whereQuery.put("artifactInfo.artifactId", artifactId);

    final FindIterable<SigTestDBDoc> dbDocuments = sigTestGenerateCollection.find(whereQuery);
    // dbDocuments.forEach(System.out::println);
    return dbDocuments;
  }

  public SigTestDBDoc findBy(String groupId, String artifactId, String version) {
    BasicDBObject whereQuery = new BasicDBObject();
    whereQuery.put("artifactInfo.groupId", groupId);
    whereQuery.put("artifactInfo.artifactId", artifactId);
    whereQuery.put("artifactInfo.version", version);

    return sigTestGenerateCollection.find(whereQuery).first();
  }

  public boolean addToDB(BasicAPICheckDoc dbDocument) {
    final InsertOneResult insertOneResult = sootDiffAPIBasicCheckCollection.insertOne(dbDocument);
    return insertOneResult.wasAcknowledged();
  }

  public boolean addToDB(CallGraphCheckDoc callGraphCheckDoc) {
    final InsertOneResult insertOneResult = sootDiffCGCheckCollection.insertOne(callGraphCheckDoc);
    return insertOneResult.wasAcknowledged();
  }

  public Iterable<SigTestCheckDBDoc> findSigCheck(
      SigTestDBDoc baseVersion, SigTestDBDoc nextVersion) {
    BasicDBObject whereQuery = new BasicDBObject();
    whereQuery.put("baseArtifact.groupId", baseVersion.getArtifactInfo().getGroupId());
    whereQuery.put("baseArtifact.artifactId", baseVersion.getArtifactInfo().getArtifactId());
    whereQuery.put("baseArtifact.version", baseVersion.getArtifactInfo().getVersion());

    whereQuery.put("nextArtifact.groupId", nextVersion.getArtifactInfo().getGroupId());
    whereQuery.put("nextArtifact.artifactId", nextVersion.getArtifactInfo().getArtifactId());
    whereQuery.put("nextArtifact.version", nextVersion.getArtifactInfo().getVersion());

    final FindIterable<SigTestCheckDBDoc> dbDocuments = sigTestCheckCollection.find(whereQuery);
    return dbDocuments;
  }

  public Iterable<BasicAPICheckDoc> findSootDiffBasicAPICheck(
      SigTestDBDoc baseVersion, SigTestDBDoc nextVersion) {

    BasicDBObject whereQuery = new BasicDBObject();
    whereQuery.put("baseArtifact.groupId", baseVersion.getArtifactInfo().getGroupId());
    whereQuery.put("baseArtifact.artifactId", baseVersion.getArtifactInfo().getArtifactId());
    whereQuery.put("baseArtifact.version", baseVersion.getArtifactInfo().getVersion());

    whereQuery.put("nextArtifact.groupId", nextVersion.getArtifactInfo().getGroupId());
    whereQuery.put("nextArtifact.artifactId", nextVersion.getArtifactInfo().getArtifactId());
    whereQuery.put("nextArtifact.version", nextVersion.getArtifactInfo().getVersion());

    final FindIterable<BasicAPICheckDoc> dbDocuments =
        sootDiffAPIBasicCheckCollection.find(whereQuery);
    return dbDocuments;
  }

  public Iterable<CallGraphCheckDoc> findSootDiffCGCheck(
      SigTestDBDoc baseVersion, SigTestDBDoc nextVersion) {

    BasicDBObject whereQuery = new BasicDBObject();
    whereQuery.put("baseArtifact.groupId", baseVersion.getArtifactInfo().getGroupId());
    whereQuery.put("baseArtifact.artifactId", baseVersion.getArtifactInfo().getArtifactId());
    whereQuery.put("baseArtifact.version", baseVersion.getArtifactInfo().getVersion());

    whereQuery.put("nextArtifact.groupId", nextVersion.getArtifactInfo().getGroupId());
    whereQuery.put("nextArtifact.artifactId", nextVersion.getArtifactInfo().getArtifactId());
    whereQuery.put("nextArtifact.version", nextVersion.getArtifactInfo().getVersion());

    final FindIterable<CallGraphCheckDoc> dbDocuments = sootDiffCGCheckCollection.find(whereQuery);
    return dbDocuments;
  }

  public boolean addToDBSource(SigTestCheckDBDoc dbDocument) {
    final InsertOneResult insertOneResult = sigTestCheckSourceCollection.insertOne(dbDocument);
    return insertOneResult.wasAcknowledged();
  }

  public Iterable<SigTestCheckDBDoc> findSigCheckSource(
      SigTestDBDoc baseVersion, SigTestDBDoc nextVersion) {
    BasicDBObject whereQuery = new BasicDBObject();
    whereQuery.put("baseArtifact.groupId", baseVersion.getArtifactInfo().getGroupId());
    whereQuery.put("baseArtifact.artifactId", baseVersion.getArtifactInfo().getArtifactId());
    whereQuery.put("baseArtifact.version", baseVersion.getArtifactInfo().getVersion());

    whereQuery.put("nextArtifact.groupId", nextVersion.getArtifactInfo().getGroupId());
    whereQuery.put("nextArtifact.artifactId", nextVersion.getArtifactInfo().getArtifactId());
    whereQuery.put("nextArtifact.version", nextVersion.getArtifactInfo().getVersion());

    final FindIterable<SigTestCheckDBDoc> dbDocuments =
        sigTestCheckSourceCollection.find(whereQuery);
    return dbDocuments;
  }
}
