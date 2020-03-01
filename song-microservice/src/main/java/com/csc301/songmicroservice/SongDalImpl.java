package com.csc301.songmicroservice;

import com.mongodb.client.result.DeleteResult;
import java.util.List;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

/**
 * The Data Access Layer that communicates with the database
 */
@Repository
public class SongDalImpl implements SongDal {

  private final MongoTemplate db;

  /**
   * Constructor fot the Song Data Access Layer
   *
   * @param mongoTemplate Access to the Mongo Database
   */
  @Autowired
  public SongDalImpl(MongoTemplate mongoTemplate) {
    this.db = mongoTemplate;
  }

  /**
   * Adds the song to the database
   *
   * @param songToAdd the song you want to add to the database
   */
  @Override
  public DbQueryStatus addSong(Song songToAdd) {
    // check if song has data
    if (songToAdd.getSongName().isEmpty() || songToAdd.getSongArtistFullName().isEmpty()
        || songToAdd.getSongAlbum().isEmpty()) {
      return new DbQueryStatus("Empty Song was passed in", DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
    // insert into the table
    Song insertedSong = db.insert(songToAdd, "songs");
    System.out.println("insertedSong: " + insertedSong);
    if (insertedSong.getJsonRepresentation().isEmpty()) {
      return new DbQueryStatus("Issue while inserting song", DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
    DbQueryStatus response = new DbQueryStatus("Song inserted successfully",
        DbQueryExecResult.QUERY_OK);
    response.setData(insertedSong);
    return response;
  }

  /**
   * Retrieves the song from the database
   *
   * @param songId the song you want to retrieve from the database
   */
  @Override
  public DbQueryStatus findSongById(String songId) {
    // check if song id is empty
    if (songId.isEmpty()) {
      return new DbQueryStatus("Empty id is passed in", DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
    ObjectId _id = null;
    try {
      _id = new ObjectId(songId);
    } catch (Exception e) {
      System.out.println("invalid object id");
      return new DbQueryStatus("Invalid ObjectId", DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
    Query query = new Query();
    query.addCriteria(Criteria.where("_id").is(_id));
    List<Song> songs = db.find(query, Song.class);
    System.out.println(songs.toString());
    // if there is no data from the database => 404
    if (songs.isEmpty()) {
      System.out.println("songs is empty");
      return new DbQueryStatus("NOT FOUND", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
    }
    // there is data => 200 and set the data in the response
    System.out.println("success!");
    DbQueryStatus response = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
    response.setData(songs);
    return response;
  }

  /**
   * Retrieves the song title from the database
   *
   * @param songId the song title you want to retrieve from the database
   */
  @Override
  public DbQueryStatus getSongTitleById(String songId) {
    // check if song id is empty
    if (songId.isEmpty()) {
      return new DbQueryStatus("Empty id is passed in", DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
    ObjectId _id = null;
    try {
      _id = new ObjectId(songId);
    } catch (Exception e) {
      // ObjId was invalid => 500
      System.out.println("invalid object id");
      return new DbQueryStatus("Invalid ObjectId", DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
    Query query = new Query();
    query.addCriteria(Criteria.where("_id").is(_id));
    List<Song> songs = db.find(query, Song.class);
    // if there is no data from the database => 404
    if (songs.isEmpty()) {
      System.out.println("songs are empty");
      return new DbQueryStatus("NOT FOUND", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
    }
    // there is data => 200 and set the data in the response
    System.out.println("song name:" + songs.get(0).getSongName());
    System.out.println("success! ");
    DbQueryStatus response = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
    response.setData(songs.get(0).getSongName());
    return response;
  }

  /**
   * Deletes the song from the database
   *
   * @param songId the song you want to delete from the database
   */
  @Override
  public DbQueryStatus deleteSongById(String songId) {
    // check if song id is empty
    if (songId.isEmpty()) {
      return new DbQueryStatus("Empty id is passed in", DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
    ObjectId _id = null;
    try {
      _id = new ObjectId(songId);
    } catch (Exception e) {
      // ObjId was invalid => 500
      System.out.println("invalid object id");
      return new DbQueryStatus("Invalid ObjectId", DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
    Query query = new Query();
    query.addCriteria(Criteria.where("_id").is(_id));
    // delete code here
    DeleteResult res = db.remove(query, Song.class);
    if (res.getDeletedCount() != 1) {
      System.out.println("could not delete");
      return new DbQueryStatus("NOT FOUND", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
    }
    System.out.println("success!");
    DbQueryStatus response = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
    return response;
  }

  /**
   * Updates the song favourite count in the database
   *
   * @param songId          the song you want to update in the database
   * @param shouldDecrement true if user wants to decrement the song favourite count. Otherwise,
   *                        false
   */
  @Override
  public DbQueryStatus updateSongFavouritesCount(String songId, boolean shouldDecrement) {
    // check if song id is empty
    if (songId.isEmpty()) {
      return new DbQueryStatus("Empty id is passed in", DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
    ObjectId _id = null;
    // convert it to an objID
    try {
      _id = new ObjectId(songId);
    } catch (Exception e) {
      // ObjId was invalid => 500
      System.out.println("invalid object id");
      return new DbQueryStatus("Invalid ObjectId", DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
    // find the song obj corresponding to the id
    Query query = new Query();
    query.addCriteria(Criteria.where("_id").is(_id));
    Song song = db.findOne(query, Song.class);
    // if there is no data from the database => 404
    if (song == null) {
      System.out.println("song is empty");
      return new DbQueryStatus("Song not found", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
    }
    // if shouldDecrement is true and songAmountFavourites is > 0 => decrease
    long favourites = song.getSongAmountFavourites();
    if (shouldDecrement && favourites > 0) {
      song.setSongAmountFavourites(favourites - 1);
    }
    // if shouldDecrement is false => increase
    else if (shouldDecrement == false) {
      song.setSongAmountFavourites(favourites + 1);
    }
    // otherwise there was an issue with favourites
    else {
      System.out.println("could not update fave num because < 0");
      return new DbQueryStatus("Cannot unlike a song that you did not like",
          DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
    // update the database with the new like value
    Song updatedSong = db.save(song, "songs");
    if (updatedSong.getSongAmountFavourites() == favourites || updatedSong.getJsonRepresentation()
        .isEmpty()) {
      System.out.println("could not update the fav num");
      return new DbQueryStatus("Issue while updating the favourite number",
          DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
    System.out.println("success!");
    return new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
  }
}
