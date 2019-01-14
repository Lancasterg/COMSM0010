/*
 * Constants used for sending errors to the client
 */

package io.openvidu.js.java;

/**
 * Local SDK constants.
 */

@SuppressWarnings("checkstyle:javadocmethod")
abstract class Constants {

    final static String ERR_EXIST = "User does not exist";
    final static String ERR_UNCONF = "User is not confirmed";

    final static String ERR_USER_EXISTS_MSG = "does_exist";
    final static String ERR_EXIST_MSG = "not_exist";
    final static String ERR_UNCONF_MSG = "not_conf";

}