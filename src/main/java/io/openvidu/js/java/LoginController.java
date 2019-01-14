package io.openvidu.js.java;
/*
 *  This code is made available by the Apache LICENSE file in this project
 *
 *  The original project this code was used from can be found at:
 *  https://github.com/OpenVidu/openvidu-tutorials/blob/master/openvidu-js-java/
 *
 *  Modified by George Lancaster
 */

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api-login")
public class LoginController {

    private int numGuests = 0;
    private AwsAuth auth = new AwsAuth();


    /**
     * Login a user as a guest
     * @param userPass: Not used
     * @param httpSession: The current httpSession
     * @return response 200
     * @throws ParseException
     */
    @RequestMapping(value = "/login-guest", method = RequestMethod.POST)
    public ResponseEntity<Object> loginGuest(@RequestBody String userPass, HttpSession httpSession) throws ParseException {
        numGuests += 1;
        String user = String.format("Guest%d", numGuests);
        httpSession.setAttribute("loggedUser", user);
        JSONObject responseJson = new JSONObject();
        responseJson.put(0, user);
        return new ResponseEntity<>(responseJson, HttpStatus.OK);
    }

    /**
     * Log user out of the session
     * @param session
     * @return
     */
    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    public ResponseEntity<Object> logout(HttpSession session) {
        System.out.println("'" + session.getAttribute("loggedUser") + "' has logged out");
        session.invalidate();
        return new ResponseEntity<>(HttpStatus.OK);
    }


    /**
     * Login using AWS Cognito
     * @param userPass: JSON object containing username and password
     * @param httpSession: The current http session
     * @return JSON object with users name if successful
     * @throws ParseException
     */
    @RequestMapping(value = "/aws-login", method = RequestMethod.POST)
    public ResponseEntity<Object> awsLogin(@RequestBody String userPass, HttpSession httpSession) throws ParseException {
        JSONObject userPassJson = (JSONObject) new JSONParser().parse(userPass);
        System.out.println("Logging in | {user, pass}=" + userPass);
        String user = (String) userPassJson.get("user");
        String pass = (String) userPassJson.get("pass");
        String result = auth.ValidateUser(user, pass);
        JSONObject responseJson = new JSONObject();
        System.out.print(result);
        if (result.equals(Constants.ERR_EXIST)) {
            responseJson.put(0, Constants.ERR_EXIST_MSG);
        }
        else if (result.equals(Constants.ERR_UNCONF)){
            responseJson.put(0, Constants.ERR_UNCONF_MSG);
        }
        else{
            responseJson.put(0, user);
        }
        return new ResponseEntity<>(responseJson, HttpStatus.OK);
    }

    /**
     *
     * @param userPass: JSON object username (email) and password
     * @param session: The current http session
     * @return ok if sign up successful, else JSON error
     * @throws ParseException
     */
    @RequestMapping(value = "/aws-sign-up", method = RequestMethod.POST)
    public ResponseEntity<Object> getPoolData(@RequestBody String userPass, HttpSession session) throws ParseException {
        JSONObject userPassJson = (JSONObject) new JSONParser().parse(userPass);
        String user = (String) userPassJson.get("user");
        String pass = (String) userPassJson.get("pass");
        JSONObject responseJson = new JSONObject();
        if (auth.signUpUser(user, pass, user)) {
            responseJson.put(0, "ok");
            return new ResponseEntity<>(responseJson, HttpStatus.OK);
        } else {
            responseJson.put(0, Constants.ERR_USER_EXISTS_MSG);
            return new ResponseEntity<>(responseJson, HttpStatus.OK);
        }
    }

    /**
     * resets the users password (Not yet implemented)
     * @param user: The users email address
     * @param session: The current http session
     * @return: Code 200
     * @throws ParseException
     */
    @RequestMapping(value = "/aws-reset-password", method = RequestMethod.POST)
    public ResponseEntity<Object> requestPassword(@RequestBody String user, HttpSession session) throws ParseException {
        JSONObject userPassJson = (JSONObject) new JSONParser().parse(user);
        String username = (String) userPassJson.get("user");
        String res = auth.resetPassword(username);
        System.out.print(res);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
