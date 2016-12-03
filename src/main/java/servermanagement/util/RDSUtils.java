package main.java.servermanagement.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSAsync;
import com.amazonaws.services.rds.AmazonRDSAsyncClient;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBSnapshot;
import com.amazonaws.services.rds.model.DeleteDBInstanceRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.DescribeDBSnapshotsRequest;
import com.amazonaws.services.rds.model.DescribeDBSnapshotsResult;
import com.amazonaws.services.rds.model.Filter;
import com.amazonaws.services.rds.model.RebootDBInstanceRequest;
import com.amazonaws.services.rds.model.RestoreDBInstanceFromDBSnapshotRequest;
import com.amazonaws.services.rds.model.Tag;

public class RDSUtils {
  public static void main(String[] args) {
    try {
      AWSCredentials credentials = new PropertiesCredentials(new File("/home/aram/Documents/aws/credentials"));
      AmazonRDSAsync rdsAsync = new AmazonRDSAsyncClient(credentials);
      AmazonRDS rds = new AmazonRDSClient(credentials);
      rdsAsync.setRegion(Region.getRegion(Regions.EU_CENTRAL_1));

      Set<String> inst = new HashSet<String>();
      inst.add("nabs-qa1");
      inst.add("nabs-qa2");
      //inst.add("nabs-qa3");
      //inst.add("nabs-qa5");

      List<DBInstance> futures = restoreDBFromSnapshot(rdsAsync, inst, "nabs-qa-17-may-2016");

      //deleteDBInstances(rds, inst);
      //rebootDBInstances(rdsAsync, inst);
      
      /*Filter filter = new Filter().withName("tag:BUNDESBANK").withValues("BUNDESBANK-RDS");
      List<Filter> filters = new ArrayList<Filter>();
      filters.add(filter);
      for (String id : getDBInstances(rdsAsync, filters)) {
        System.out.println(id);
      }*/
      
      System.out.println("Complete");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static List<String> getDBSnapshotes(AmazonRDSAsync rds, Collection<Filter> filters) {
    List<String> snapshotNames = new ArrayList<String>();
    DescribeDBSnapshotsRequest describeRequest = new DescribeDBSnapshotsRequest().withFilters(filters);
    DescribeDBSnapshotsResult describeResult = rds.describeDBSnapshots(describeRequest);

    for (DBSnapshot snapshot : describeResult.getDBSnapshots()) {
      snapshotNames.add(snapshot.getDBSnapshotIdentifier());
    }

    return snapshotNames;
  }

  public static List<String> getDBInstances(AmazonRDSAsync rds, Collection<Filter> filters) {
    List<String> instanceIds = new ArrayList<String>();
    DescribeDBInstancesRequest describeRequest = new DescribeDBInstancesRequest().withFilters(filters);
    DescribeDBInstancesResult describeResult = rds.describeDBInstances(describeRequest);

    for (DBInstance dbInstance : describeResult.getDBInstances()) {
      instanceIds.add(dbInstance.getDBInstanceIdentifier());
    }

    return instanceIds;
  }

  public static List<Future<DBInstance>> rebootDBInstances(AmazonRDSAsync rds, Set<String> dbInstanceIdentifiers) {
    List<Future<DBInstance>> futures = new ArrayList<Future<DBInstance>>();
    RebootDBInstanceRequest rebootRequest = null;

    for (String dBInstanceIdentifier : dbInstanceIdentifiers) {
      rebootRequest = new RebootDBInstanceRequest(dBInstanceIdentifier);
      futures.add(rds.rebootDBInstanceAsync(rebootRequest));
    }

    return futures;
  }

  public static List<DBInstance> deleteDBInstances(AmazonRDSAsync rds, Set<String> dbInstanceIdentifiers) {
    List<DBInstance> dBInstances = new ArrayList<DBInstance>();
    DeleteDBInstanceRequest deleteRequest = null;

    for (String dbInstanceIdentifier : dbInstanceIdentifiers) {
      deleteRequest = new DeleteDBInstanceRequest(dbInstanceIdentifier).withSkipFinalSnapshot(true);
      dBInstances.add(rds.deleteDBInstance(deleteRequest));
    }

    return dBInstances;
  }

  public static List<DBInstance> restoreDBFromSnapshot(AmazonRDSAsync rds, Set<String> dbInstanceIdentifiers,
      String snapshotIdentifier) {
    List<DBInstance> futures = new ArrayList<DBInstance>();
    RestoreDBInstanceFromDBSnapshotRequest restoreRequest = null;

    for (String dbInstanceIdentifier : dbInstanceIdentifiers) {
      restoreRequest = new RestoreDBInstanceFromDBSnapshotRequest(dbInstanceIdentifier, snapshotIdentifier);
      futures.add(rds.restoreDBInstanceFromDBSnapshot(restoreRequest));
    }

    return futures;
  }

  public static List<Future<DBInstance>> restoreDBFromSnapshotAsync(AmazonRDSAsync rds, Set<String> dbInstanceIdentifiers,
      String snapshotIdentifier) {
    List<Future<DBInstance>> futures = new ArrayList<Future<DBInstance>>();
    RestoreDBInstanceFromDBSnapshotRequest restoreRequest = null;
    /*Tag tag = new Tag();
    tag.setKey("Name");
    tag.setValue("GAGO");*/

    for (String dbInstanceIdentifier : dbInstanceIdentifiers) {
      restoreRequest = new RestoreDBInstanceFromDBSnapshotRequest(dbInstanceIdentifier, snapshotIdentifier);
      futures.add(rds.restoreDBInstanceFromDBSnapshotAsync(restoreRequest));
    }

    return futures;
  }
}
