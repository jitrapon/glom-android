package com.abborg.glom;

public class Const {

//    public static final String HOST_ADDRESS = "http://putsreq.com/KJVQCdWbHg0xXf2aHWf1";
    public static String HOST_ADDRESS = "http://10.11.3.202:8080";
    public static final String SENT_TOKEN_TO_SERVER = "sentTokenToServer";
    public static final String REGISTRATION_COMPLETE = "registrationComplete";

    /**
     * TEST INFO
     * **/
//    public static final String TEST_USER_ID = "yoshi3003";
    public static final String TEST_USER_ID = "fatcat18";
    public static final String TEST_CIRCLE_ID = "dev-test-circle-1";
    public static final String TEST_API_AUTHORIZATION_HEADER = "GLOM-AUTH-TOKEN abcdefghijklmnopqrstuvwxyz0123456789";

    /**
     * NOTIFICATIONS ID
     */
    public static final int NOTIFY_LOCATION_UPDATE = 0;
    public static final int NOTIFY_BROADCAST_LOCATION = 1;

    /**
     * ACTIVITY RESULTCODES
     */
    public static final int CREATE_EVENT_RESULT_CODE = 700;
    public static final int UPDATE_EVENT_RESULT_CODE = 701;

    /**
     * API
     */
    public static final String API_AUTHORIZATION_HEADER = "Authorization";
    public static final String API_CHECKIN = "/checkin";
    public static final String API_LOCATION = "/location";
    public static final String API_GET_USERS = "/circle/%s/users";

    public static final String JSON_SERVER_MESSAGE = "message";
    public static final String JSON_SERVER_USERID = "user_id";
    public static final String JSON_SERVER_USERNAME = "user_name";
    public static final String JSON_SERVER_USER_AVATAR = "avatar";
    public static final String JSON_SERVER_USERIDS = "user_ids";
    public static final String JSON_SERVER_CIRCLEID = "circle_id";
    public static final String JSON_SERVER_GCM_TOKEN = "gcm_token";
    public static final String JSON_SERVER_LOCATION = "location";
    public static final String JSON_SERVER_LOCATION_LAT = "lat";
    public static final String JSON_SERVER_LOCATION_LONG = "long";
    public static final String JSON_SERVER_OP = "op";
    public static final String JSON_SERVER_USERS = "users";

    /**
     * HANDLER WHAT CONSTANTS
     */
    public static final int MSG_GET_USERS = 5000;
    public static final int MSG_EVENT_DELETED = 5001;
}
