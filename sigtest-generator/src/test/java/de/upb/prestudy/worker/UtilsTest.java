package de.upb.prestudy.worker;

import static org.junit.Assert.assertNotNull;

import de.upb.upcy.base.sigtest.db.MongoDBHandler;
import de.upb.upcy.base.sigtest.db.model.generate.SigTestDBDoc;
import de.upb.upcy.base.sigtest.worker.Utils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Ignore;
import org.junit.Test;

public class UtilsTest {

  @Test
  @Ignore
  public void testGenerateSemanticVersionPairsForComparision() {

    MongoDBHandler mongoDBHandler = MongoDBHandler.getInstance();

    final Iterable<SigTestDBDoc> comsquare =
        mongoDBHandler.findBy("com.squareup.okhttp3", "okhttp");
    assertNotNull(comsquare);
    List<SigTestDBDoc> result = new ArrayList<SigTestDBDoc>();
    comsquare.forEach(result::add);

    final Map<Pair<String, String>, Collection<Pair<SigTestDBDoc, SigTestDBDoc>>>
        pairCollectionMap = Utils.generateSemanticVersionPairsForComparison(result);
    assertNotNull(pairCollectionMap);
  }
}
