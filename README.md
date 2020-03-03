# Neo4j and MongoDB Project

This project was authored by [Sakina Gadriwala](http://github.com/SakinaGadri/ "Github Profile") and [Seemin Syed](https://github.com/SeeminSyed/ "Github Profile")

## Overview:
The project has two microservices: profile and songs. The profiles data is stored in [Neo4j](https://neo4j.com/docs/driver-manual/current/ "Neo4j Documentation") and the songs data is stored in [MongoDB](https://docs.mongodb.com/manual/ "MongoDB Documentation").

## APIs

### Profile MicroService
#### Create
* description: Adds a new user profile to the database.
* request: `POST /profile?userName={userName}&fullName={fullName}&password={password}`
* response: 200
    * body: User added to DB
* response: 404
    * body: User already exist in the DB
* response: 500
    * body: Missing Parameters
    * body: User not added to DB
---

#### Read
* description: Gets the user's friends song lists.
* request: `GET /getAllFriendFavouriteSongTitles/{userName}`
    * `username` - the user who's friends to find
* response: 200
    * body: User doesn't follow anyone
    * body: All songs in friends' playlists returned
* response: 404
    * body: User doesn't exist in the DB
* response: 500
    * body: Connection to Songs getSongTitleById API unavailable
    * body: Missing Parameters
    * body: Something went wrong
---

#### Update
* description: Makes one user follow another in the database. (Only adds relationship from A to B, not B to A.)
* request: `PUT /followFriend/{userName}/{friendUserName}`
    * `username` - the userName of the profile who will follow
    * `friendUserName` - the userName of the profile to be followed
* response: 200
    * body: `userName` is now following `friendUserName`
* response: 404
    * body: User not in DB
* response: 500
    * body: Missing Parameters
    * body: Relationship already exists in the DB
    * body: Relationship not added to DB
---

* description: Makes one user unfollow another in the DB, assuming they already follow each other. (Only removed relationship from A to B, not B to A.)
* request: `PUT /unfollowFriend/{userName}/{friendUserName}`
    * `username` - the userName of the profile who will follow
    * `friendUserName` - the userName of the profile to be followed
* response: 200
    * body: `userName` has now unfollowed `friendUserName`
* response: 404
    * body: Relationship doesn't exist in the DB
    * body: User not in DB
* response: 500
    * body: Missing Parameters
    * body: User cannot unfollow themself
    * body: Relationship not removed from DB
---

* description: Likes song and adds it to the user's playlist.
* request: `PUT /likeSong/{userName}/{songId}`
    * `username` - username of profile who's playlist to add song to
    * `songId` - id of the song to add to the user's playlist
* response: 200
    * body: Song added to playlist
* response: 404
    * body: Song does not exist or could not be liked
* response: 500
    * body: Connection to Songs updateSongFavouritesCount API unavailable
---

* description: Unlikes song and removes it from the user's playlist, assuming the user already has it in their playlist.
* request: `PUT /unlikeSong/{userName}/{songId}`
    * `username` - username of profile who's playlist to remove song from
    * `songId` - id of the song to remove from the user's playlist
* response: 200
    * body: Song removed from playlist
* response: 404
    * body: Song does not exist or could not be unliked
* response: 500
    * body: Connection to Songs updateSongFavouritesCount API unavailable
---
#### Delete
* description: Removes a song from all playlists, deleting it from the database
* request: `PUT /deleteAllSongsFromDb/{songId}`
    * `songId` - songId for the song to remove
* response: 200
    * body: OK
* response: 500
    * body: Server side error
---
### Songs Microservice
#### Create
* description: Returns the response to the POST API for adding the song when given `songName`, `songArtistFullName` and `songAlbum`
* request: `POST /addSong?songName={songName}&songArtistFullName={songArtistFullName}&songAlbum={songAlbum}`
    * `songName` - name of the song
    * `songArtistFullName` - name of the artist of the song
    * `songAlbum` - name of the album to which the song belongs to
* response: 200
    * body: Song inserted successfully
* response: 500
    * body: Missing required parameters
    * body: Empty Song was passed in
    * body: Issue while inserting song
---
#### Read
* description: Returns the response to the GET API for getting the song when given an id
* request: `GET /getSongById/{songId}`
    * `songId` - The id for the song you want to get
* response: 200
    * body: OK
* response: 404
    * body: NOT FOUND
* response: 500
    * body: Empty id is passed in
    * body: Invalid ObjectId
---
* description: Returns the response to the GET API for getting the song title when given an id
* request: `GET /getSongTitleById/{songId}`
    * `songId` - The id for the song you want to get
* response: 200
    * body: OK
* response: 404
    * body: NOT FOUND
* response: 500
    * body: Empty id is passed in
    * body: Invalid ObjectId
---
#### Update
* description: Returns the response to the PUT API for updating the favourite count of the song when provided with the id
* request: `PUT /updateSongFavouritesCount/{songId}?shouldDecrement={true/false}`
    * `songId` - The id for the song you want to update
    * `shouldDecrement ` - set to true, if you want to decrease the favourite count. Otherwise, false
* response: 200
    * body: OK
* response: 404
    * body: Song not found
* response: 500
    * body: Missing required parameters
    * body: Empty id is passed in
    * body: Invalid ObjectId
    * body: Cannot unlike a song that you did not like
    * body: Issue while updating the favourite number
---
#### Delete
* description: Returns the response to the DELETE API for deleting the song provided the song id
* request: `DELETE /deleteSongById/{songId}`
    * `songId` - The id for the song you want to delete
* response: 200
    * body: OK
* response: 404
    * body: NOT FOUND
* response: 500
    * body: Empty id is passed in
    * body: Invalid ObjectId
---
## How to run the project?
* Download [Neo4j](https://neo4j.com/download/ "Download Link for Neo4j") and [MongoDB](https://www.mongodb.com/download-center/community "Download Link for MongoDB").
* The run `mvn compile` and `mvn exec:java`. 
* The Profile Service are accessible on port `3002`. The Songs Service is accessible on port `3001`.
* Use [`curl`](http://www.mit.edu/afs.new/sipb/user/ssen/src/curl-7.11.1/docs/curl.html "curl Documentation") or [Postman](https://www.postman.com/downloads/ "Download Postman") to interact with the APIs.
* Enjoy!😄 
