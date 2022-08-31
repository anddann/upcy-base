package de.upb.upcy.base.sigtest;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import de.upb.upcy.base.sigtest.db.MongoDBHandler;
import de.upb.upcy.base.sigtest.db.model.generate.SigTestDBDoc;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import org.bson.Document;
import org.bson.types.ObjectId;

public class MainDelDuplicate {

  public static void main(String[] args) {
    HashSet<String> seenGavs = new HashSet<>();
    MongoDBHandler mongoDBHandler = MongoDBHandler.getInstance();
    final MongoCollection<SigTestDBDoc> sigTestGenerateCollection =
        mongoDBHandler.getSigTestGenerateCollection();
    FindIterable<SigTestDBDoc> iterDoc = sigTestGenerateCollection.find();
    Iterator<SigTestDBDoc> it = iterDoc.iterator();
    ArrayList<ObjectId> idsToDelete = new ArrayList<>();
    int i = 0;
    while (it.hasNext()) {
      final SigTestDBDoc next = it.next();
      String gav =
          next.getArtifactInfo().getGroupId()
              + ":"
              + next.getArtifactInfo().getArtifactId()
              + ":"
              + next.getArtifactInfo().getVersion();
      System.out.println("Checking document: " + i++);
      if (seenGavs.contains(gav)) {
        final ObjectId id = next.getId();
        idsToDelete.add(id);
      } else {
        seenGavs.add(gav);
      }
    }
    System.out.println("Found Dupl #" + idsToDelete.size());

    for (ObjectId objectId : idsToDelete) {
      System.out.println(objectId);
      sigTestGenerateCollection.deleteOne(new Document("_id", objectId));
    }
  }
}
