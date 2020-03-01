package com.csc301.profilemicroservice;

import static org.neo4j.driver.v1.Values.parameters;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.springframework.stereotype.Repository;

@Repository
public class PlaylistDriverImpl implements PlaylistDriver {

  Driver driver = ProfileMicroserviceApplication.driver;

  public static void InitPlaylistDb() {
    String queryStr;

    try (Session session = ProfileMicroserviceApplication.driver.session()) {
      try (Transaction trans = session.beginTransaction()) {
        queryStr = "CREATE CONSTRAINT ON (nPlaylist:playlist) ASSERT exists(nPlaylist.plName)";
        trans.run(queryStr);
        trans.success();
      }
      session.close();
    }
  }

  @Override
  public DbQueryStatus likeSong(String userName, String songId) {
    // add song and/or relationship to db
    DbQueryStatus queryStatus;
    // check params
    if (userName == null || songId == null || userName.isEmpty() || songId.isEmpty()) {
      return new DbQueryStatus("Missing Parameters", DbQueryExecResult.QUERY_ERROR_GENERIC);
    }

    try (Session session = this.driver.session()) {
      try (Transaction trans = session.beginTransaction()) {
        // check user exists
        StatementResult resultUser = trans.run(
            "MATCH (nUser:profile {userName: $userName}) RETURN nUser",
            parameters("userName", userName));
        if (!resultUser.hasNext()) { // both users in db
          return new DbQueryStatus("User doesn't exist in the DB",
              DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
        }

        StatementResult resultSong = trans.run(
            "MATCH (nPlaylist:playlist { plName: $plName }) "
                + "MERGE (nSong:song {songId: $songId}) "
                + " MERGE (nPlaylist)-[r:includes]->(nSong) "
                + " RETURN nPlaylist.plName, type(r), nSong.songId ",
            parameters("plName", userName + "-favorites", "songId", songId));
        if (!resultSong.hasNext()) { // both users in db
          queryStatus = new DbQueryStatus("Song couldn't be added to the playlist",
              DbQueryExecResult.QUERY_ERROR_GENERIC);
        } else {
          queryStatus = new DbQueryStatus("Song added to playlist",
              DbQueryExecResult.QUERY_OK);
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
  public DbQueryStatus unlikeSong(String userName, String songId) {
    // check song in db
    DbQueryStatus queryStatus;
    // check params
    if (userName == null || songId == null || userName.isEmpty() || songId.isEmpty()) {
      return new DbQueryStatus("Missing Parameters", DbQueryExecResult.QUERY_ERROR_GENERIC);
    }

    try (Session session = this.driver.session()) {
      try (Transaction trans = session.beginTransaction()) {

        // check user exists
        StatementResult resultUser = trans.run(
            "MATCH (nUser:profile {userName: $userName}) RETURN nUser",
            parameters("userName", userName));
        if (!resultUser.hasNext()) { // both users in db
          return new DbQueryStatus("User doesn't exist in the DB",
              DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
        }

        // check song exists
        StatementResult resultSong = trans.run(
            "MATCH (nSong:song {songId: $songId}) RETURN nSong",
            parameters("songId", songId));
        if (!resultSong.hasNext()) { // both users in db
          return new DbQueryStatus("Song doesn't exist in the DB",
              DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
        }

        // check relationship exists
        StatementResult resultRelationship = trans.run(
            "MATCH (nPlaylist:playlist { plName: $plName }) "
                + " MATCH (nSong:song {songId: $songId}) "
                + " RETURN EXISTS ((nPlaylist)-[:includes]->(nSong)) AS rl_exists",
            parameters("plName", userName + "-favorites", "songId", songId));
        if (!resultRelationship.next().get("rl_exists", false)) { // if relationship doesn't exist
          return new DbQueryStatus("Relationship doesn't exist in the DB",
              DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
        }

        // check relationship
        StatementResult result = trans.run(
            "MATCH (nPlaylist:playlist { plName: $plName }) "
                + " MATCH (nSong:song {songId: $songId}) "
                + " MATCH (nPlaylist)-[r:includes]->(nSong) "
                + " RETURN nPlaylist.plName, type(r), nSong.songId ",
            parameters("plName", userName + "-favorites", "songId", songId));

        // if exists, remove
        if (!result.hasNext()) { // both users in db
          queryStatus = new DbQueryStatus("Relationship already not DB",
              DbQueryExecResult.QUERY_ERROR_GENERIC);
        } else { // relationship does exist, remove it
          result = trans.run(
              "MATCH (nPlaylist:playlist { plName: $plName }) "
                  + " MATCH (nSong:song {songId: $songId}) "
                  + " MATCH (nPlaylist)-[r:includes]->(nSong) "
                  + " DELETE r",
              parameters("plName", userName + "-favorites", "songId", songId));
          queryStatus = new DbQueryStatus("Song removed from playlist",
              DbQueryExecResult.QUERY_OK);
        }

        // check if song has no connections
        result = trans.run(
            "MATCH (nSong:song {songId: $songId}) "
                + " MATCH (nSong) WHERE NOT ()-[:includes]->(nSong) "
                + " DELETE (nSong)",
            parameters("songId", songId));

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
  public DbQueryStatus deleteSongFromDb(String songId) {
    DbQueryStatus queryStatus = new DbQueryStatus("Song removed from DB",
        DbQueryExecResult.QUERY_OK);
    // check params
    if (songId == null || songId.isEmpty()) {
      return new DbQueryStatus("Missing Parameters", DbQueryExecResult.QUERY_ERROR_GENERIC);
    }

    try (Session session = this.driver.session()) {
      try (Transaction trans = session.beginTransaction()) {

        // check song exists
        StatementResult resultSong = trans.run(
            "MATCH (nSong:song {songId: $songId}) RETURN nSong",
            parameters("songId", songId));
        if (!resultSong.hasNext()) { // both users in db
          return new DbQueryStatus("Song doesn't exist in the DB",
              DbQueryExecResult.QUERY_OK);
        }

        // remove song and relationships
        StatementResult result = trans.run(
            "MATCH (nSong:song {songId: $songId}) "
                + " DETACH DELETE (nSong)",
            parameters("songId", songId));

        trans.success();
        // check relationship created
      } catch (Exception e) {
        e.printStackTrace();
        queryStatus = new DbQueryStatus("Song not removed from DB",
            DbQueryExecResult.QUERY_ERROR_GENERIC);
      }
      session.close();
    }
    return queryStatus;
  }
}
