package de.upb.upcy.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import org.junit.Test;

public class RandomTest {

  @Test
  public void checkRandGenerate() {

    ArrayList<Integer> newerVersion = new ArrayList<>();
    newerVersion.add(1);
    newerVersion.add(2);
    newerVersion.add(3);
    newerVersion.add(4);
    newerVersion.add(5);
    newerVersion.add(6);

    ArrayList<Integer> jsonUpdateNodes = new ArrayList<>();
    while (jsonUpdateNodes.size() < 6 && !newerVersion.isEmpty()) {
      double random = Math.random();
      random = random * newerVersion.size();
      Integer randNewVersion = newerVersion.remove((int) random);
      // add to the choosen the vertex set
      jsonUpdateNodes.add(randNewVersion);
    }

    assertFalse(jsonUpdateNodes.isEmpty());
    assertEquals(6, jsonUpdateNodes.size());
    System.out.println(jsonUpdateNodes);
  }

  @Test
  public void checkRandGenerate2() {

    ArrayList<Integer> newerVersion = new ArrayList<>();
    newerVersion.add(1);
    newerVersion.add(2);
    newerVersion.add(3);
    newerVersion.add(4);
    newerVersion.add(5);
    newerVersion.add(6);

    ArrayList<Integer> jsonUpdateNodes = new ArrayList<>();
    while (jsonUpdateNodes.size() < 3 && !newerVersion.isEmpty()) {
      double random = Math.random();
      random = random * newerVersion.size();
      Integer randNewVersion = newerVersion.remove((int) random);
      // add to the choosen the vertex set
      jsonUpdateNodes.add(randNewVersion);
    }

    assertFalse(jsonUpdateNodes.isEmpty());
    assertEquals(3, jsonUpdateNodes.size());
    System.out.println(jsonUpdateNodes);
  }

  @Test
  public void checkRandGenerate3() {

    ArrayList<Integer> newerVersion = new ArrayList<>();
    newerVersion.add(1);
    newerVersion.add(2);
    newerVersion.add(3);
    newerVersion.add(4);
    newerVersion.add(5);
    newerVersion.add(6);

    ArrayList<Integer> jsonUpdateNodes = new ArrayList<>();
    while (jsonUpdateNodes.size() < 10 && !newerVersion.isEmpty()) {
      double random = Math.random();
      random = random * newerVersion.size();
      Integer randNewVersion = newerVersion.remove((int) random);
      // add to the choosen the vertex set
      jsonUpdateNodes.add(randNewVersion);
    }

    assertFalse(jsonUpdateNodes.isEmpty());
    assertEquals(6, jsonUpdateNodes.size());
    System.out.println(jsonUpdateNodes);
  }
}
