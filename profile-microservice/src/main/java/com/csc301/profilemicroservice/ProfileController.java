package com.csc301.profilemicroservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestController
@RequestMapping("/")
public class ProfileController {

  public static final String KEY_USER_NAME = "userName";
  public static final String KEY_USER_FULLNAME = "fullName";
  public static final String KEY_USER_PASSWORD = "password";

  @Autowired
  private final ProfileDriverImpl profileDriver;

  @Autowired
  private final PlaylistDriverImpl playlistDriver;

  OkHttpClient client = new OkHttpClient();

  public ProfileController(ProfileDriverImpl profileDriver, PlaylistDriverImpl playlistDriver) {
    this.profileDriver = profileDriver;
    this.playlistDriver = playlistDriver;
  }

  /**
   * POST: Adds a new user profile to the database.
   * <p>
   * When creating the new user profile, it also creates a playlist named 'username'-favorites with
   * username being the profile username.
   *
   * @param params  string 'userName', 'fullName' and 'password' as profile?userName={{userName}}&fullName={{fullName}}&password={{password}}
   * @param request the request sent to this API
   * @return "status":"OK" if request was successfully
   */
  @RequestMapping(value = "/profile", method = RequestMethod.POST)
  public @ResponseBody
  Map<String, Object> addProfile(@RequestParam Map<String, String> params,
      HttpServletRequest request) {

    Map<String, Object> response = new HashMap<String, Object>();
    response.put("path", String.format("POST %s", Utils.getUrl(request)));

    String userName = params.get("userName");
    String fullName = params.get("fullName");
    String password = params.get("password");

    // call DbQueryStatus = createUserProfile(userName, fullName, password)
    DbQueryStatus dbQueryStatus = profileDriver.createUserProfile(userName, fullName, password);
    response.put("message", dbQueryStatus.getMessage());
    // call setResponseStatus(Map<String, Object> response, DbQueryExecResult dbQueryExecResult, Object data)
    return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), null);
  }

  /**
   * PUT: Makes one user follow another in the DB.
   * <p>
   * Only adds relationship from A to B, not B to A.
   *
   * @param userName       the userName of the profile who will follow
   * @param friendUserName the userName of the profile to be followed
   * @param request        the request sent to this API
   * @return "status":"OK" if request was successfully
   */
  @RequestMapping(value = "/followFriend/{userName}/{friendUserName}", method = RequestMethod.PUT)
  public @ResponseBody
  Map<String, Object> followFriend(@PathVariable("userName") String userName,
      @PathVariable("friendUserName") String friendUserName, HttpServletRequest request) {

    Map<String, Object> response = new HashMap<String, Object>();
    response.put("path", String.format("PUT %s", Utils.getUrl(request)));

    // call DbQueryStatus = profileDriver.followFriend(userName, friendUserName);
    DbQueryStatus dbQueryStatus = profileDriver.followFriend(userName, friendUserName);
    response.put("message", dbQueryStatus.getMessage());
    // call setResponseStatus(Map<String, Object> response, DbQueryExecResult dbQueryExecResult, Object data)
    return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), null);
  }

  /**
   * PUT: Makes one user unfollow another in the DB, assuming they already follow each other.
   * <p>
   * Only removed relationship from A to B, not B to A.
   *
   * @param userName       the userName of the profile who will unfollow
   * @param friendUserName the userName of the profile to be unfollowed
   * @param request        the request sent to this API
   * @return "status":"OK" if request was successfully
   */
  @RequestMapping(value = "/unfollowFriend/{userName}/{friendUserName}", method = RequestMethod.PUT)
  public @ResponseBody
  Map<String, Object> unfollowFriend(@PathVariable("userName") String userName,
      @PathVariable("friendUserName") String friendUserName, HttpServletRequest request) {

    Map<String, Object> response = new HashMap<String, Object>();
    response.put("path", String.format("PUT %s", Utils.getUrl(request)));

    // call DbQueryStatus = profileDriver.unfollowFriend(userName, friendUserName);
    DbQueryStatus dbQueryStatus = profileDriver.unfollowFriend(userName, friendUserName);
    response.put("message", dbQueryStatus.getMessage());
    // call setResponseStatus(Map<String, Object> response, DbQueryExecResult dbQueryExecResult, Object data)
    return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), null);
  }

  /**
   * PUT: Likes song and adds it to the user's playlist.
   * <p>
   * Calls the Songs Microservice to change the number of likes the song has.
   *
   * @param userName username of profile who's playlist to add song to
   * @param songId   id of the song to add to the user's playlist
   * @param request  the request sent to this API
   * @return "status":"OK" if request was successfully
   */
  @RequestMapping(value = "/likeSong/{userName}/{songId}", method = RequestMethod.PUT)
  public @ResponseBody
  Map likeSong(@PathVariable("userName") String userName,
      @PathVariable("songId") String songId, HttpServletRequest request) {
    DbQueryStatus dbQueryStatus = new DbQueryStatus("Song added to playlist",
        DbQueryExecResult.QUERY_OK);

    Map<String, Object> response = new HashMap<String, Object>();
    response.put("path", String.format("PUT %s", Utils.getUrl(request)));

    dbQueryStatus = playlistDriver.likeSong(userName, songId);
    if (dbQueryStatus.getdbQueryExecResult() == DbQueryExecResult.QUERY_OK
        && dbQueryStatus.getMessage().compareTo("Relationship already exist in the DB") != 0) {
      try {
        if (callDecrementSongAPI(songId, false)) { // if true, song liked
          dbQueryStatus.setMessage("Song added to playlist");
        } else { // song doesnt exist or issue when liking
          dbQueryStatus.setMessage("Song does not exist or could not be liked");
        }
      } catch (RestClientException connectionUnavailable) {
        dbQueryStatus = new DbQueryStatus(
            "Connection to Songs updateSongFavouritesCount API unavailable",
            DbQueryExecResult.QUERY_ERROR_GENERIC);
        response.put("message", dbQueryStatus.getMessage());
      }
    }
    response.put("message", dbQueryStatus.getMessage());
    return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), null);

  }

  /**
   * PUT: Unlikes song and removes it from the user's playlist, assuming the user already has it in
   * their playlist.
   * <p>
   * Calls the Songs Microservice to change the number of likes the song has.
   *
   * @param userName username of profile who's playlist to remove song from
   * @param songId   id of the song to remove from the user's playlist
   * @param request  the request sent to this API
   * @return "status":"OK" if request was successfully
   */
  @RequestMapping(value = "/unlikeSong/{userName}/{songId}", method = RequestMethod.PUT)
  public @ResponseBody
  Map<String, Object> unlikeSong(@PathVariable("userName") String userName,
      @PathVariable("songId") String songId, HttpServletRequest request) {
    DbQueryStatus dbQueryStatus = new DbQueryStatus("Default",
        DbQueryExecResult.QUERY_OK);

    Map<String, Object> response = new HashMap<String, Object>();
    response.put("path", String.format("PUT %s", Utils.getUrl(request)));
    try {
      if (callDecrementSongAPI(songId, true)) { // if true, song exists and unliked
        // insert into db
        dbQueryStatus = playlistDriver.unlikeSong(userName, songId);
        response.put("message", dbQueryStatus.getMessage());
      } else { // song doesnt exist or issue when unliking
        response.put("message", "Song does not exist or could not be unliked");
      }
    } catch (RestClientException connectionUnavailable) {
      dbQueryStatus = new DbQueryStatus(
          "Connection to Songs updateSongFavouritesCount API unavailable",
          DbQueryExecResult.QUERY_ERROR_GENERIC);
      response.put("message", dbQueryStatus.getMessage());
    }

    dbQueryStatus = playlistDriver.unlikeSong(userName, songId);
    if (dbQueryStatus.getdbQueryExecResult() == DbQueryExecResult.QUERY_OK) {
      try {
        if (callDecrementSongAPI(songId, false)) { // if true, song liked
          dbQueryStatus.setMessage("Song removed from playlist");
        } else { // song doesnt exist or issue when liking
          dbQueryStatus.setMessage("Song does not exist or could not be unliked");
        }
      } catch (RestClientException connectionUnavailable) {
        dbQueryStatus = new DbQueryStatus(
            "Connection to Songs updateSongFavouritesCount API unavailable",
            DbQueryExecResult.QUERY_ERROR_GENERIC);
        response.put("message", dbQueryStatus.getMessage());
      }
    }
    response.put("message", dbQueryStatus.getMessage());
    return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), null);

  }

  /**
   * GET: Gets the user's friends song lists.
   * <p>
   * Calls the Songs Microservice to get the titles of each song.
   *
   * @param userName the user who's friends to find
   * @param request  the request sent to this API
   * @return "status":"OK" if request was successfully
   */
  @RequestMapping(value = "/getAllFriendFavouriteSongTitles/{userName}", method = RequestMethod.GET)
  public @ResponseBody
  Map<String, Object> getAllFriendFavouriteSongTitles(@PathVariable("userName") String userName,
      HttpServletRequest request) {

    Map<String, Object> response = new HashMap<String, Object>();
    response.put("path", String.format("PUT %s", Utils.getUrl(request)));

    DbQueryStatus dbQueryStatus = profileDriver.getAllSongFriendsLike(userName);
    if (dbQueryStatus.getdbQueryExecResult() != DbQueryExecResult.QUERY_OK) {
      dbQueryStatus.setData(null);
    } else {
      try {
        dbQueryStatus
            .setData(getAllSongTitlesAPI((Map<String, List<String>>) dbQueryStatus.getData()));
      } catch (RestClientException connectionUnavailable) {
        dbQueryStatus = new DbQueryStatus("Connection to Songs getSongTitleById API unavailable",
            DbQueryExecResult.QUERY_ERROR_GENERIC);
      }
    }
    response.put("message", dbQueryStatus.getMessage());
    return Utils
        .setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
  }

  /**
   * PUT: Removes a song from all playlists, deleting it from the DB.
   * <p>
   * The request is considered successful if the songId is not found in the DB.
   *
   * @param songId  songId for the song to remove
   * @param request the request sent to this API
   * @return "status":"OK" if request was successfully
   */
  @RequestMapping(value = "/deleteAllSongsFromDb/{songId}", method = RequestMethod.PUT)
  public @ResponseBody
  Map<String, Object> deleteAllSongsFromDb(@PathVariable("songId") String songId,
      HttpServletRequest request) {

    Map<String, Object> response = new HashMap<String, Object>();
    response.put("path", String.format("PUT %s", Utils.getUrl(request)));

    DbQueryStatus dbQueryStatus = playlistDriver.deleteSongFromDb(songId);
    response.put("message", dbQueryStatus.getMessage());

    return Utils
        .setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), null);
  }

  private boolean callDecrementSongAPI(String songId, boolean decrement)
      throws RestClientException {
    RestTemplate restTemplate = new RestTemplate();

    final String uri = "http://localhost:3001/updateSongFavouritesCount/{songId}?shouldDecrement={decrement}";
    // add pathVariables
    Map<String, String> uriVariables = new HashMap<>();
    uriVariables.put("songId", songId);
    uriVariables.put("decrement", String.valueOf(decrement));

    // creates object type to use as return type in exchange
    ParameterizedTypeReference<Map<String, Object>> typeRef =
        new ParameterizedTypeReference<Map<String, Object>>() {
        };
    // requestEntity null because our parameters are PathVariables, put in as uriVariables
    // if not we use
    //    HttpHeaders headers = new HttpHeaders();
    //    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
    //    HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);
    ResponseEntity<Map<String, Object>> result = restTemplate
        .exchange(uri, HttpMethod.PUT, null, typeRef, uriVariables);
    return (result.getBody().get("status").toString().compareTo("OK") == 0);
  }

  private Map<String, List<String>> getAllSongTitlesAPI(Map<String, List<String>> idLists)
      throws RestClientException {
    Map<String, List<String>> titleLists = new HashMap<>();
    // loop through users and add to map
    for (String user : idLists.keySet()) {
      List<String> songIds = idLists.get(user);
      List<String> songTitles = new ArrayList<>();
      // loop through songIds and add title to list
      for (String songId : songIds) {
        String title = getSongTitlesAPI(songId);
        if (title != null) {
          songTitles.add(getSongTitlesAPI(songId));
        }
      }
      titleLists.put(user, songTitles);
    }
    return titleLists;
  }

  private String getSongTitlesAPI(String songId) throws RestClientException {
    RestTemplate restTemplate = new RestTemplate();
    final String uri = "http://localhost:3001/getSongTitleById/{songId}";
    // add pathVariables
    Map<String, String> uriVariables = new HashMap<>();
    uriVariables.put("songId", songId);

    // creates object type to use as return type in exchange
    ParameterizedTypeReference<Map<String, Object>> typeRef =
        new ParameterizedTypeReference<Map<String, Object>>() {
        };
    ResponseEntity<Map<String, Object>> result = restTemplate
        .exchange(uri, HttpMethod.GET, null, typeRef, uriVariables);
    // { "status" : "OK", "data" : "Never going to give you up" }
    return (result.getBody().get("status").toString().compareTo("OK") == 0 ? result.getBody()
        .get("data").toString() : null);

  }

  @ControllerAdvice
  public class GlobalExceptionController {

    @ExceptionHandler(NoHandlerFoundException.class)
    public @ResponseBody
    Map<String, Object> handleError404(HttpServletRequest request, Exception e) {
      DbQueryStatus dbQueryStatus = new DbQueryStatus("No handler found for path",
          DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
      Map<String, Object> response = new HashMap<String, Object>();
      response.put("path", String.format("POST %s", Utils.getUrl(request)));
      response.put("message", dbQueryStatus.getMessage());
      return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), null);
    }
  }

}
