var OV;
var session;
var sessionName;	// Name of the video session the user will connect to
var token;			// Token retrieved from OpenVidu Server
var logged_in = false;
var ERR_EXIST_MSG = "not_exist";
var ERR_UNCONF_MSG = "not_conf";
var ERR_USER_EXISTS_MSG = "does_exist";


/* reset password */
function showReset() {
    $('#resetModal').modal()
}

function sendReset() {
    var user = $("#userReset").val();
    if (validateEmail(user)) {
        httpPostRequest(
            'api-login/aws-reset-password',
            {user: user},
            'Reset WRONG',
            (response) => {
                console.log('success');
            }
        )
    }
}


/* Sign up */

function showSignUp() {
    $('#signupModal').modal()
}


/**
 * Sign up a user using AWS Cognito
 */
function signUp() {
    var user = $("#userSignup").val(); // Username
    var pass = $("#passSignup").val(); // Password
    var passvalid = $("#passSignup").val(); // Password

    console.log('func call');
    if ((pass !==passvalid) === false) {
        $("#error-show-signup").text('Passwords do no match');
    }
    else if (pass.length < 8) {
        $("#error-show-signup").text('Passwords must be at least 8 characters');
    }
    else if (validateEmail(user) === false){
        $("#error-show-signup").text('Incorrect email format');
    }
    else {
        httpPostRequest(
            'api-login/aws-sign-up',
            {user: user, pass: pass},
            'Signup WRONG',
            (response) => {
                if (response[0] === ERR_USER_EXISTS_MSG) {
                    $("#error-show-signup").text('Account name taken, sorry');
                } else {
                    $('#signupModal').modal('hide');
                    $("#confirm-modal-body").text("Confirmation email sent to: " + user);
                    $('#confirmModal').modal();
                }
            }
        )
    }
}


function setSession() {
    sessionName = $("#sessName").val();
    if (logged_in === false) {
        loginGuest();
    }
    $("#session").show();
    $("#join").hide();
    $("#not-logged").hide();
    $("#logged").show();

    joinSessionName();
    return false;
}


function loginGuest() {
    httpPostRequest(
        'api-login/login-guest',
        {user: "guest"},
        'Login WRONG',
        (response) => {
            user = response[0]; // Get token from response
            console.log(user);
            $("#name-user").text(user);
            $("#not-logged").hide();
            logged_in = true;
            $("#nickName").val(user);
            $("#user").val(user);
            console.log("logged in");
        }
    );
}

function leaveSession() {

    // --- 9) Leave the session by calling 'disconnect' method over the Session object ---

    session.disconnect();
    session = null;

    // Removing all HTML elements with the user's nicknames
    cleanSessionView();
    $("#not-logged").hide();
    $("#session").hide();
    $("#logged").show();
    $("#join").show();


}

/* OPENVIDU METHODS */


/* APPLICATION REST METHODS */


function awsLogin(user, pass) {
    httpPostRequest(
        'api-login/aws-login',
        {user: user, pass: pass},
        'Login WRONG',
        (response) => {
            var res = response[0];
            if (res === user) {
                $("#name-user").text(user);
                $("#not-logged").hide();
                $("#logged").show();
                $("#join").show();

                // Random nickName and session
                $("#sessionName").val("Session" + Math.floor(Math.random() * 10));
                $("#nickName").val("Participant" + Math.floor(Math.random() * 100));
                logged_in = true;
            } else if (res === ERR_EXIST_MSG) {
                console.log(ERR_EXIST_MSG);
                $("#error-show").text('Incorrect username/password');
            } else if (res === ERR_UNCONF_MSG) {
                console.log(ERR_UNCONF_MSG);
                $("#error-show").text('Please confirm email to login');
            }
        }
    )
}


function login() {
    var user = $("#user").val(); // Username
    var pass = $("#pass").val(); // Password
    awsLogin(user, pass);
}

function logOut() {
    httpPostRequest(
        'api-login/logout',
        {},
        'Logout WRONG',
        (response) => {
            $("#not-logged").show();
            $("#logged").hide();
            logged_in = false;
        }
    );
}


function removeUser() {
    httpPostRequest(
        'api-sessions/remove-user',
        {sessionName: sessionName, token: token},
        'User couldn\'t be removed from session',
        (response) => {
            console.warn("You have been removed from session " + sessionName);
        }
    );
}

function inviteEmail() {


    var invite = $("#invite-entry").val(); // Username

    if (validateEmail(invite)) {
        httpPostRequest(
            'api-mail/sendInvite',
            {emailAddress: invite, sessionName: sessionName},
            'Email not sent',
            (response) => {
                console.warn("Email sent to: " + invite);
                $("#modal-body").text("Email sent to: " + invite);
                $("#exampleModalLabel").text("Invite sent");
                $('#exampleModal').modal()
            }
        );

    } else {
        $("#exampleModalLabel").text("Invite not sent");
        $("#modal-body").text('Invalid email address');
        $('#exampleModal').modal()
    }

}


function httpPostRequest(url, body, errorMsg, callback) {
    var http = new XMLHttpRequest();
    http.open('POST', url, true);
    http.setRequestHeader('Content-type', 'application/json');
    http.addEventListener('readystatechange', processRequest, false);
    http.send(JSON.stringify(body));

    function processRequest() {
        if (http.readyState == 4) {
            if (http.status == 200) {
                try {
                    callback(JSON.parse(http.responseText));
                } catch (e) {
                    callback();
                }
            } else {
                console.warn(errorMsg);
                console.warn(http.responseText);
            }
        }
    }
}

/* APPLICATION REST METHODS */


/* APPLICATION BROWSER METHODS */

window.onbeforeunload = () => { // Gracefully leave session
    if (session) {
        removeUser();
        leaveSession();
    }
    logOut();
};

function appendUserData(videoElement, connection) {
    var clientData;
    var serverData;
    var nodeId;
    if (connection.nickName) { // Appending local video data
        clientData = connection.nickName;
        serverData = connection.userName;
        nodeId = 'main-videodata';
    } else {
        clientData = JSON.parse(connection.data.split('%/%')[0]).clientData;
        serverData = JSON.parse(connection.data.split('%/%')[1]).serverData;
        nodeId = connection.connectionId;
    }
    var dataNode = document.createElement('div');
    dataNode.className = "data-node";
    dataNode.id = "data-" + nodeId;
    dataNode.innerHTML = "<p class='nickName'>" + clientData + "</p><p class='userName'>" + serverData + "</p>";
    videoElement.parentNode.insertBefore(dataNode, videoElement.nextSibling);
    addClickListener(videoElement, clientData, serverData);
}

function removeUserData(connection) {
    var userNameRemoved = $("#data-" + connection.connectionId);
    if ($(userNameRemoved).find('p.userName').html() === $('#main-video p.userName').html()) {
        cleanMainVideo(); // The participant focused in the main video has left
    }
    $("#data-" + connection.connectionId).remove();
}

function removeAllUserData() {
    $(".data-node").remove();
}

function cleanMainVideo() {
    $('#main-video video').get(0).srcObject = null;
    $('#main-video p').each(function () {
        $(this).html('');
    });
}

function addClickListener(videoElement, clientData, serverData) {
    videoElement.addEventListener('click', function () {
        var mainVideo = $('#main-video video').get(0);
        if (mainVideo.srcObject !== videoElement.srcObject) {
            $('#main-video').fadeOut("fast", () => {
                $('#main-video p.nickName').html(clientData);
                $('#main-video p.userName').html(serverData);
                mainVideo.srcObject = videoElement.srcObject;
                $('#main-video').fadeIn("fast");
            });
        }
    });
}

function initMainVideo(videoElement, userData) {
    $('#main-video video').get(0).srcObject = videoElement.srcObject;
    $('#main-video p.nickName').html(userData.nickName);
    $('#main-video p.userName').html(userData.userName);
    $('#main-video video').prop('muted', true);
}

function initMainVideoThumbnail() {
    $('#main-video video').css("background", "url('images/subscriber-msg.jpg') round");
}


function cleanSessionView() {
    removeAllUserData();
    cleanMainVideo();
    $('#main-video video').css("background", "");
}

/* APPLICATION BROWSER METHODS */


function parseURLParams(url) {
    var queryStart = url.indexOf("?") + 1,
        queryEnd = url.indexOf("#") + 1 || url.length + 1,
        query = url.slice(queryStart, queryEnd - 1),
        pairs = query.replace(/\+/g, " ").split("&"),
        parms = {}, i, n, v, nv;

    if (query === url || query === "") return;

    for (i = 0; i < pairs.length; i++) {
        nv = pairs[i].split("=", 2);
        n = decodeURIComponent(nv[0]);
        v = decodeURIComponent(nv[1]);

        if (!parms.hasOwnProperty(n)) parms[n] = [];
        parms[n].push(nv.length === 2 ? v : null);
    }
    return parms;
}


function joinSessionName() {
    getTokenName((token) => {
        OV = new OpenVidu();
        session = OV.initSession();
        session.on('streamCreated', (event) => {
            var subscriber = session.subscribe(event.stream, 'video-container');
            subscriber.on('videoElementCreated', (event) => {
                appendUserData(event.element, subscriber.stream.connection);
            });
        });
        session.on('streamDestroyed', (event) => {
            // Delete the HTML element with the user's name and nickname
            removeUserData(event.stream.connection);
        });
        var nickName = $("#nickName").val();
        session.connect(token, {clientData: nickName})
            .then(() => {
                var userName = $("#user").val();
                $('#session-title').text(sessionName);
                $('#join').hide();
                $('#session').show();
                var publisher = OV.initPublisher('video-container', {
                    audioSource: undefined, // The source of audio. If undefined default microphone
                    videoSource: undefined, // The source of video. If undefined default webcam
                    publishAudio: true,  	// Whether you want to start publishing with your audio unmuted or not
                    publishVideo: true,  	// Whether you want to start publishing with your video enabled or not
                    resolution: '640x480',  // The resolution of your video
                    frameRate: 30,			// The frame rate of your video
                    insertMode: 'APPEND',	// How the video is inserted in the target element 'video-container'
                    mirror: false       	// Whether to mirror your local video or not
                });
                publisher.on('videoElementCreated', (event) => {
                    // Init the main video with ours and append our data
                    var userData = {
                        nickName: nickName,
                        userName: userName
                    };
                    initMainVideo(event.element, userData);
                    appendUserData(event.element, userData);
                    $(event.element).prop('muted', true); // Mute local video
                });
                session.publish(publisher);
            })
            .catch(error => {
                console.warn('There was an error connecting to the session:', error.code, error.message);
            });
    });

    return false;
}

function joinSession() {
    getToken((token) => {
        OV = new OpenVidu();
        session = OV.initSession();
        session.on('streamCreated', (event) => {
            var subscriber = session.subscribe(event.stream, 'video-container');
            subscriber.on('videoElementCreated', (event) => {
                appendUserData(event.element, subscriber.stream.connection);
            });
        });
        session.on('streamDestroyed', (event) => {
            removeUserData(event.stream.connection);
        });
        var nickName = $("#nickName").val();
        session.connect(token, {clientData: nickName})
            .then(() => {
                var userName = $("#user").val();
                $('#session-title').text(sessionName);
                $('#join').hide();
                $('#session').show();
                var publisher = OV.initPublisher('video-container', {
                    audioSource: undefined, // The source of audio. If undefined default microphone
                    videoSource: undefined, // The source of video. If undefined default webcam
                    publishAudio: true,  	// Whether you want to start publishing with your audio unmuted or not
                    publishVideo: true,  	// Whether you want to start publishing with your video enabled or not
                    resolution: '640x480',  // The resolution of your video
                    frameRate: 30,			// The frame rate of your video
                    insertMode: 'APPEND',	// How the video is inserted in the target element 'video-container'
                    mirror: false       	// Whether to mirror your local video or not
                });
                publisher.on('videoElementCreated', (event) => {
                    var userData = {
                        nickName: nickName,
                        userName: userName
                    };
                    initMainVideo(event.element, userData);
                    appendUserData(event.element, userData);
                    $(event.element).prop('muted', true); // Mute local video
                });
                session.publish(publisher);
            })
            .catch(error => {
                console.warn('There was an error connecting to the session:', error.code, error.message);
            });
    });

    return false;
}


/* Get Tokens */


function getToken(callback) {

    sessionName = $("#sessionName").val(); // Video-call chosen by the user

    httpPostRequest(
        'api-sessions/get-token',
        {sessionName: sessionName, join: join},
        'Request of TOKEN gone WRONG:',
        (response) => {
            token = response[0]; // Get token from response
            sessionName = response[1];
            console.warn('Request of TOKEN gone WELL (TOKEN:' + token + ')');
            callback(token); // Continue the join operation
        }
    );

    console.log(sessionName);
}


function getTokenName(callback) {
    httpPostRequest(
        'api-sessions/get-token',
        {sessionName: sessionName, join: join},
        'Request of TOKEN gone WRONG:',
        (response) => {
            token = response[0]; // Get token from response
            sessionName = response[1];
            console.warn('Request of TOKEN gone WELL (TOKEN:' + token + ')');
            callback(token); // Continue the join operation
        }
    );

    console.log(sessionName);
}

function validateEmail(email) {
    var re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
    return re.test(String(email).toLowerCase());
}