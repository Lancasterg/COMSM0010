package io.openvidu.js.java;

/*
 *  This code is made available by the Apache LICENSE file in this project
 *
 *  The original project this code was used from can be found at:
 *  https://github.com/OpenVidu/openvidu-tutorials/blob/master/openvidu-js-java/
 *
 *  Modified by George Lancaster
 */

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpSession;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CopyOnWriteArrayList;

import io.openvidu.java.client.OpenVidu;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.java.client.Session;
import io.openvidu.java.client.TokenOptions;

@RestController
@RequestMapping("/api-sessions")
public class SessionController {

	// OpenVidu object as entrypoint of the SDK
	private OpenVidu openVidu;

	// Collection to pair session names and OpenVidu Session objects
	private Map<String, Session> mapSessions = new ConcurrentHashMap<>();
	private Map<String, Map<String, OpenViduRole>> mapSessionNamesTokens = new ConcurrentHashMap<>();
	private CopyOnWriteArrayList<String> uniqueSessions = new CopyOnWriteArrayList<>();

	// URL where our OpenVidu server is listening
	private String OPENVIDU_URL;
	// Secret shared with our OpenVidu server
	private String SECRET;

	public SessionController(@Value("${openvidu.secret}") String secret, @Value("${openvidu.url}") String openviduUrl) {
		this.SECRET = secret;
		this.OPENVIDU_URL = openviduUrl;
		this.openVidu = new OpenVidu(OPENVIDU_URL, SECRET);
	}

	/**
	 * Index page
	 * @param itemid
	 * @return
	 */
	@RequestMapping(value="/", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<HttpStatus> getitem(@RequestParam("data") String itemid){
		return new ResponseEntity<>(HttpStatus.OK);
	}


	/**
	 * Get a token from the OpenVidu server
	 * If the session is new, create a new session else return token for existing session.
	 *
	 * @param sessionNameParam JSON of the session name
	 * @param httpSession the current http session
	 * @return
	 * @throws ParseException
	 */
	@RequestMapping(value = "/get-token", method = RequestMethod.POST)
	public ResponseEntity<JSONObject> getToken(@RequestBody String sessionNameParam, HttpSession httpSession)
			throws ParseException {

		System.out.println("Getting a token from OpenVidu Server | {sessionName}=" + sessionNameParam);
		JSONObject toSessionName = (JSONObject) new JSONParser().parse(sessionNameParam);
		String sessionName = (String) toSessionName.get("sessionName");

        System.out.println(sessionName);
		// Role associated to this user
		OpenViduRole role = OpenViduRole.PUBLISHER;

		String serverData = "{\"serverData\": \"" + httpSession.getAttribute("loggedUser") + "\"}";

		// Build tokenOptions object with the serverData and the role
		TokenOptions tokenOptions = new TokenOptions.Builder().data(serverData).role(role).build();

		JSONObject responseJson = new JSONObject();

		if (this.mapSessions.get(sessionName) != null && !sessionName.equals("default")) {
			// Session already exists
			System.out.println("Existing session " + sessionName);
			try {

				// Generate a new token with the recently created tokenOptions
				String token = this.mapSessions.get(sessionName).generateToken(tokenOptions);

				// Update our collection storing the new token
				this.mapSessionNamesTokens.get(sessionName).put(token, role);

				// Prepare the response with the token
				responseJson.put(0, token);
				responseJson.put(1,sessionName);

				// Return the response to the client
				return new ResponseEntity<>(responseJson, HttpStatus.OK);

			} catch (Exception e) {
				// If error generate an error message and return it to client
				return getErrorResponse(e);
			}

		} else {
			// New session
			System.out.println("New session " + sessionName);
			try {
				// Create a new OpenVidu Session
				Session session = this.openVidu.createSession();
				// Generate a new token with the recently created tokenOptions
				String token = session.generateToken(tokenOptions);

				// Store the session and the token in our collections
				this.mapSessions.put(sessionName, session);
				this.mapSessionNamesTokens.put(sessionName, new ConcurrentHashMap<>());
				this.mapSessionNamesTokens.get(sessionName).put(token, role);

				// Prepare the response with the token
				responseJson.put(0, token);
				responseJson.put(1, sessionName);

				// Return the response to the client
				return new ResponseEntity<>(responseJson, HttpStatus.OK);

			} catch (Exception e) {
				// If error generate an error message and return it to client
				return getErrorResponse(e);
			}
		}
	}

	/**
	 * Remove a user from the session
	 * @param sessionNameToken token for current session
	 * @param httpSession the current http session
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/remove-user", method = RequestMethod.POST)
	public ResponseEntity<JSONObject> removeUser(@RequestBody String sessionNameToken, HttpSession httpSession)
			throws Exception {

		try {
			checkUserLogged(httpSession);
		} catch (Exception e) {
			return getErrorResponse(e);
		}
		System.out.println("Removing user | {sessionName, token}=" + sessionNameToken);

		// Retrieve the params from BODY
		JSONObject sessionNameTokenJSON = (JSONObject) new JSONParser().parse(sessionNameToken);
		String sessionName = (String) sessionNameTokenJSON.get("sessionName");
		String token = (String) sessionNameTokenJSON.get("token");

		// If the session exists
		if (this.mapSessions.get(sessionName) != null && this.mapSessionNamesTokens.get(sessionName) != null) {

			// If the token exists
			if (this.mapSessionNamesTokens.get(sessionName).remove(token) != null) {
				// User left the session
				if (this.mapSessionNamesTokens.get(sessionName).isEmpty()) {
					// Last user left: session must be removed
					this.mapSessions.remove(sessionName);
				}
				return new ResponseEntity<>(HttpStatus.OK);
			} else {
				// The TOKEN wasn't valid
				System.out.println("Problems in the app server: the TOKEN wasn't valid");
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}

		} else {
			// The SESSION does not exist
			System.out.println("Problems in the app server: the SESSION does not exist");
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Return internal error
	 * @param e internal server error
	 * @return
	 */
	private ResponseEntity<JSONObject> getErrorResponse(Exception e) {
		JSONObject json = new JSONObject();
		json.put("cause", e.getCause());
		json.put("error", e.getMessage());
		json.put("exception", e.getClass());
		return new ResponseEntity<>(json, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	/**
	 * Check if the user is logged in
	 *
	 * @param httpSession
	 * @throws Exception
	 */
	private void checkUserLogged(HttpSession httpSession) throws Exception {
		if (httpSession == null || httpSession.getAttribute("loggedUser") == null) {
			throw new Exception("User not logged");
		}
	}


}
