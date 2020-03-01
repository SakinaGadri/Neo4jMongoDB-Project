package com.csc301.songmicroservice;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/")
/**
 * Song Controller is where the microservice routes are placed This is the class
 * that will be receiving requests and returning response to Clients
 *
 * @author Sakina Gadriwala
 */
public class SongController {

	/**
	 * The Data access layer that communicates to the database
	 */
	@Autowired
	private final SongDal songDal;

	/**
	 * Constructor for the SongController
	 *
	 * @param songDal the data access layer
	 */
	public SongController(SongDal songDal) {
		this.songDal = songDal;
	}

	/**
	 * Returns the response to the GET API for getting the song when given an id
	 *
	 * @param songId  The id for the song you want to get
	 * @param request provide request information for HTTP servlets
	 * @return "status":"OK" if request was successfully
	 */
	@RequestMapping(value = "/getSongById/{songId}", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> getSongById(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("GET %s", Utils.getUrl(request)));
		// call the DAL object and return the response
		DbQueryStatus dbQueryStatus = songDal.findSongById(songId);

		System.out.println("dbquery: " + dbQueryStatus.toString());
		response.put("message", dbQueryStatus.getMessage());
		response = Utils
				.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		return response;
	}

	/**
	 * Returns the response to the GET API for getting the song title when given an id
	 *
	 * @param songId  The id for the song title you want to get
	 * @param request provide request information for HTTP servlets
	 * @return "status":"OK" if request was successfully
	 */
	@RequestMapping(value = "/getSongTitleById/{songId}", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> getSongTitleById(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		songDal.getSongTitleById(songId);
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("GET %s", Utils.getUrl(request)));
		// call the database
		DbQueryStatus dbQueryStatus = songDal.getSongTitleById(songId);

		response.put("message", dbQueryStatus.getMessage());
		response = Utils
				.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());

		return response;
	}

	/**
	 * Returns the response to the DELETE API for deleting the song provided the song id
	 *
	 * @param songId  The id for the song you want to delete
	 * @param request provide request information for HTTP servlets
	 * @return "status":"OK" if request was successfully
	 */
	@RequestMapping(value = "/deleteSongById/{songId}", method = RequestMethod.DELETE)
	@ResponseBody
	public Map<String, Object> deleteSongById(@PathVariable("songId") String songId,
			HttpServletRequest request) {
		DbQueryStatus dbQueryStatus = new DbQueryStatus("Default", DbQueryExecResult.QUERY_OK);
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("DELETE %s", Utils.getUrl(request)));

		// delete from the database
		if (callDeleteAllSongsFromDB(songId)) { // if true, song exists and is deleted
			// delete from the database
			dbQueryStatus = songDal.deleteSongById(songId);
			if (dbQueryStatus.getdbQueryExecResult() != DbQueryExecResult.QUERY_OK) {
				// not deleted
				response.put("message", dbQueryStatus.getMessage());
			}
		} else { // song doesnt exist or issue when deleting
			response.put("message", "Song does not exist or could not be deleted");
			dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
		System.out.println("message we're returning: " + response.get("message"));
		return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), null);
	}

	/**
	 * Returns the response to the POST API for adding the song when given "songName",
	 * "songArtistFullName" and "songAlbum"
	 *
	 * @param params  contains "songName", "songArtistFullName" and "songAlbum" that the user wants to
	 *                add to the database
	 * @param request provide request information for HTTP servlets
	 * @return "status":"OK" if request was successfully
	 */
	@RequestMapping(value = "/addSong", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> addSong(@RequestParam Map<String, String> params,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("POST %s", Utils.getUrl(request)));
		System.out.println("params" + params);
		// create a song obj and try and enter it into the database
		if (!params.containsKey("songName") || !params.containsKey("songArtistFullName") || !params
				.containsKey("songAlbum")) {
			response.put("message", "Missing required parameters");
			response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
			return response;
		}
		Song toInsert = new Song(params.get("songName"), params.get("songArtistFullName"),
				params.get("songAlbum"));
		DbQueryStatus dbQueryStatus = songDal.addSong(toInsert);

		response.put("message", dbQueryStatus.getMessage());
		response = Utils
				.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		return response;
	}

	/**
	 * Returns the response to the PUT API for updating the favourite count of the song when provided
	 * with the id
	 *
	 * @param songId          The id for the song you want to update
	 * @param shouldDecrement set to true, if you want to decrease the favourite count. Otherwise,
	 *                        false
	 * @param request         provide request information for HTTP servlets
	 * @return "status":"OK" if request was successfully
	 */
	@RequestMapping(value = "/updateSongFavouritesCount/{songId}", method = RequestMethod.PUT)
	@ResponseBody
	public Map<String, Object> updateFavouritesCount(@PathVariable("songId") String songId,
			@RequestParam("shouldDecrement") String shouldDecrement, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("data", String.format("PUT %s", Utils.getUrl(request)));
		System.out.println(shouldDecrement);
		System.out.println(songId);
		if (songId.isEmpty() || shouldDecrement.isEmpty()) {
			response.put("message", "Missing required parameters");
			response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
			return response;
		}
		// make call to the database
		DbQueryStatus dbQueryStatus = songDal
				.updateSongFavouritesCount(songId, Boolean.valueOf(shouldDecrement));

		response.put("message", dbQueryStatus.getMessage());
		response = Utils
				.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		return response;
	}

	/**
	 * Returns true if songId was successfully deleted from the profile database. Otherwise, returns
	 * false
	 *
	 * @param songId The id for the song you want to delete
	 * @return boolean if the song was successfully deleted
	 */
	private boolean callDeleteAllSongsFromDB(String songId) {
		RestTemplate restTemplate = new RestTemplate();
		final String uri = "http://localhost:3002/deleteAllSongsFromDb/{songId}";
		// add pathVariables
		Map<String, String> uriVariables = new HashMap<>();
		uriVariables.put("songId", songId);
		// creates object type to use as return type in exchange
		ParameterizedTypeReference<Map<String, Object>> typeRef = new ParameterizedTypeReference<Map<String, Object>>() {
		};
		ResponseEntity<Map<String, Object>> result = restTemplate
				.exchange(uri, HttpMethod.PUT, null, typeRef,
						uriVariables);
		return (result.getBody().get("status").toString().compareTo("OK") == 0);
		// return false;
	}
}