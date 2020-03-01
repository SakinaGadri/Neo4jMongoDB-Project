package com.csc301.profilemicroservice;

import static org.neo4j.driver.v1.Values.parameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.springframework.stereotype.Repository;

@Repository
public class ProfileDriverImpl implements ProfileDriver {

  Driver driver = ProfileMicroserviceApplication.driver;

  public static void InitProfileDb() {
    String queryStr;

    try (Session session = ProfileMicroserviceApplication.driver.session()) {
      try (Transaction trans = session.beginTransaction()) {
        queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.userName)";
        trans.run(queryStr);

        queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.password)";
        trans.run(queryStr);

        queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT nProfile.userName IS UNIQUE";
        trans.run(queryStr);

        trans.success();
      }
      session.close();
    }
  }

  @Override
  public DbQueryStatus createUserProfile(String userName, String fullName, String password) {
    DbQueryStatus queryStatus;
    // check params
    if (userName == null || fullName == null || password == null || userName.isEmpty() || fullName
        .isEmpty() || password.isEmpty()) {
      return new DbQueryStatus("Missing Parameters", DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
    try (Session session = this.driver.session()) {
      try (Transaction trans = session.beginTransaction()) {
        // check user in db
        StatementResult result = trans.run(
            "MATCH (nProfile:profile {userName: $userName}) RETURN nProfile",
            parameters("userName", userName));

        if (result.hasNext()) { // user in db
          queryStatus = new DbQueryStatus("User already exist in the DB",
              DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
        } else { // insert into db
          // create new profile and new favorites playlist userName-favorites
          trans.run(
              "CREATE (nProfile:profile {userName: $userName, "
                  + "fullName: $fullName, password: $password})"
                  + "-[:created]->(nPlaylist:playlist {plName:$plName})",
              parameters("userName", userName, "fullName", fullName, "password", password,
                  "plName", userName + "-favorites"));
          queryStatus = new DbQueryStatus("User added to DB", DbQueryExecResult.QUERY_OK);
        }
        trans.success();
      } catch (Exception e) {
        queryStatus = new DbQueryStatus("User not added to DB",
            DbQueryExecResult.QUERY_ERROR_GENERIC);
      }
      session.close();
    }
    return queryStatus;
  }

  @Override
  public DbQueryStatus followFriend(String userName, String frndUserName) {
    DbQueryStatus queryStatus;
    // check params
    if (userName == null || frndUserName == null || userName.isEmpty() || frndUserName.isEmpty()) {
      return new DbQueryStatus("Missing Parameters", DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
    try (Session session = this.driver.session()) {
      try (Transaction trans = session.beginTransaction()) {
        // check users exists
        StatementResult resultUser = trans.run(
            "MATCH (nUser:profile {userName: $userName}) RETURN nUser",
            parameters("userName", userName));
        StatementResult resultFriend = trans.run(
            "MATCH (nFriend:profile {userName: $userName}) RETURN nFriend",
            parameters("userName", frndUserName));
        if (resultUser.hasNext() && resultFriend.hasNext()) { // both users in db
          StatementResult result = trans.run(
              "MATCH (nUser:profile {userName: $userName1}) "
                  + " MATCH (nFriend:profile {userName: $userName2}) "
                  + "RETURN EXISTS ((nUser)-[:follows]->(nFriend)) AS rl_exists",
              parameters("userName1", userName, "userName2", frndUserName));
          if (result.next().get("rl_exists", false)) { // if relationship exists
            queryStatus = new DbQueryStatus("Relationship already exists in the DB",
                DbQueryExecResult.QUERY_ERROR_GENERIC);
          } else {
            result = trans.run(
                "MATCH (nUser:profile {userName: $userName1}) "
                    + " MATCH (nFriend:profile {userName: $userName2}) "
                    + " MERGE (nUser)-[:follows]->(nFriend)",
                parameters("userName1", userName, "userName2", frndUserName));
            queryStatus = new DbQueryStatus(userName + " is now following " + frndUserName,
                DbQueryExecResult.QUERY_OK);
          }
        } else {
          // users not in db
          queryStatus = new DbQueryStatus("User not in DB",
              DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
        }
        trans.success();
        // check relationship created
      } catch (Exception e) {
        e.printStackTrace();
        queryStatus = new DbQueryStatus("Relationship not added to DB",
            DbQueryExecResult.QUERY_ERROR_GENERIC);
      }
      session.close();
    }
    return queryStatus;
  }

  @Override
  public DbQueryStatus unfollowFriend(String userName, String frndUserName) {
    DbQueryStatus queryStatus;
    // check params
    if (userName == null || frndUserName == null || userName.isEmpty() || frndUserName.isEmpty()) {
      return new DbQueryStatus("Missing Parameters", DbQueryExecResult.QUERY_ERROR_GENERIC);
    } else if (userName.compareTo(frndUserName) == 0) {
      return new DbQueryStatus("User cannot unfollow themself",
          DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
    try (Session session = this.driver.session()) {
      try (Transaction trans = session.beginTransaction()) {
        // check users exists
        StatementResult resultUser = trans.run(
            "MATCH (nUser:profile {userName: $userName}) RETURN nUser",
            parameters("userName", userName));
        StatementResult resultFriend = trans.run(
            "MATCH (nFriend:profile {userName: $userName}) RETURN nFriend",
            parameters("userName", frndUserName));
        if (resultUser.hasNext() && resultFriend.hasNext()) { // both users in db
          // check relationship exists
          StatementResult result = trans.run(
              "MATCH (nUser:profile {userName: $userName1}) "
                  + " MATCH (nFriend:profile {userName: $userName2}) "
                  + "RETURN EXISTS ((nUser)-[:follows]->(nFriend)) AS rl_exists",
              parameters("userName1", userName, "userName2", frndUserName));
          if (!result.next().get("rl_exists", false)) { // if relationship doesnt exist
            queryStatus = new DbQueryStatus("Relationship doesn't exist in the DB",
                DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
          } else { // relationship does exist, remove it
            result = trans.run(
                "MATCH (nUser:profile {userName: $userName1}) "
                    + " MATCH (nFriend:profile {userName: $userName2}) "
                    + " MATCH (nUser)-[r:follows]->(nFriend) "
                    + " DELETE r",
                parameters("userName1", userName, "userName2", frndUserName));
            queryStatus = new DbQueryStatus(userName + " has now unfollowed " + frndUserName,
                DbQueryExecResult.QUERY_OK);
          }
        } else {
          // users not in db
          queryStatus = new DbQueryStatus("User not in DB",
              DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
        }
        trans.success();
        // check relationship created
      } catch (Exception e) {
        e.printStackTrace();
        queryStatus = new DbQueryStatus("Relationship not removed from DB",
            DbQueryExecResult.QUERY_ERROR_GENERIC);
      }
      session.close();
    }
    return queryStatus;
  }

  @Override
  public DbQueryStatus getAllSongFriendsLike(String userName) {
    Map<String, List<String>> idListsMap = new HashMap<>();
    List<String> songIds = new ArrayList<>();
    DbQueryStatus queryStatus;
    // check params
    if (userName == null || userName.isEmpty()) {
      return new DbQueryStatus("Missing Parameters", DbQueryExecResult.QUERY_ERROR_GENERIC);
    }

    // check user exists
    try (Session session = this.driver.session()) {
      try (Transaction trans = session.beginTransaction()) {
        // check user in db
        StatementResult result = trans.run(
            "MATCH (nProfile:profile {userName: $userName}) RETURN nProfile",
            parameters("userName", userName));
        if (!result.hasNext()) {
          return new DbQueryStatus("User doesn't exist in the DB",
              DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
        }

        // get all users followed by user
        result = trans
            .run("MATCH (nUser:profile {userName: $userName})-[r:follows]->(nFriend:profile) "
                    + " RETURN nFriend.userName AS name",
                parameters("userName", userName));
        if (!result.hasNext()) {
          queryStatus = new DbQueryStatus("User doesn't follow anyone",
              DbQueryExecResult.QUERY_OK);
          queryStatus.setData(idListsMap);
          return queryStatus;
        }
        // get friend names
//        friendNames.clear();
        while (result.hasNext()) {
          Record record = result.next();
          idListsMap.put(record.get("name").asString(), new ArrayList<>());
        }

        // get all songIds for all users followed by user
        for (String name : idListsMap.keySet()) {
          result = trans.run(
              "MATCH (nFriend:profile {userName: $userName})-[r:created]->(nPlaylist:playlist) "
                  + " MATCH (nPlaylist)-[s:includes]->(nSong:song) "
                  + " RETURN nSong.songId AS songId",
              parameters("userName", name));
          if (result.hasNext()) {
            // get songs
            songIds.clear();
            while (result.hasNext()) {
              Record record = result.next();
              songIds.add(record.get("songId").asString());
            }
            idListsMap.put(name, new ArrayList<>(songIds));
          }
        }
        trans.success();
      } catch (Exception e) {
        queryStatus = new DbQueryStatus("Something went wrong",
            DbQueryExecResult.QUERY_ERROR_GENERIC);
      }
      session.close();
    }
    queryStatus = new DbQueryStatus("All songs in friends' playlists returned",
        DbQueryExecResult.QUERY_OK);
    queryStatus.setData(idListsMap);
    return queryStatus;
  }

}
