package com.abborg.glom;

public class Const {

//    public static final String HOST_ADDRESS = "http://putsreq.com/KJVQCdWbHg0xXf2aHWf1";
    public static String HOST_ADDRESS = BuildConfig.SERVER_URL;
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
    public static final String API_BOARD = "/circle/%s/board";
    public static final String API_BOARD_ITEM = "/circle/%1$s/board/%2$s";

    public static final String JSON_SERVER_MESSAGE = "message";
    public static final String JSON_SERVER_USERID = "user_id";
    public static final String JSON_SERVER_USERNAME = "user_name";
    public static final String JSON_SERVER_USER_AVATAR = "avatar";
    public static final String JSON_SERVER_USERTYPE = "user_type";
    public static final String JSON_SERVER_USERIDS = "user_ids";
    public static final String JSON_SERVER_CIRCLEID = "circle_id";
    public static final String JSON_SERVER_GCM_TOKEN = "gcm_token";
    public static final String JSON_SERVER_LOCATION = "location";
    public static final String JSON_SERVER_LOCATION_LAT = "lat";
    public static final String JSON_SERVER_LOCATION_LONG = "long";
    public static final String JSON_SERVER_OP = "op";
    public static final String JSON_SERVER_USERS = "users";
    public static final String JSON_SERVER_ITEMS = "items";
    public static final String JSON_SERVER_ITEM_ID = "item_id";
    public static final String JSON_SERVER_ITEM_TYPE = "item_type";
    public static final String JSON_SERVER_CREATED_TIME = "created_time";
    public static final String JSON_SERVER_UPDATED_TIME = "updated_time";
    public static final String JSON_SERVER_INFO = "info";
    public static final String JSON_SERVER_EVENT_NAME = "event_name";
    public static final String JSON_SERVER_EVENT_START_TIME = "start_time";
    public static final String JSON_SERVER_EVENT_END_TIME = "end_time";
    public static final String JSON_SERVER_EVENT_PLACE_ID = "place_id";
    public static final String JSON_SERVER_EVENT_NOTE = "note";
    public static final String JSON_SERVER_ERROR = "error";
    public static final String JSON_SERVER_TIME = "time";
    public static final String JSON_SERVER_MESSAGE_ID = "id";
    public static final String JSON_SERVER_MESSAGE_TYPE = "type";
    public static final String JSON_SERVER_SENDER = "sender";
    public static final String JSON_VALUE_MESSAGE_TYPE_TEXT = "text";

    /**
     * HANDLER WHAT CONSTANTS
     */
    public static final int MSG_GET_USERS = 5000;
    public static final int MSG_ITEM_TO_DELETE = 5001;
    public static final int MSG_GET_ITEMS = 5002;
    public static final int MSG_EVENT_CREATED = 5003;
    public static final int MSG_EVENT_CREATED_SUCCESS = 5004;
    public static final int MSG_EVENT_CREATED_FAILED = 5005;
    public static final int MSG_EVENT_UPDATED = 5006;
    public static final int MSG_EVENT_UPDATED_SUCCESS = 5007;
    public static final int MSG_EVENT_UPDATED_FAILED = 5008;
    public static final int MSG_ITEM_DELETED_SUCCESS = 5010;
    public static final int MSG_ITEM_DELETED_FAILED = 5011;
    public static final int MSG_INIT_SUCCESS = 5012;
}
