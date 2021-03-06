package org.infinispan.distribution.ch.impl;

import org.infinispan.commons.hash.Hash;
import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.distribution.ch.ConsistentHashFactory;
import org.infinispan.marshall.core.Ids;
import org.infinispan.remoting.transport.Address;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Factory for ReplicatedConsistentHash.
 *
 * @author Dan Berindei
 * @author anistor@redhat.com
 * @since 5.2
 */
public class ReplicatedConsistentHashFactory implements ConsistentHashFactory<ReplicatedConsistentHash> {

   @Override
   public ReplicatedConsistentHash create(Hash hashFunction, int numOwners, int numSegments, List<Address> members,
                                          Map<Address, Float> capacityFactors) {
      int[] primaryOwners = new int[numSegments];
      for (int i = 0; i < numSegments; i++) {
         primaryOwners[i] = i % members.size();
      }
      return new ReplicatedConsistentHash(hashFunction, members, primaryOwners);
   }

   @Override
   public ReplicatedConsistentHash updateMembers(ReplicatedConsistentHash baseCH, List<Address> newMembers,
                                                 Map<Address, Float> actualCapacityFactors) {
      if (newMembers.equals(baseCH.getMembers()))
         return baseCH;

      // recompute primary ownership based on the new list of members (removes leavers)
      int numSegments = baseCH.getNumSegments();
      int[] primaryOwners = new int[numSegments];
      int[] nodeUsage = new int[newMembers.size()];
      boolean foundOrphanSegments = false;
      for (int segmentId = 0; segmentId < numSegments; segmentId++) {
         Address primaryOwner = baseCH.locatePrimaryOwnerForSegment(segmentId);
         int primaryOwnerIndex = newMembers.indexOf(primaryOwner);
         primaryOwners[segmentId] = primaryOwnerIndex;
         if (primaryOwnerIndex == -1) {
            foundOrphanSegments = true;
         } else {
            nodeUsage[primaryOwnerIndex]++;
         }
      }

      // ensure leavers are replaced with existing members so no segments are orphan
      if (foundOrphanSegments) {
         for (int i = 0; i < numSegments; i++) {
            if (primaryOwners[i] == -1) {
               int leastUsed = findLeastUsedNode(nodeUsage);
               primaryOwners[i] = leastUsed;
               nodeUsage[leastUsed]++;
            }
         }
      }

      // ensure even spread of ownership
      int minSegmentsPerNode = numSegments / newMembers.size();
      Queue<Integer>[] segmentsByNode = new Queue[newMembers.size()];
      for (int segmentId = 0; segmentId < primaryOwners.length; ++segmentId) {
         int owner = primaryOwners[segmentId];
         Queue<Integer> segments = segmentsByNode[owner];
         if (segments == null) {
            segmentsByNode[owner] = segments = new ArrayDeque<Integer>(minSegmentsPerNode);
         }
         segments.add(segmentId);
      }
      int mostUsedNode = 0;
      for (int node = 0; node < nodeUsage.length; node++) {
         while (nodeUsage[node] < minSegmentsPerNode) {
            // we can take segment from any node that has > minSegmentsPerNode + 1, not only the most used
            if (nodeUsage[mostUsedNode] <= minSegmentsPerNode + 1) {
               mostUsedNode = findMostUsedNode(nodeUsage);
            }
            int segmentId = segmentsByNode[mostUsedNode].poll();
            // we don't have to add the segmentId to the new owner's queue
            primaryOwners[segmentId] = node;
            nodeUsage[mostUsedNode]--;
            nodeUsage[node]++;
         }
      }

      return new ReplicatedConsistentHash(baseCH.getHashFunction(), newMembers, primaryOwners);
   }

   private int findLeastUsedNode(int[] nodeUsage) {
      int res = 0;
      for (int node = 1; node < nodeUsage.length; node++) {
         if (nodeUsage[node] < nodeUsage[res]) {
            res = node;
         }
      }
      return res;
   }

   private int findMostUsedNode(int[] nodeUsage) {
      int res = 0;
      for (int node = 1; node < nodeUsage.length; node++) {
         if (nodeUsage[node] > nodeUsage[res]) {
            res = node;
         }
      }
      return res;
   }

   @Override
   public ReplicatedConsistentHash rebalance(ReplicatedConsistentHash baseCH) {
      return baseCH;
   }

   @Override
   public ReplicatedConsistentHash union(ReplicatedConsistentHash ch1, ReplicatedConsistentHash ch2) {
      if (!ch1.getHashFunction().equals(ch2.getHashFunction())) {
         throw new IllegalArgumentException("The consistent hash objects must have the same hash function");
      }
      if (ch1.getNumSegments() != ch2.getNumSegments()) {
         throw new IllegalArgumentException("The consistent hash objects must have the same number of segments");
      }

      List<Address> unionMembers = new ArrayList<Address>(ch1.getMembers());
      for (Address member : ch2.getMembers()) {
         if (!unionMembers.contains(member)) {
            unionMembers.add(member);
         }
      }

      int[] primaryOwners = new int[ch1.getNumSegments()];
      for (int segmentId = 0; segmentId < primaryOwners.length; segmentId++) {
         Address primaryOwner = ch1.locatePrimaryOwnerForSegment(segmentId);
         int primaryOwnerIndex = unionMembers.indexOf(primaryOwner);
         primaryOwners[segmentId] = primaryOwnerIndex;
      }

      return new ReplicatedConsistentHash(ch1.getHashFunction(), unionMembers, primaryOwners);
   }

   public static class Externalizer extends AbstractExternalizer<ReplicatedConsistentHashFactory> {

      @Override
      public void writeObject(ObjectOutput output, ReplicatedConsistentHashFactory chf) {
      }

      @Override
      @SuppressWarnings("unchecked")
      public ReplicatedConsistentHashFactory readObject(ObjectInput unmarshaller) {
         return new ReplicatedConsistentHashFactory();
      }

      @Override
      public Integer getId() {
         return Ids.REPLICATED_CONSISTENT_HASH_FACTORY;
      }

      @Override
      public Set<Class<? extends ReplicatedConsistentHashFactory>> getTypeClasses() {
         return Collections.<Class<? extends ReplicatedConsistentHashFactory>>singleton(ReplicatedConsistentHashFactory.class);
      }
   }
}
